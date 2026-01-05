package Modelo;

import java.io.Serializable;
import java.util.Objects;

public class Carta implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Color color;
    private final Numero valor;

    public Carta(Color color, Numero valor) {
        this.color = color;
        this.valor = valor;
    }

    public synchronized Color getColor() { return color; }
    public synchronized Numero getValor() { return valor; }
//------------No se si dejar esto
    @Override
    public synchronized String toString() {
        return color + " " + valor;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Carta carta = (Carta) o;
        return color == carta.color && valor == carta.valor;
    }

    @Override
    public int hashCode() {
        return Objects.hash(color, valor);
    }
}


