package Modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.Random;

// Representa un mazo de cartas
class Mazo {
    private final Stack<Carta> mazo;
    private final List<Carta> descartes;

    public Mazo() {
        this.mazo = new Stack<>();
        this.descartes = new ArrayList<>();
        inicializarMazo();
    }

    private synchronized void inicializarMazo() {
        for (Color color : Color.values()) {
            if (color == Color.SIN_COLOR) continue; // Excluir el color especial para cartas Wild

            // Cartas numéricas (0 al 9, donde el 1 al 9 aparecen dos veces)
            for (int i = 0; i <= 9; i++) {
                mazo.add(new Carta(color, Numero.getNumero(i)));
                if (i > 0) mazo.add(new Carta(color, Numero.getNumero(i))); // Duplicar del 1 al 9
            }

            // Cartas especiales de cada color
            for (int i = 0; i < 2; i++) {
                mazo.add(new Carta(color, Numero.SALTARSE));
                mazo.add(new Carta(color, Numero.CAMBIOSENTIDO));
                mazo.add(new Carta(color, Numero.MASDOS));
            }
        }

        // Cartas Wild (sin color)
        for (int i = 0; i < 4; i++) {
            mazo.add(new Carta(Color.SIN_COLOR, Numero.CAMBIOCOLOR));
            mazo.add(new Carta(Color.SIN_COLOR, Numero.MASCUATRO));
        }
        barajar();
    }

    public synchronized  void barajar() {
        Collections.shuffle(mazo);
    }

    public synchronized Carta robarCarta() {
        if (mazo.isEmpty()) {
            reponerMazo();
        }
        return mazo.pop();
    }

    public synchronized void descartar(Carta carta) {
        descartes.add(carta);
    }

    private synchronized void reponerMazo() {
        if (descartes.isEmpty()) {
            throw new IllegalStateException("No hay cartas en los descartes para reponer el mazo.");
        }

        // Mantener la última carta del descarte
        Carta ultimaCarta = descartes.remove(descartes.size() - 1);

        // Pasar todas las demás al mazo
        mazo.addAll(descartes);

        // Limpiar descartes y dejar solo la última carta
        descartes.clear();
        descartes.add(ultimaCarta);

        // Barajar solo el mazo
        barajar();
    }

    public synchronized List<Carta> getDescartes() {
        return Collections.unmodifiableList(descartes);
    }

    public synchronized Carta getUltimaCartaJugadas() {
        if (descartes.isEmpty()) {
            throw new IllegalStateException("No hay cartas en el descarte.");
        }
        return descartes.get(descartes.size() - 1);
    }

    public synchronized boolean isEmpty() {
        return mazo.isEmpty();
    }

    public synchronized void reiniciar() {
        mazo.clear();
        descartes.clear();
        inicializarMazo();
    }

}
