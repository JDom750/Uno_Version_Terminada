package Modelo;

import java.io.Serializable;

public class Evento implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String tipo;
    private final Object datos;

    public Evento(String tipo, Object datos) {
        this.tipo = tipo;
        this.datos = datos;
    }

    public String getTipo() { return tipo; }
    public Object getDatos() { return datos; }
}

