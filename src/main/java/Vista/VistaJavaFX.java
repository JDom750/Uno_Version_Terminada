package Vista;

import Controlador.ControladorUNO;
import Controlador.VistaObserver;
import Modelo.Carta;
import Modelo.Color;
import Modelo.Jugador;
import Modelo.Numero;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class VistaJavaFX implements VistaObserver {

    private final ControladorUNO controlador;
    private Stage stage;

    // Componentes de la interfaz
    private Label lblEstadoJuego = new Label();
    private Label lblMiNombre = new Label();
    private HBox contenedorMano = new HBox(10); // Contenedor horizontal para mis cartas
    private StackPane pilaDescartes = new StackPane(); // Donde va la última carta
    private StackPane mazoRobar = new StackPane();     // El mazo boca abajo
    private Button btnPasar = new Button("Pasar Turno");
    private CircleColor indicadorColor = new CircleColor(); // Un círculo que muestra el color actual
    private BorderPane rootLayout = new BorderPane();

    // Variables de control
    private boolean dialogAbierto = false;
    private boolean yaRobe = false;
    private String ultimoJugadorActual = "";
    // Controlar cambio de turno
    private Carta ultimaCartaRegistrada = null;

    public VistaJavaFX(ControladorUNO controlador) {
        this.controlador = controlador;
        controlador.registrarVista(this);
    }

    public void start(Stage stage) {
        this.stage = stage;
        stage.setTitle("UNO - Juego en Red");

        // --- ZONA SUPERIOR (Info) ---
        VBox topPane = new VBox(10);
        topPane.setPadding(new Insets(15));
        topPane.setAlignment(Pos.CENTER);
        topPane.setStyle("-fx-background-color: #333333;");

        lblMiNombre.setTextFill(javafx.scene.paint.Color.WHITE);
        lblMiNombre.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        lblEstadoJuego.setTextFill(javafx.scene.paint.Color.LIGHTGRAY);
        lblEstadoJuego.setFont(Font.font("Arial", 16));

        topPane.getChildren().addAll(lblMiNombre, lblEstadoJuego);
        rootLayout.setTop(topPane);

        // --- ZONA CENTRAL (Mesa) ---
        HBox mesa = new HBox(40); // Espacio entre mazo y descarte
        mesa.setAlignment(Pos.CENTER);
        mesa.setPadding(new Insets(30));

        // 1. Mazo para robar (Diseño visual)
        construirMazoVisual();

        // 2. Pila de descartes (Inicialmente vacía)
        pilaDescartes.setPrefSize(100, 150);

        // 3. Indicador de color activo
        VBox panelCentralInfo = new VBox(10, new Label("Color Actual:"), indicadorColor);
        panelCentralInfo.setAlignment(Pos.CENTER);

        mesa.getChildren().addAll(mazoRobar, pilaDescartes, panelCentralInfo);
        rootLayout.setCenter(mesa);

        // --- ZONA INFERIOR (Mano del jugador) ---
        VBox bottomPane = new VBox(10);
        bottomPane.setPadding(new Insets(15));
        bottomPane.setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #cccccc; -fx-border-width: 2px 0 0 0;");

        Label lblTuMano = new Label("Tus Cartas (Click para jugar):");
        lblTuMano.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        // Scroll por si tenés muchas cartas
        ScrollPane scrollMano = new ScrollPane(contenedorMano);
        scrollMano.setFitToHeight(true);
        scrollMano.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollMano.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        contenedorMano.setAlignment(Pos.CENTER_LEFT);
        contenedorMano.setPadding(new Insets(10));

        bottomPane.getChildren().addAll(lblTuMano, scrollMano);
        rootLayout.setBottom(bottomPane);

        // --- ZONA DERECHA (Acciones extra) ---
        VBox rightPane = new VBox(15);
        rightPane.setPadding(new Insets(20));
        rightPane.setAlignment(Pos.CENTER);

        // Estilo botón pasar
        btnPasar.setStyle("-fx-background-color: #ff6b6b; -fx-text-fill: white; -fx-font-weight: bold;");
        btnPasar.setPrefWidth(100);
        btnPasar.setDisable(true);
        btnPasar.setOnAction(e -> accionPasarTurno());

        rightPane.getChildren().add(btnPasar);
        rootLayout.setRight(rightPane);

        Scene scene = new Scene(rootLayout, 900, 600); // Ventana más grande
        stage.setScene(scene);
        stage.show();

        this.actualizar();
    }

    /** Crea el gráfico del mazo boca abajo */
    private void construirMazoVisual() {
        Rectangle bg = new Rectangle(100, 150);
        bg.setArcWidth(15);
        bg.setArcHeight(15);
        bg.setFill(javafx.scene.paint.Color.DARKGRAY);
        bg.setStroke(javafx.scene.paint.Color.WHITE);
        bg.setStrokeWidth(2);

        Text txt = new Text("UNO");
        txt.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        txt.setFill(javafx.scene.paint.Color.WHITE);
        txt.setRotate(-45);

        mazoRobar.getChildren().addAll(bg, txt);
        mazoRobar.setEffect(new DropShadow(5, javafx.scene.paint.Color.BLACK));

        // Acción de robar al hacer click en el mazo
        mazoRobar.setOnMouseClicked(e -> accionRobar());
    }

    // ================= ACCIONES =================

    private void accionRobar() {
        if (mazoRobar.isDisabled()) return; // Si no es mi turno o ya robé
        try {
            controlador.robarCarta();
            yaRobe = true;
            actualizar();
        } catch (Exception ex) {
            mostrarMensaje("Error", ex.getMessage());
        }
    }

    private void accionPasarTurno() {
        try {
            controlador.pasarTurno();
        } catch (Exception ex) {
            mostrarMensaje("Error", ex.getMessage());
        }
    }

    private void accionJugarCarta(int indice) {
        try {
            controlador.jugarCarta(indice);
            // El servidor avanza el turno solo, no hacemos nada más
        } catch (Exception ex) {
            mostrarMensaje("Jugada Inválida", ex.getMessage());
        }
    }

    // ================= ACTUALIZACIÓN =================

    @Override
    public void actualizar() {
        Platform.runLater(() -> {
            try {
                // 1. Datos básicos
                boolean enCurso = controlador.isPartidaEnCurso();
                Jugador jugadorActual = controlador.obtenerJugadorActual();
                Carta ultima = controlador.obtenerUltimaCartaJugadas();
                Color color = controlador.obtenerColorActual();
                Jugador local = controlador.getJugadorLocal();

                if (local == null) return;

                // 2. Detección cambio de turno
//                if (!jugadorActual.getNombre().equals(ultimoJugadorActual)) {
//                    ultimoJugadorActual = jugadorActual.getNombre();
//                    yaRobe = false;
//                }
                // A. Detectar si cambió el jugador
                boolean cambioJugador = !jugadorActual.getNombre().equals(ultimoJugadorActual);

                // B. Detectar si se jugó una carta nueva en la mesa
                boolean huboJugada = ultima != null && !ultima.equals(ultimaCartaRegistrada);

                if (cambioJugador || huboJugada) {
                    ultimoJugadorActual = jugadorActual.getNombre();
                    ultimaCartaRegistrada = ultima;

                    // Si cambió algo, reseteamos el estado de "ya robé" porque es una nueva oportunidad
                    yaRobe = false;
                }

                // 3. Info Textual
                lblMiNombre.setText("Jugador: " + local.getNombre());
                boolean esMiTurno = local.getNombre().equals(jugadorActual.getNombre());

                if (!enCurso) {
                    lblEstadoJuego.setText("PARTIDA FINALIZADA");
                    deshabilitarTodo();
                    return;
                }

                if (esMiTurno) {
                    lblEstadoJuego.setText("¡ES TU TURNO!");
                    lblEstadoJuego.setTextFill(javafx.scene.paint.Color.YELLOW);
                } else {
                    lblEstadoJuego.setText("Turno de: " + jugadorActual.getNombre());
                    lblEstadoJuego.setTextFill(javafx.scene.paint.Color.LIGHTGRAY);
                }

                // 4. Actualizar Mesa (Última carta y Color)
                actualizarPilaDescartes(ultima);
                indicadorColor.setColor(color);

                // 5. Habilitar/Deshabilitar controles
                // El mazo (robar) se deshabilita si no es mi turno o si ya robé
                mazoRobar.setDisable(!esMiTurno || yaRobe);
                mazoRobar.setOpacity(mazoRobar.isDisabled() ? 0.5 : 1.0);

                // Botón pasar solo si ya robé
                btnPasar.setDisable(!esMiTurno || !yaRobe);

                // 6. Dibujar mi mano
                contenedorMano.getChildren().clear();
                List<Carta> misCartas = local.getCartas();
                for (int i = 0; i < misCartas.size(); i++) {
                    Carta c = misCartas.get(i);
                    int indexFinal = i;

                    // Crear vista de carta
                    Node cartaVisual = CartaVisualFactory.crearCarta(c);

                    // Si es mi turno, le damos comportamiento de click
                    if (esMiTurno) {
                        cartaVisual.setOnMouseClicked(e -> accionJugarCarta(indexFinal));
                        cartaVisual.setCursor(javafx.scene.Cursor.HAND);
                        // Efecto hover
                        cartaVisual.setOnMouseEntered(e -> cartaVisual.setTranslateY(-10));
                        cartaVisual.setOnMouseExited(e -> cartaVisual.setTranslateY(0));
                    } else {
                        cartaVisual.setOpacity(0.7); // Un poco transparente si no es mi turno
                    }

                    contenedorMano.getChildren().add(cartaVisual);
                }

                // 7. Dialogo color
                if (enCurso && controlador.isEstadoEsperandoColor() && esMiTurno && !dialogAbierto) {
                    pedirColor();
                }

            } catch (Exception ex) {
                // ex.printStackTrace();
            }
        });
    }

    private void deshabilitarTodo() {
        mazoRobar.setDisable(true);
        btnPasar.setDisable(true);
        contenedorMano.setDisable(true);
    }

    private void actualizarPilaDescartes(Carta c) {
        pilaDescartes.getChildren().clear();
        if (c != null) {
            // Reutilizamos la fábrica de cartas visuales
            Node cartaNode = CartaVisualFactory.crearCarta(c);
            pilaDescartes.getChildren().add(cartaNode);
        } else {
            // Placeholder vacío
            Rectangle r = new Rectangle(100, 150, javafx.scene.paint.Color.TRANSPARENT);
            r.setStroke(javafx.scene.paint.Color.GRAY);
            r.getStrokeDashArray().addAll(5.0); // Esto es lo correcto
            pilaDescartes.getChildren().add(r);
        }
    }

    private void pedirColor() {
        dialogAbierto = true;
        List<Color> colores = Arrays.asList(Color.ROJO, Color.AZUL, Color.VERDE, Color.AMARILLO);

        ChoiceDialog<Color> dlg = new ChoiceDialog<>(Color.ROJO, colores);
        dlg.setTitle("UNO");
        dlg.setHeaderText("¡Comodín jugado!");
        dlg.setContentText("Seleccioná el próximo color:");

        dlg.getDialogPane().lookupButton(ButtonType.CANCEL).setVisible(false);
        dlg.getDialogPane().getScene().getWindow().setOnCloseRequest(e -> e.consume());

        Optional<Color> result = dlg.showAndWait();
        result.ifPresent(color -> controlador.manejarCambioDeColor(color));
        dialogAbierto = false;
    }

    @Override
    public void mostrarMensaje(String titulo, String mensaje) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Información");
            alert.setHeaderText(titulo);
            alert.setContentText(mensaje);
            alert.show();
        });
    }

    // ================= CLASES INTERNAS VISUALES =================

    /** Clase auxiliar para dibujar una carta linda con JavaFX puro */
    private static class CartaVisualFactory {
        public static Node crearCarta(Carta carta) {
            StackPane root = new StackPane();
            root.setPrefSize(100, 150);

            // 1. Fondo base (Blanco para borde)
            Rectangle base = new Rectangle(100, 150);
            base.setArcWidth(15);
            base.setArcHeight(15);
            base.setFill(javafx.scene.paint.Color.WHITE);
            base.setStroke(javafx.scene.paint.Color.BLACK);

            // 2. Interior color
            Rectangle interior = new Rectangle(90, 140);
            interior.setArcWidth(10);
            interior.setArcHeight(10);
            interior.setFill(obtenerPintura(carta.getColor()));

            // 3. Texto Central (Número o Símbolo)
            String texto = obtenerTextoCarta(carta);
            Text valor = new Text(texto);
            valor.setFont(Font.font("Arial", FontWeight.BOLD, 40));
            valor.setFill(javafx.scene.paint.Color.WHITE);
            valor.setStroke(javafx.scene.paint.Color.BLACK);
            valor.setStrokeWidth(1.5);

            // Sombra para dar profundidad
            root.setEffect(new DropShadow(3, javafx.scene.paint.Color.GRAY));

            root.getChildren().addAll(base, interior, valor);
            return root;
        }

        private static Paint obtenerPintura(Color c) {
            return switch (c) {
                case ROJO -> javafx.scene.paint.Color.RED;
                case AZUL -> javafx.scene.paint.Color.BLUE;
                case VERDE -> javafx.scene.paint.Color.GREEN;
                case AMARILLO -> javafx.scene.paint.Color.GOLD;
                default -> javafx.scene.paint.Color.BLACK; // Comodines
            };
        }

        private static String obtenerTextoCarta(Carta c) {
            Numero n = c.getValor();
            return switch (n) {
                case CERO->"0"; case UNO->"1"; case DOS->"2"; case TRES->"3"; case CUATRO->"4";
                case CINCO->"5"; case SEIS->"6"; case SIETE->"7"; case OCHO->"8"; case NUEVE->"9";
                case MASDOS -> "+2";
                case SALTARSE -> "⊘"; // Símbolo de prohibido
                case CAMBIOSENTIDO -> "⇄"; // Flechas
                case MASCUATRO -> "+4";
                case CAMBIOCOLOR -> "🎨";
                default -> "?";
            };
        }
    }

    /** Círculo simple para mostrar el color actual */
    private static class CircleColor extends StackPane {
        private Rectangle rect;
        public CircleColor() {
            rect = new Rectangle(40, 40);
            rect.setArcWidth(40);
            rect.setArcHeight(40);
            rect.setStroke(javafx.scene.paint.Color.BLACK);
            rect.setStrokeWidth(2);
            this.getChildren().add(rect);
        }
        public void setColor(Color c) {
            rect.setFill(CartaVisualFactory.obtenerPintura(c));
        }
    }
}