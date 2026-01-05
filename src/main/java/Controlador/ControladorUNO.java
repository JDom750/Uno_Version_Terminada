package Controlador;

import Modelo.*;
import Vista.VistaEsperaJavaFX;
import ar.edu.unlu.rmimvc.cliente.IControladorRemoto;
import ar.edu.unlu.rmimvc.observer.IObservableRemoto;
import java.util.concurrent.CopyOnWriteArrayList;


import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class ControladorUNO implements IControladorRemoto {

    private IPartidaRemota partida;                        // modelo remoto
    //private final List<VistaObserver> vistas = new ArrayList<>(); // vistas locales
    private final List<VistaObserver> vistas = new CopyOnWriteArrayList<>();  //thread safety
    private String nombreLocal;
    // Agregar este atributo:
    private VistaEsperaJavaFX vistaEspera;


    // ============  MVC local (vistas) ============

    public void registrarVista(VistaObserver vista) {
        this.vistas.add(vista);
    }

    private void notificarVistas() {
        for (VistaObserver v : vistas) {
            v.actualizar();
        }
    }

    // ============ IControladorRemoto (RMI) ============

    @Override
    public <T extends IObservableRemoto> void setModeloRemoto(T modeloRemoto) throws RemoteException {
        this.partida = (IPartidaRemota) modeloRemoto; // cast seguro
    }

//    @Override
//    public void actualizar(IObservableRemoto observable, Object o) throws RemoteException {
//        // Cada vez que el modelo remoto notifica, yo aviso a mis vistas locales
//        notificarVistas();
//    }
@Override
public void actualizar(IObservableRemoto observable, Object evento) throws RemoteException {

    // Si el servidor manda un Evento
    if (evento instanceof Evento e) {

        switch (e.getTipo()) {

            case "JUGADOR_REGISTRADO":
                if (vistaEspera != null) {
                    vistaEspera.agregarJugador((String) e.getDatos());
                }
                break;

            case "INICIO_PARTIDA":  // ✔ nombre corregido
                if (vistaEspera != null) {
                    vistaEspera.cerrar();
                    vistaEspera = null;
                }
                notificarVistas(); // Abrir vista principal
                break;
            // --- NUEVOS CASOS ---
            case "UNO_GRITADO":
                String nombreJugador = (String) e.getDatos();
                notificarMensaje("¡UNO!", "¡El jugador " + nombreJugador + " tiene una sola carta!");
                notificarVistas();
                break;

            case "FIN_PARTIDA":
                String ganador = (String) e.getDatos();
                notificarMensaje("FIN DEL JUEGO", "¡Ha ganado " + ganador + "!");
                // No llamamos a notificarVistas() aquí porque el juego terminó,
                // pero si querés que se vea la última carta tirada, llamalo:
                notificarVistas();
                break;

            default:
                // eventos de juego normales: turno, color, carta jugada, etc.
                notificarVistas();
                break;
        }
    }
    else {
        // fallback – no era un evento
        notificarVistas();
    }
}
    // Método auxiliar para enviar mensajes a todas las vistas conectadas
    private void notificarMensaje(String titulo, String mensaje) {
        for (VistaObserver v : vistas) {
            v.mostrarMensaje(titulo, mensaje);
        }
    }

    // ============ SEGURIDAD (El Portero) ============

    /**
     * Verifica si el jugador local es el dueño del turno actual.
     */
    private boolean esMiTurno() {
        try {
            if (partida == null || nombreLocal == null) return false;
            Jugador actual = partida.getJugadorActual();
            return actual.getNombre().equals(this.nombreLocal);
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ============ API usada por las vistas ============


    /** Inicia la partida con una lista de nombres de jugadores */
    public void iniciarPartida(List<String> nombresJugadores) {
        try {
            partida.iniciarPartida(nombresJugadores);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    /** El jugador intenta jugar una carta */
//    public void jugarCarta(int indiceCarta) {
//        try {
//            Jugador jugadorActual = partida.getJugadorActual();
//            Carta carta = jugadorActual.getCartas().get(indiceCarta);
//
//            partida.jugarCarta(carta);
//
//            // Caso especial: modelo indica que debe elegirse un color (+4 o CambioColor)
//            if (partida.isEstadoEsperandoColor()) {
//                return; // la vista debe pedir color al jugador
//            }
//
//            // La carta es normal o reversa :
//            if (!partida.isEstadoEsperandoColor() && carta.getColor() != Color.SIN_COLOR) {
//                partida.pasarTurno();
//            }
//
//        } catch (RemoteException e) {
//            throw new RuntimeException(e);
//        }
//    }
//    public void jugarCarta(int indiceCarta) {
//        try {
//            // Delegate entirely to server. Server will validate, aplicar efectos y pasar turno.
//            partida.jugarCarta(indiceCarta);
//        } catch (RemoteException e) {
//            throw new RuntimeException(e);
//        }
//    }
    public void jugarCarta(int indiceCarta) {
        // SEGURIDAD: Si no es mi turno, no hago nada
        if (!esMiTurno()) {
            notificarMensaje("Error", "No es tu turno. Esperá a que te toque.");
            return;
        }

        try {
            partida.jugarCarta(indiceCarta);
        } catch (Exception e) {
            // Capturamos Exception general para mostrar el mensaje en la vista (ej. validación de color)
            notificarMensaje("Jugada inválida", e.getMessage());
        }
    }


    /**
     * Robar carta NO pasa turno automáticamente.
     * La regla oficial permite jugar la carta robada si es válida.
     * La vista decide si el jugador juega esa carta o pasa turno.
     */
//    public void robarCarta() {
//        try {
//            partida.robarCartaDelMazo();
//        } catch (RemoteException e) {
//            throw new RuntimeException(e);
//        }
//    }
    public void robarCarta() {
        // SEGURIDAD
        if (!esMiTurno()) {
            notificarMensaje("Error", "No podés robar si no es tu turno.");
            return;
        }

        try {
            partida.robarCartaDelMazo();
        } catch (Exception e) {
            notificarMensaje("Aviso", e.getMessage());
        }
    }
    /**
     * Se llama solo cuando la vista eligió un color para +4 o CambioColor.
     */
//    public void manejarCambioDeColor(Color nuevoColor) {
//        try {
//            if (nuevoColor == Color.SIN_COLOR) {
//                throw new IllegalArgumentException("El color no puede ser SIN_COLOR.");
//            }
//            partida.cambiarColorActual(nuevoColor);
//            //partida.pasarTurno();
//        } catch (RemoteException e) {
//            throw new RuntimeException(e);
//        }
//    }

    public void manejarCambioDeColor(Color nuevoColor) {
        // SEGURIDAD
        if (!esMiTurno()) return; // No avisamos error aquí para no molestar con popups, solo ignoramos

        try {
            if (nuevoColor == Color.SIN_COLOR) {
                throw new IllegalArgumentException("El color no puede ser SIN_COLOR.");
            }
            partida.cambiarColorActual(nuevoColor);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** El jugador decide pasar turno (por ejemplo, después de robar) */
//    public void pasarTurno() {
//        try {
//            partida.pasarTurno();
//        } catch (RemoteException e) {
//            throw new RuntimeException(e);
//        }
//    }

    public void pasarTurno() {
        // SEGURIDAD
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

    public void registrarJugador(String nombreJugador) {
        try {
            partida.registrarJugador(nombreJugador);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void setNombreLocal(String nombre) {
        this.nombreLocal = nombre;
    }

    // Setter para registrar la vista:
    public void setVistaEspera(VistaEsperaJavaFX vista) {
        this.vistaEspera = vista;
    }

    public void solicitarInicioPartida() {
        try {
            partida.iniciarJuego();
        } catch (RemoteException e) {
            // Podrías mostrar un error si e.g. no hay suficientes jugadores
            e.printStackTrace();
        }
    }


    // ============ getters usados por la vista ============
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

}
