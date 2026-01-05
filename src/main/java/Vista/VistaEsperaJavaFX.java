package Vista;

import Controlador.ControladorUNO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.application.Platform;

public class VistaEsperaJavaFX {

    private final ControladorUNO controlador;
    private Stage stage;
    private ObservableList<String> jugadoresList;
    private Button btnIniciar; // Guardarlo como atributo para habilitar/deshabilitar

    public VistaEsperaJavaFX(ControladorUNO controlador) {
        this.controlador = controlador;
        this.jugadoresList = FXCollections.observableArrayList();

        // Registramos la vista en el controlador
        controlador.setVistaEspera(this);
    }

    /** ⬇ Se llama cuando se abre la ventana */
    public void mostrar(Stage stage) {
        this.stage = stage;

        Label titulo = new Label("Esperando jugadores...");
        titulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Label subtitulo = new Label("La partida comenzará automáticamente cuando haya suficientes jugadores.");
        subtitulo.setStyle("-fx-font-size: 12px;");

        ListView<String> listaJugadores = new ListView<>(jugadoresList);
        listaJugadores.setPrefHeight(200);

        btnIniciar = new Button("Iniciar Partida");
        btnIniciar.setDisable(true); // Desactivado al principio
        btnIniciar.setOnAction(e -> controlador.solicitarInicioPartida());

        VBox root = new VBox(15, titulo, subtitulo, listaJugadores, btnIniciar);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-padding: 20px;");

        Scene scene = new Scene(root, 350, 350);
        stage.setScene(scene);
        stage.setTitle("UNO - Esperando jugadores");
        stage.show();
    }

    /** ⬇ Llamado por ControladorUNO cuando llega el evento JUGADOR_REGISTRADO */
    public void agregarJugador(String nombre) {
        if (!jugadoresList.contains(nombre)) {
            jugadoresList.add(nombre);
        }
        // Si ya hay 2 o más, habilitamos el botón
        if (jugadoresList.size() >= 2) {
            btnIniciar.setDisable(false);}
    }

    /** ⬇ Llamado por ControladorUNO cuando llega PARTIDA_INICIADA */
    public void cerrar() {
        Platform.runLater(() -> {
            // 1. Cerrar la ventana de espera
            if (stage != null) {
                stage.close();
            }

            // 2. Abrir la ventana del juego
            try {
                // Creamos la nueva vista y la iniciamos
                VistaJavaFX vistaJuego = new VistaJavaFX(controlador);
                vistaJuego.start(new Stage());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}

