package Vista;

import Controlador.ControladorUNO;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Vista inicial de la aplicación (Login).
 * <p>
 * Su única responsabilidad es capturar el nombre del jugador y
 * gestionar la transición hacia la Sala de Espera (Lobby).
 * <p>
 * No contiene lógica de juego, solo lógica de registro inicial.
 */
public class VistaLoginJavaFX {

    // Referencia al controlador para comunicar el nombre ingresado.
    private ControladorUNO controlador;

    public VistaLoginJavaFX(ControladorUNO controlador) {
        this.controlador = controlador;
    }

    /**
     * Configura y muestra la ventana de Login.
     * @param stage El escenario principal de JavaFX donde se montará la escena.
     */
    public void start(Stage stage) {
        stage.setTitle("UNO - Ingreso");

        // Layout vertical simple
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        Label lbl = new Label("Ingrese su nombre:");
        TextField txtNombre = new TextField();
        Button btnEntrar = new Button("Entrar");

        // Acción del botón "Entrar"
        btnEntrar.setOnAction(e -> {
            String nombre = txtNombre.getText().trim();
            if (nombre.isEmpty()) return; // Validación básica

            // 1. PASO CRÍTICO: Guardar la identidad Local.
            // Antes de hablar con el servidor, le decimos al controlador: "Yo soy este usuario".
            // Esto es necesario para que el controlador pueda validar turnos después.
            controlador.setNombreLocal(nombre);

            // 2. Preparamos la siguiente vista (Sala de Espera).
            // Le pasamos el controlador para que siga escuchando eventos.
            VistaEsperaJavaFX vistaEspera = new VistaEsperaJavaFX(controlador);

            // 3. Comunicación con el Modelo Remoto (Servidor).
            try {
                // Enviamos la petición de registro vía RMI.
                // Si el servidor acepta, disparará el evento "JUGADOR_REGISTRADO".
                controlador.registrarJugador(nombre);

                // 4. Transición de Pantalla.
                // Si no hubo excepciones de red, cambiamos la escena actual por la del Lobby.
                vistaEspera.mostrar(stage);

            } catch (Exception ex) {
                ex.printStackTrace(); // Manejo de errores de conexión (ej. Servidor caído)
            }
        });

        root.getChildren().addAll(lbl, txtNombre, btnEntrar);
        stage.setScene(new Scene(root, 300, 150));
        stage.show();
    }
}