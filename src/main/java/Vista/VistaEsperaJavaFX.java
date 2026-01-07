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

/**
 * Vista de Sala de Espera (Lobby).
 * <p>
 * Pantalla intermedia donde los jugadores esperan a que se conecten los demás.
 * Responsabilidades:
 * 1. Mostrar la lista de jugadores conectados en tiempo real.
 * 2. Permitir iniciar la partida (solo si hay suficientes jugadores).
 * 3. Gestionar la transición automática hacia la Vista Principal del juego.
 */
public class VistaEsperaJavaFX {

    private final ControladorUNO controlador;
    private Stage stage;

    // Lista observable: Si agrego un elemento aquí, la ListView de la UI se actualiza sola.
    private ObservableList<String> jugadoresList;

    // Referencia al botón para habilitarlo/deshabilitarlo según la cantidad de jugadores.
    private Button btnIniciar;

    public VistaEsperaJavaFX(ControladorUNO controlador) {
        this.controlador = controlador;
        this.jugadoresList = FXCollections.observableArrayList();

        // Registro especial: Le decimos al controlador "Esta es tu vista de espera".
        // Así el controlador puede llamar a 'agregarJugador' o 'cerrar' directamente.
        controlador.setVistaEspera(this);
    }

    /**
     * Construye y muestra la interfaz gráfica del Lobby.
     * @param stage El escenario donde se dibujará la escena.
     */
    public void mostrar(Stage stage) {
        this.stage = stage;

        Label titulo = new Label("Esperando jugadores...");
        titulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Label subtitulo = new Label("La partida comenzará automáticamente cuando haya suficientes jugadores.");
        subtitulo.setStyle("-fx-font-size: 12px;");

        // Lista visual vinculada a los datos
        ListView<String> listaJugadores = new ListView<>(jugadoresList);
        listaJugadores.setPrefHeight(200);

        // Botón de Inicio
        btnIniciar = new Button("Iniciar Partida");
        btnIniciar.setDisable(true); // Nace desactivado (necesitamos min 2 jugadores)

        // Acción: Pedir al controlador (y por ende al servidor) que arranque el juego
        btnIniciar.setOnAction(e -> controlador.solicitarInicioPartida());

        VBox root = new VBox(15, titulo, subtitulo, listaJugadores, btnIniciar);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-padding: 20px;");

        Scene scene = new Scene(root, 350, 350);
        stage.setScene(scene);
        stage.setTitle("UNO - Esperando jugadores");
        stage.show();
    }

    /**
     * Método invocado por el Controlador cuando el Servidor notifica "JUGADOR_REGISTRADO".
     * Actualiza la lista visual y verifica si ya se puede iniciar.
     */
    public void agregarJugador(String nombre) {
        // Verificamos duplicados visuales por seguridad
        if (!jugadoresList.contains(nombre)) {
            jugadoresList.add(nombre);
        }
        // Regla de Negocio UI: Solo habilitar el botón si hay al menos 2 jugadores conectados.
        if (jugadoresList.size() >= 2) {
            btnIniciar.setDisable(false);
        }
    }

    /**
     * Método invocado por el Controlador cuando el Servidor notifica "INICIO_PARTIDA".
     * Cierra el Lobby y abre la mesa de juego.
     */
    public void cerrar() {
        // Platform.runLater es necesario porque este llamado viene del hilo de red (RMI),
        // y no podemos tocar componentes gráficos (Stage) desde un hilo que no sea el de JavaFX.
        Platform.runLater(() -> {
            // 1. Cerrar la ventana de espera actual
            if (stage != null) {
                stage.close();
            }

            // 2. Transición: Abrir la ventana principal del juego
            try {
                // Instanciamos la Vista Principal y le pasamos el mismo controlador
                VistaJavaFX vistaJuego = new VistaJavaFX(controlador);
                vistaJuego.start(new Stage()); // Nuevo escenario para el juego
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}