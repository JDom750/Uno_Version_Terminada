package Vista;

import Controlador.ControladorUNO;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class VistaLoginJavaFX {

    private ControladorUNO controlador;

    public VistaLoginJavaFX(ControladorUNO controlador) {
        this.controlador = controlador;
    }

    public void start(Stage stage) {
        stage.setTitle("UNO - Ingreso");

        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        Label lbl = new Label("Ingrese su nombre:");
        TextField txtNombre = new TextField();
        Button btnEntrar = new Button("Entrar");

//        btnEntrar.setOnAction(e -> {                          //Cambio esta parte hasta abajo por otra version del codigo
//            String nombre = txtNombre.getText().trim();
//            if (nombre.isEmpty()) return;
//
//            // 🔹 REGISTRA EL JUGADOR EN EL CONTROLADOR
//            controlador.registrarJugador(nombre);
//
//
//            // 🔹 PASA A LA VISTA PRINCIPAL
//            VistaJavaFX vistaJuego = new VistaJavaFX(controlador);
//            vistaJuego.start(stage);
//        });
        btnEntrar.setOnAction(e -> {
            String nombre = txtNombre.getText().trim();
            if (nombre.isEmpty()) return;

            // 1. Guardar el nombre localmente PRIMERO
            controlador.setNombreLocal(nombre);

            // 2. Registrar la vista del juego ANTES de llamar al servidor
            // (Esto soluciona el problema de perder el evento "INICIO_PARTIDA")
            //VistaJavaFX vistaJuego = new VistaJavaFX(controlador);  //Comento esto y voy a levantar y crear la vista principal desde el controlador cuando se inicie la partida
            // Nota: VistaJavaFX se registra a sí misma en el controlador en su constructor.

            // 2. Crear la vista de ESPERA (NO la del juego todavía)
            VistaEsperaJavaFX vistaEspera = new VistaEsperaJavaFX(controlador);

            // 3. Llamar al servidor
            try {
                controlador.registrarJugador(nombre);
                // Si todo sale bien, cerramos login y mostramos juego (o espera)
                //stage.close();

                // Dependiendo de la lógica, podés abrir la vista de espera o directo la del juego.
                // Como tu servidor arranca automático con 2 jugadores, quizás quieras ir directo
                // o usar la lógica de VistaEspera si implementás una sala de espera real.
                // Por ahora, asumamos tu flujo actual:
                vistaEspera.mostrar(stage);

            } catch (Exception ex) {
                ex.printStackTrace(); // Manejar error de conexión o nombre duplicado
            }
        });

        root.getChildren().addAll(lbl, txtNombre, btnEntrar);
        stage.setScene(new Scene(root, 300, 150));
        stage.show();
    }
}
