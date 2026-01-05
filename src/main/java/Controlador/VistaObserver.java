package Controlador;

public interface VistaObserver {
    void actualizar(); // Método que define cómo cada vista se actualiza
    // NUEVO MÉTODO:
    void mostrarMensaje(String titulo, String mensaje);
}

