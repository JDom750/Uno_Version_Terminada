package Modelo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Representa a un jugador en la partida de UNO.
 * Contiene su nombre y las cartas que tiene en la mano.
 * La lógica de validación de jugadas se maneja en la clase Partida.
 */
public class Jugador implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String nombre;
    private final List<Carta> cartas;

    public Jugador(String nombre) {
        this.nombre = nombre;
        this.cartas = new ArrayList<>();
    }

    public synchronized String getNombre() {
        return nombre;
    }


    /**
     * Devuelve una copia inmodificable de la mano del jugador.
     */
    public synchronized List<Carta> getCartas() {
        return Collections.unmodifiableList(cartas);
    }

    /**
     * Agrega una carta a la mano del jugador.
     */
    public synchronized void tomarCarta(Carta carta) {
        cartas.add(carta);
    }

    /**
     * Elimina una carta de la mano del jugador.
     * Lanza excepción si el jugador no tiene la carta.
     */
    public synchronized void jugarCarta(Carta carta) {
        if (!cartas.remove(carta)) {
            throw new IllegalArgumentException("La carta no está en la mano del jugador: " + carta);
        }
    }

    /**
     * Indica si al jugador aún le quedan cartas en la mano.
     */
    public synchronized boolean tieneCartas() {
        return !cartas.isEmpty();
    }

    /**
     * Devuelve el número de cartas que le quedan al jugador.
     */
    public synchronized int cantidadCartas() {
        return cartas.size();
    }

    public synchronized void vaciarMano() {
        cartas.clear();
    }
}
