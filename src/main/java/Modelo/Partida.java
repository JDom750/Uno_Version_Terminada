package Modelo;

import ar.edu.unlu.rmimvc.observer.ObservableRemoto;

import java.rmi.RemoteException;
import java.io.Serializable;
import java.util.*;

/**
 * Clase principal del Modelo (Core del Juego).
 * <p>
 * Representa el estado de una partida de UNO en el Servidor.
 * Implementa {@link IPartidaRemota} para ser accesible vía RMI desde los Controladores.
 * <p>
 * Responsabilidades:
 * 1. Mantener el estado (Mazo, Jugadores, Turno, Color Actual).
 * 2. Validar las reglas del UNO (si una carta se puede jugar, si se puede pasar turno).
 * 3. Gestionar la concurrencia (synchronized) para evitar condiciones de carrera entre jugadores.
 * 4. Notificar cambios a todos los clientes mediante el patrón Observer (Evento).
 */
public class Partida extends ObservableRemoto implements IPartidaRemota, Serializable {

    private static final int MIN_JUGADORES = 2;
    private static final int MAX_JUGADORES = 10;

    private final Mazo mazo;
    private final List<Jugador> jugadores;

    // Estado del flujo de juego
    private int turnoActual;          // Índice del jugador en la lista 'jugadores'
    private boolean partidaEnCurso;
    private boolean direccionNormal;  // true = Sentido horario, false = Anti-horario
    private Color colorActual;        // El color válido para jugar (puede cambiar por carta o comodín)

    // Variables de control para Comodines (+4 y Cambio Color)
    private boolean estadoEsperandoColor; // Bloquea el juego hasta que el usuario elija color
    private int indiceJugadorUltimaJugada = -1; // Para chequear victoria tras elegir color

    // Bandera de seguridad para reglas de turno:
    // Evita que un jugador pase el turno sin haber hecho nada (robar o jugar).
    private boolean haRobadoEnTurnoActual = false;

    public Partida() throws RemoteException {
        super();
        this.mazo = new Mazo();
        this.jugadores = new ArrayList<>();
        this.turnoActual = 0;
        this.partidaEnCurso = false;
        this.direccionNormal = true;
        this.colorActual = Color.SIN_COLOR;
        this.estadoEsperandoColor = false;
    }

    @Override
    public Color getColorActual() throws RemoteException {
        return colorActual;
    }

    /**
     * Inicia una partida con una lista explícita de nombres.
     * Útil si se quiere reiniciar el juego reemplazando los jugadores actuales.
     */
    @Override
    public synchronized void iniciarPartida(List<String> nombresJugadores) throws RemoteException {
        if (nombresJugadores.size() < MIN_JUGADORES || nombresJugadores.size() > MAX_JUGADORES) {
            throw new IllegalArgumentException("El número de jugadores debe estar entre " + MIN_JUGADORES + " y " + MAX_JUGADORES + ".");
        }

        // Reiniciar estado interno (Reset completo)
        jugadores.clear();
        turnoActual = 0;
        direccionNormal = true;
        estadoEsperandoColor = false;
        indiceJugadorUltimaJugada = -1;
        partidaEnCurso = false;
        colorActual = Color.SIN_COLOR;

        // Reiniciar mazo (Crucial para que las cartas vuelvan a estar disponibles)
        mazo.reiniciar();
        mazo.barajar();

        for (String nombre : nombresJugadores) {
            jugadores.add(new Jugador(nombre));
        }

        repartirCartasIniciales();
        partidaEnCurso = true;

        // Lógica de la primera carta en la mesa (no puede ser comodín al inicio)
        Carta primeraCarta = mazo.robarCarta();
        while (primeraCarta.getColor() == Color.SIN_COLOR) {
            mazo.descartar(primeraCarta);
            primeraCarta = mazo.robarCarta();
        }
        mazo.descartar(primeraCarta);
        this.colorActual = primeraCarta.getColor();

        // Notificar a todos los clientes que el juego arrancó
        notificarEvento(new Evento("INICIO_PARTIDA", colorActual));
    }

    private synchronized void repartirCartasIniciales() throws RemoteException {
        for (Jugador jugador : jugadores) {
            jugador.vaciarMano(); // Limpieza preventiva
            for (int i = 0; i < 7; i++) {
                jugador.tomarCarta(mazo.robarCarta());
            }
        }
    }

    /**
     * Método principal de la lógica del juego.
     * Es invocado por el cliente cuando hace clic en una carta.
     *
     * @param indiceCarta La posición de la carta en la mano del jugador actual.
     */
    public synchronized void jugarCarta(int indiceCarta) throws RemoteException {

        // 1. Validaciones de Estado
        if (!partidaEnCurso) {
            throw new IllegalStateException("No hay una partida en curso.");
        }
        if (jugadores.isEmpty()) {
            throw new IllegalStateException("No hay jugadores en la partida.");
        }

        Jugador jugadorActual = jugadores.get(turnoActual);

        if (indiceCarta < 0 || indiceCarta >= jugadorActual.getCartas().size()) {
            throw new IllegalArgumentException("Índice de carta inválido.");
        }

        Carta carta = jugadorActual.getCartas().get(indiceCarta);
        Carta ultima = null;
        try {
            ultima = mazo.getUltimaCartaJugadas();
        } catch (IllegalStateException e) {
            ultima = null; // Caso borde: primera jugada
        }

        // 2. Validación de Reglas UNO (Color, Número o Comodín)
        if (!esCartaValida(carta, ultima)) {
            throw new IllegalArgumentException("La carta no coincide con color/valor.");
        }

        // Validación estricta de +4 (Solo se puede jugar si no tenés el color actual)
        if (carta.getValor() == Numero.MASCUATRO &&
                jugadorTieneDelColor(jugadorActual, colorActual)) {
            throw new IllegalArgumentException("No podés jugar +4 si tenés el color actual.");
        }

        // 3. Ejecución de la jugada
        // Marcamos que el jugador "actuó" para permitir el paso de turno posterior.
        haRobadoEnTurnoActual = true;

        jugadorActual.jugarCarta(carta);
        mazo.descartar(carta);

        // Guardamos referencia por si hay que chequear victoria luego de elegir color
        indiceJugadorUltimaJugada = turnoActual;

        // 4. Manejo de Comodines (+4 y Cambio Color)
        // Estos requieren una segunda acción del usuario (elegir color), por lo que
        // pausamos el flujo aquí y notificamos a la vista.
        if (carta.getValor() == Numero.CAMBIOCOLOR || carta.getValor() == Numero.MASCUATRO) {
            estadoEsperandoColor = true;
            notificarEvento(new Evento("ESPERANDO_COLOR", jugadorActual.getNombre()));
            return; // Salimos para esperar la respuesta de cambiarColorActual()
        }

        // 5. Manejo de Cartas de Efecto (Salto, Reversa, +2)
        // Retorna true si la carta especial ya se encargó de mover el turno.
        boolean yaAvanzoTurno = manejarCartaEspecial(carta);

        // 6. Verificación de Victoria
        if (!jugadorActual.tieneCartas()) {
            finalizarPartida(jugadorActual);
            return;
        }

        // Actualizar color si la carta jugada tiene uno (no es comodín)
        if (carta.getColor() != Color.SIN_COLOR) {
            colorActual = carta.getColor();
        }

        // 7. Verificación de regla "UNO!" (Le queda 1 carta)
        if (jugadorActual.cantidadCartas() == 1) {
            notificarEvento(new Evento("UNO_GRITADO", jugadorActual.getNombre()));
        }

        // 8. Pase de Turno (Para cartas numéricas normales)
        if (!yaAvanzoTurno) {
            pasarTurno();
        }

        notificarEvento(new Evento("JUGAR_CARTA", carta));
    }


    private synchronized boolean esCartaValida(Carta carta, Carta ultimaCarta) {
        // Los comodines siempre se pueden tirar (con validación extra para +4 hecha antes)
        if (carta.getValor().equals(Numero.CAMBIOCOLOR) || carta.getValor().equals(Numero.MASCUATRO)) {
            return true;
        }
        if (ultimaCarta == null) {
            return true;
        }
        // Coincidencia por color o por valor/número
        return carta.getColor().equals(colorActual) ||
                carta.getValor().equals(ultimaCarta.getValor());
    }

    private synchronized boolean jugadorTieneDelColor(Jugador jugador, Color color) {
        for (Carta carta : jugador.getCartas()) {
            if (carta.getColor() == color) {
                return true;
            }
        }
        return false;
    }

    /**
     * Aplica la lógica de cartas especiales que alteran el flujo de turnos.
     * Utiliza `avanzarTurnoInterno` para evitar bloqueos de validación.
     *
     * @return true si el turno fue modificado aquí.
     */
    private synchronized boolean manejarCartaEspecial(Carta carta) throws RemoteException {
        switch (carta.getValor()) {
            case CAMBIOSENTIDO:
                // Invierte el orden de juego
                direccionNormal = !direccionNormal;

                // Regla especial 1vs1: Reversa actúa como Salto (juego de nuevo)
                if (jugadores.size() == 2) {
                    avanzarTurnoInterno(); // Salta al rival
                    avanzarTurnoInterno(); // Vuelve a mí
                    return true;
                }
                return false; // En multiplayer normal, solo invierte, el turno pasa normal después

            case SALTARSE:
                avanzarTurnoInterno(); // Mueve el foco a la víctima
                avanzarTurnoInterno(); // Salta a la víctima y pasa al siguiente
                return true;

            case MASDOS:
                robarCartasSiguientes(2); // La víctima roba
                avanzarTurnoInterno();    // Mueve el foco a la víctima
                avanzarTurnoInterno();    // Salta a la víctima (pierde turno)
                return true;

            default:
                return false;
        }
    }


    private synchronized void robarCartasSiguientes(int cantidad) throws RemoteException {
        if (jugadores.isEmpty()) return;

        // Calculamos quién es el siguiente (la víctima) respetando la dirección
        int siguienteJugador = (direccionNormal) ?
                (turnoActual + 1) % jugadores.size() :
                (turnoActual - 1 + jugadores.size()) % jugadores.size();

        Jugador j = jugadores.get(siguienteJugador);

        for (int i = 0; i < cantidad; i++) {
            Carta c = mazo.robarCarta();
            if (c == null) continue;
            j.tomarCarta(c);
        }

        // Notificamos que alguien robó cartas (sin decir cuáles, por privacidad)
        notificarEvento(new Evento("ROBAR_CARTAS", j.getNombre()));
    }

    /**
     *Metodo para desconectar al jugador y avisar al server
     */
    @Override
    public synchronized void desconectar(String nombreJugador) throws RemoteException {
        // Buscamos y removemos al jugador por su nombre
        jugadores.removeIf(j -> j.getNombre().equals(nombreJugador));

        // Avisamos a los que quedan que alguien se fue
        notificarEvento(new Evento("JUGADOR_DESCONECTADO", nombreJugador));

        // Si la partida estaba en curso y quedaron menos de 2, la terminamos a la fuerza
        if (partidaEnCurso && jugadores.size() < MIN_JUGADORES) {
            partidaEnCurso = false;
            notificarEvento(new Evento("FIN_PARTIDA", "Nadie (Falta de jugadores)"));
        }
    }

    /**
     * Usa este metodo para poder reiniciar la partida y jugar otra manteniendo los mismos jugadores
     */

    @Override
    public synchronized void reiniciarPartida() throws RemoteException {
        // Solo permitimos reiniciar si la partida terminó (por seguridad)
        if (partidaEnCurso) {
            throw new IllegalStateException("No se puede reiniciar una partida en curso.");
        }

        // VALIDACIÓN IMPORTANTE:
        if (jugadores.size() < MIN_JUGADORES) {
            throw new IllegalStateException("No hay suficientes jugadores para reiniciar.");
        }
        // Reutilizamos tu lógica existente que limpia manos y reparte
        iniciarPartidaInterna();
    }

    /**
     * Método invocado tras elegir un color para un comodín (+4 o Cambio Color).
     * Aplica el efecto del +4 en este momento.
     */
    @Override
    public synchronized void cambiarColorActual(Color nuevoColor) throws RemoteException {
        if (!estadoEsperandoColor) {
            throw new IllegalStateException("No se esperaba elección de color en este momento.");
        }
        if (nuevoColor == null || nuevoColor == Color.SIN_COLOR) {
            throw new IllegalArgumentException("El color ingresado no es válido.");
        }
        this.colorActual = nuevoColor;
        estadoEsperandoColor = false;

        // Permitimos que el flujo avance (cumple condición de haber actuado)
        haRobadoEnTurnoActual = true;

        // Revisamos si era un +4 para aplicar el castigo ahora
        Carta ultima = mazo.getUltimaCartaJugadas();
        if (ultima != null && ultima.getValor() == Numero.MASCUATRO) {
            robarCartasSiguientes(4);
            avanzarTurnoInterno(); // Mueve a la víctima
            avanzarTurnoInterno(); // Salta a la víctima
        } else {
            // Si fue CAMBIOCOLOR normal, el turno pasa al siguiente
            avanzarTurnoInterno();
        }

        // Chequeo de victoria diferido (por si se quedó sin cartas al tirar el comodín)
        if (indiceJugadorUltimaJugada >= 0) {
            Jugador posibleGanador = jugadores.get(indiceJugadorUltimaJugada);
            if (!posibleGanador.tieneCartas()) {
                finalizarPartida(posibleGanador);
                indiceJugadorUltimaJugada = -1;
                return;
            }
            indiceJugadorUltimaJugada = -1;
        }

        notificarEvento(new Evento("CAMBIO_COLOR", nuevoColor));
    }

    /**
     * Mueve el índice del turno sin realizar validaciones de reglas.
     * Se usa internamente para efectos automáticos (Salto, +2, +4).
     */
    private void avanzarTurnoInterno() {
        if (jugadores.isEmpty()) return;

        // Reseteamos bandera para que el próximo jugador deba actuar obligatoriamente
        haRobadoEnTurnoActual = false;

        // Cálculo matemático circular del siguiente índice
        turnoActual = (direccionNormal) ?
                (turnoActual + 1) % jugadores.size() :
                (turnoActual - 1 + jugadores.size()) % jugadores.size();

        notificarEvento(new Evento("CAMBIO_TURNO", jugadores.get(turnoActual).getNombre()));
    }

    /**
     * Acción manual del usuario: "Pasar Turno".
     * Solo permitido si el jugador ya robó o jugó (aunque si jugó, el turno suele pasar solo).
     */
    @Override
    public synchronized void pasarTurno() throws RemoteException {
        // VALIDACIÓN PARA EL USUARIO: Anti-AFK / Anti-Trampa
        if (!haRobadoEnTurnoActual) {
            throw new IllegalStateException("Debés robar o jugar antes de pasar.");
        }
        // Si pasa la validación, avanzamos
        avanzarTurnoInterno();
    }

    private synchronized void finalizarPartida(Jugador jugadorGanador) throws RemoteException {
        partidaEnCurso = false;
        notificarEvento(new Evento("FIN_PARTIDA", jugadorGanador.getNombre()));
    }

    @Override
    public synchronized boolean isPartidaEnCurso() throws RemoteException {
        return partidaEnCurso;
    }

    @Override
    public synchronized List<Jugador> getJugadores() throws RemoteException {
        return Collections.unmodifiableList(jugadores);
    }

    @Override
    public synchronized Jugador getJugadorActual() throws RemoteException {
        return jugadores.get(turnoActual);
    }

    public synchronized Mazo getMazo() throws RemoteException {
        return mazo;
    }

    @Override
    public synchronized Carta robarCartaDelMazo() throws RemoteException {
        if (!partidaEnCurso) throw new IllegalStateException("No hay partida.");

        // VALIDACIÓN: Solo se puede robar 1 vez por turno
        if (haRobadoEnTurnoActual) {
            throw new IllegalStateException("Ya robaste una carta. Debés jugar o pasar turno.");
        }

        Carta carta = mazo.robarCarta();
        jugadores.get(turnoActual).tomarCarta(carta);

        // Marcamos que ya robó (habilita el botón "Pasar Turno" en el cliente)
        haRobadoEnTurnoActual = true;

        notificarEvento(new Evento("ROBAR_CARTA", carta));
        return carta;
    }

    @Override
    public synchronized Carta getUltimaCartaJugadas() throws RemoteException {
        return mazo.getUltimaCartaJugadas();
    }

    private synchronized void notificarEvento(Evento evento) {
        try {
            // Llama al método update() de todos los Controladores conectados
            notificarObservadores(evento);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public synchronized boolean isEstadoEsperandoColor() {
        return estadoEsperandoColor;
    }

    public synchronized void setEstadoEsperandoColor(boolean estadoEsperandoColor) {
        this.estadoEsperandoColor = estadoEsperandoColor;
    }

    //-------------------------------------------------------------------------
    // MÉTODOS DE LA SALA DE ESPERA (LOBBY)
    //-------------------------------------------------------------------------

    public synchronized void registrarJugador(String nombre) {
        if (partidaEnCurso) return; // No se puede entrar si ya arrancó
        jugadores.add(new Jugador(nombre));
        notificarEvento(new Evento("JUGADOR_REGISTRADO", nombre));
    }

    public synchronized void iniciarJuego() throws RemoteException {
        // BLINDAJE: Si ya está en curso, ignoramos segundas llamadas (doble click)
        if (partidaEnCurso) return;

        if (jugadores.size() < MIN_JUGADORES) {
            throw new IllegalStateException("Faltan jugadores.");
        }
        iniciarPartidaInterna();
    }

    /**
     * Configuración interna para comenzar el juego.
     * Baraja, reparte y pone la primera carta.
     */
    private synchronized void iniciarPartidaInterna() {
        try {
            // Reiniciar estado interno
            turnoActual = 0;
            direccionNormal = true;
            estadoEsperandoColor = false;
            indiceJugadorUltimaJugada = -1;

            // Reiniciar y mezclar mazo
            mazo.reiniciar();
            mazo.barajar();

            // Repartir cartas
            repartirCartasIniciales();

            partidaEnCurso = true;

            // Seleccionar primera carta válida (que no sea comodín)
            Carta primeraCarta = mazo.robarCarta();
            while (primeraCarta.getColor() == Color.SIN_COLOR) {
                mazo.descartar(primeraCarta);
                primeraCarta = mazo.robarCarta();
            }
            mazo.descartar(primeraCarta);
            this.colorActual = primeraCarta.getColor();

            // Notificar inicio a todos
            notificarEvento(new Evento("INICIO_PARTIDA", colorActual));
            notificarEvento(new Evento(
                    "CAMBIO_TURNO",
                    jugadores.get(turnoActual).getNombre()
            ));

        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}