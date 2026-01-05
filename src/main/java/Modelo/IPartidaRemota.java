package Modelo;

import ar.edu.unlu.rmimvc.observer.IObservableRemoto;
import java.rmi.RemoteException;
import java.util.List;

public interface IPartidaRemota extends IObservableRemoto {
    void iniciarPartida(List<String> nombresJugadores) throws RemoteException;
    void jugarCarta(int indiceCarta) throws RemoteException;
    Carta robarCartaDelMazo() throws RemoteException;
    void cambiarColorActual(Color nuevoColor) throws RemoteException;

    Jugador getJugadorActual() throws RemoteException;
    Carta getUltimaCartaJugadas() throws RemoteException;
    Color getColorActual() throws RemoteException;
    boolean isPartidaEnCurso() throws RemoteException;
    List<Jugador> getJugadores() throws RemoteException;

    boolean isEstadoEsperandoColor() throws RemoteException;
    void registrarJugador(String nombre) throws RemoteException;
    void iniciarJuego() throws RemoteException;


    void pasarTurno() throws RemoteException;
}

