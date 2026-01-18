package Controlador;

import Modelo.*;
import Vista.VistaEsperaJavaFX;
import ar.edu.unlu.rmimvc.cliente.IControladorRemoto;
import ar.edu.unlu.rmimvc.observer.IObservableRemoto;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Controlador del lado del Cliente.
 * <p>
 * Actúa como intermediario (Patrón Adapter/Bridge) entre la Vista Local (JavaFX)
 * y el Modelo Remoto (Servidor RMI).
 * <p>
 * Sus responsabilidades son:
 * 1. Recibir las acciones de la vista (Jugar, Robar, etc.).
 * 2. Validar localmente la identidad (Turno) antes de llamar al servidor.
 * 3. Enviar la orden al servidor vía RMI.
 * 4. Recibir actualizaciones del servidor (Patrón Observer Distribuido) y refrescar la vista local.
 */
public class ControladorUNO implements IControladorRemoto {

    // Referencia al Modelo Remoto (Proxy/Stub).
    // Todas las llamadas a este objeto viajan por la red hasta el servidor.
    private IPartidaRemota partida;

    // Lista de vistas locales conectadas a este controlador.
    // Se usa CopyOnWriteArrayList para garantizar seguridad de hilos (Thread-Safety),
    // ya que las actualizaciones RMI llegan en hilos distintos al de JavaFX.
    private final List<VistaObserver> vistas = new CopyOnWriteArrayList<>();

    // Nombre del jugador que está usando este cliente.
    // Sirve para validar si es "mi turno" antes de enviar comandos.
    private String nombreLocal;

    // Referencia específica a la vista de espera para poder cerrarla cuando inicie el juego.
    private VistaEsperaJavaFX vistaEspera;


    // ============  MVC local (Gestión de Vistas) ============

    /**
     * Registra una vista para que reciba actualizaciones del modelo.
     * @param vista La interfaz gráfica (o consola) que implementa VistaObserver.
     */
    public void registrarVista(VistaObserver vista) {
        this.vistas.add(vista);
    }

    /**
     * Ordena a todas las vistas locales que se redibujen.
     * Se llama cuando llega un cambio de estado desde el servidor.
     */
    private void notificarVistas() {
        for (VistaObserver v : vistas) {
            v.actualizar();
        }
    }

    /**
     * Envía un mensaje emergente (Popup/Alert) a las vistas.
     * Útil para mostrar errores de validación o avisos importantes (Ganador, UNO).
     *
     * @param titulo Título de la ventana o mensaje.
     * @param mensaje Contenido del mensaje.
     */
    private void notificarMensaje(String titulo, String mensaje) {
        for (VistaObserver v : vistas) {
            v.mostrarMensaje(titulo, mensaje);
        }
    }

    // ============ IControladorRemoto (Librería RMI) ============

    /**
     * Método llamado automáticamente por la librería RMI al iniciar la conexión.
     * Vincula este controlador con el objeto remoto del servidor.
     *
     * @param modeloRemoto El Stub del modelo (IPartidaRemota).
     */
    @Override
    public <T extends IObservableRemoto> void setModeloRemoto(T modeloRemoto) throws RemoteException {
        this.partida = (IPartidaRemota) modeloRemoto; // cast seguro a nuestra interfaz
    }

    /**
     * Método llamado por el Servidor (vía RMI) cuando ocurre un cambio en el Modelo.
     * Este es el corazón del patrón Observer Distribuido.
     *
     * @param observable El modelo que cambió.
     * @param evento El objeto Evento que describe qué pasó (ej: "JUGAR_CARTA", "FIN_PARTIDA").
     */
    @Override
    public void actualizar(IObservableRemoto observable, Object evento) throws RemoteException {

        // Verificamos si lo que llega es un evento estructurado de nuestra lógica
        if (evento instanceof Evento e) {

            switch (e.getTipo()) {

                case "JUGADOR_REGISTRADO":
                    // Actualizamos la lista de nombres en la sala de espera
                    if (vistaEspera != null) {
                        vistaEspera.agregarJugador((String) e.getDatos());
                    }
                    // 2. CORRECCIÓN LOBBY MUDO:
                    // Avisamos a todas las vistas (incluida la consola) que entró alguien.
                    // Usamos notificarMensaje para que salga como texto/popup y no redibuje todo.
                    notificarMensaje("Lobby", "El jugador " + e.getDatos() + " se ha unido a la sala.");
                    break;

                case "INICIO_PARTIDA":
                    // Cerramos la sala de espera y forzamos a la vista principal a abrirse/actualizarse
                    if (vistaEspera != null) {
                        vistaEspera.cerrar();
                        vistaEspera = null; // Ya no la necesitamos
                    }
                    //notificarVistas(); --->Lo eliminamos para evitar un doble print, solo lo va a imprimir despues del CAMBIO_TURNO
                    break;

                case "CAMBIO_COLOR":
                    // CORRECCIÓN 1: No actualizamos la vista completa.
                    // El evento CAMBIO_TURNO viene inmediatamente después y traerá el color nuevo.
                    // Opcional: Mandar mensajito de texto si querés.
                    notificarMensaje("Juego", "El color ha cambiado a " + e.getDatos());
                    break;
//                case "JUGAR_CARTA":
//                    // Solo avisamos qué pasó, pero NO redibujamos la mesa entera todavía.
//                    // La mesa se redibujará cuando cambie el turno (que pasa casi al mismo tiempo).
//                    Carta c = (Carta) e.getDatos();
//                    notificarMensaje("Juego", "Se jugó: " + c);
//                    break;
                case "JUGAR_CARTA":
                    // CORRECCIÓN 2: Este evento suele venir junto con CAMBIO_TURNO.
                    // Si actualizamos acá, vemos la carta nueva.
                    // Si actualizamos en CAMBIO_TURNO, vemos el jugador nuevo.
                    // Lo mejor es dejar este activo y silenciar el otro, o viceversa.
                    // Pero como JUGAR_CARTA es informativo, dejemos que actualice.
                    //notificarVistas();
                    //Carta c = (Carta) e.getDatos();
                    //notificarMensaje("Juego", "Se jugó: " + c);
                    break;

                case "CAMBIO_TURNO":
                    // Este es el evento más importante. SIEMPRE actualizamos aquí.
                    notificarVistas();
                    break;

                case "UNO_GRITADO":
                    // Caso especial: Solo mostramos mensaje, NO actualizamos la mesa completa todavía
                    // (para evitar parpadeos, ya que enseguida llega el evento de carta jugada)
                    String nombreJugador = (String) e.getDatos();
                    notificarMensaje("¡UNO!", "¡El jugador " + nombreJugador + " tiene una sola carta!");
                    // Nota: Aquí intencionalmente no llamamos a notificarVistas()
                    break;

                case "FIN_PARTIDA":
                    String ganador = (String) e.getDatos();
                    notificarMensaje("FIN DEL JUEGO", "¡Ha ganado " + ganador + "!");
                    notificarVistas(); // Mostramos la mesa final
                    break;
                case "JUGADOR_DESCONECTADO":
                    String seFue = (String) e.getDatos();
                    notificarMensaje("Información", "El jugador " + seFue + " se ha desconectado.");
                    break;
//                case "ROBAR_CARTA":
//                case "ROBAR_CARTAS":
//                    // Estos sí conviene actualizarlos para ver que aumentó el nro de cartas
//                    notificarVistas();
//                    break;

                default:
                    // Para cualquier otro evento (Turno, Carta Jugada, Robar), refrescamos la UI
                    notificarVistas();
                    break;
            }
        }
        else {
            // Fallback por si el servidor manda algo que no es un Evento (ej. notificación genérica)
            notificarVistas();
        }
    }

    // ============ SEGURIDAD (El Portero) ============

    /**
     * Validación Local de Identidad.
     * Compara el nombre de este cliente con el jugador que tiene el turno en el servidor.
     * Evita enviar peticiones innecesarias si no es el momento de actuar.
     *
     * @return true si es mi turno, false si es el de otro.
     */
    private boolean esMiTurno() {
        try {
            if (partida == null || nombreLocal == null) return false;
            Jugador actual = partida.getJugadorActual(); // Llamada RMI
            return actual.getNombre().equals(this.nombreLocal);
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ============ API usada por las Vistas (Interacción Usuario -> Servidor) ============

    /**
     * Intenta jugar una carta seleccionada por el índice en la mano.
     */
    public void jugarCarta(int indiceCarta) {
        // 1. Barrera de Seguridad: ¿Es mi turno?
        if (!esMiTurno()) {
            notificarMensaje("Error", "No es tu turno. Esperá a que te toque.");
            return;
        }

        try {
            // 2. Llamada al Servidor
            partida.jugarCarta(indiceCarta);
        } catch (Exception e) {
            // 3. Manejo de Errores del Negocio (ej: Color incorrecto, Carta inválida)
            // Convertimos la excepción técnica en un mensaje amigable para el usuario.
            notificarMensaje("Jugada inválida", e.getMessage());
        }
    }

    /**
     * Intenta robar una carta del mazo.
     */
    public void robarCarta() {
        if (!esMiTurno()) {
            notificarMensaje("Error", "No podés robar si no es tu turno.");
            return;
        }

        try {
            partida.robarCartaDelMazo();
        } catch (Exception e) {
            notificarMensaje("Aviso", e.getMessage()); // Ej: "Ya robaste en este turno"
        }
    }

    /**
     * Envía la elección de color tras jugar un comodín (+4 o Cambio Color).
     */
    public void manejarCambioDeColor(Color nuevoColor) {
        if (!esMiTurno()) return;

        try {
            if (nuevoColor == Color.SIN_COLOR) {
                throw new IllegalArgumentException("El color no puede ser SIN_COLOR.");
            }
            partida.cambiarColorActual(nuevoColor);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Pasa el turno al siguiente jugador (solo permitido si ya se robó/jugó).
     */
    public void pasarTurno() {
        if (!esMiTurno()) {
            notificarMensaje("Error", "No es tu turno.");
            return;
        }

        try {
            partida.pasarTurno();
        } catch (Exception e) {
            notificarMensaje("Aviso", e.getMessage()); // Ej: "Debés robar antes de pasar"
        }
    }

    /**
     * Registra al jugador en el servidor para entrar a la sala de espera.
     */
    public void registrarJugador(String nombreJugador) {
        try {
            partida.registrarJugador(nombreJugador);
        } catch (RemoteException e) {
            throw new RuntimeException("Error de conexión al registrar jugador", e);
        }
    }

    public void setNombreLocal(String nombre) {
        this.nombreLocal = nombre;
    }

    public void setVistaEspera(VistaEsperaJavaFX vista) {
        this.vistaEspera = vista;
    }

    /**
     * Solicita al servidor iniciar el juego (botón de la sala de espera).
     */
    public void solicitarInicioPartida() {
        try {
            partida.iniciarJuego();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     *Uso el metodo para poder solicitar un reinicio de la partida desde la vista, manteniendo los mismo jugadores
     */

    public void solicitarReiniciarPartida() {
        try {
            partida.reiniciarPartida();
        } catch (Exception e) {
            // CORRECCIÓN: Si el error es porque ya arrancó, lo ignoramos (es éxito para nosotros)
            if (e.getMessage() != null && e.getMessage().contains("curso")) {
                // No hacemos nada, esperamos el evento INICIO_PARTIDA que debe estar por llegar
                System.out.println("La partida ya fue reiniciada por otro jugador.");
            } else {
                notificarMensaje("Error", "No se pudo reiniciar: " + e.getMessage());
            }
        }
    }

    /**
     *Meotodo para que el usuario pueda cerrar sesion
     */

    public void cerrarCesion() {
        try {
            if (nombreLocal != null) {
                partida.desconectar(nombreLocal);
            }
        } catch (Exception e) {
            // Si falla es porque ya no hay conexión, no importa
        }
    }


    // ============ Getters (Consultas al Servidor) ============
    // Estos métodos son invocados por la Vista para redibujarse.
    // Cada uno realiza una llamada a través de la red (RMI).

    public Jugador obtenerJugadorActual() {
        try {
            return partida.getJugadorActual();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public Carta obtenerUltimaCartaJugadas() {
        try {
            return partida.getUltimaCartaJugadas();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public Color obtenerColorActual() {
        try {
            return partida.getColorActual();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isEstadoEsperandoColor() {
        try {
            return partida.isEstadoEsperandoColor();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isPartidaEnCurso() {
        try {
            return partida.isPartidaEnCurso();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Jugador> getJugadores() {
        try {
            return partida.getJugadores();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public Jugador getJugadorLocal() {
        try {
            // Buscamos nuestro objeto Jugador en la lista del servidor usando el nombre local
            for (Jugador j : partida.getJugadores()) {
                if (j.getNombre().equals(nombreLocal)) {
                    return j;
                }
            }
            return null;
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }
    //Metodo para obtener el Top 5 gaandores
    public List<String> getRankingTop5() {
        try {
            return partida.obtenerRanking();
        } catch (RemoteException e) {
            return List.of("Error al obtener ranking");
        }
    }

    /**
     * Obtiene solo los nombres de los jugadores conectados.
     * Ideal para la Sala de Espera (evita acoplar la Vista con el Modelo).
     */
    public List<String> obtenerNombresJugadores() {
        try {
            List<Jugador> objetosJugador = partida.getJugadores();
            List<String> nombres = new ArrayList<>();
            for (Jugador j : objetosJugador) {
                nombres.add(j.getNombre());
            }
            return nombres;
        } catch (RemoteException e) {
            e.printStackTrace();
            return new ArrayList<>(); // Retorna lista vacía en caso de error
        }
    }
}