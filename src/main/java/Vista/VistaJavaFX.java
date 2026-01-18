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

/**
 * Vista Principal Gráfica (GUI) del juego UNO.
 * <p>
 * Implementada con JavaFX. Actúa como Observador del Controlador (Patrón Observer).
 * Su responsabilidad es:
 * 1. Dibujar el estado actual del juego (Mesa, Mano, Turnos).
 * 2. Capturar la interacción del usuario (Clicks en cartas, botones).
 * 3. Reflejar visualmente las reglas (Deshabilitar botones si no es el turno).
 */
public class VistaJavaFX implements VistaObserver {

    private final ControladorUNO controlador;
    private Stage stage;

    // --- Componentes visuales de la interfaz ---
    private Label lblEstadoJuego = new Label(); // Muestra "Es tu turno" o "Turno de X"
    private Label lblMiNombre = new Label();    // Muestra quién soy yo en este cliente
    private HBox contenedorMano = new HBox(10); // Contenedor horizontal donde se dibujan mis cartas
    private StackPane pilaDescartes = new StackPane(); // Lugar donde se muestran las cartas jugadas
    private StackPane mazoRobar = new StackPane();     // El mazo boca abajo para hacer click y robar
    private Button btnPasar = new Button("Pasar Turno");
    private CircleColor indicadorColor = new CircleColor(); // Círculo que indica el color vigente (útil tras comodines)
    private BorderPane rootLayout = new BorderPane();       // Layout principal (Arriba, Centro, Abajo, Derecha)

    // --- Variables de Control de Estado Local ---
    // Evita abrir múltiples diálogos de color si el servidor manda muchas actualizaciones seguidas.
    private boolean dialogAbierto = false;

    // Controla si el jugador ya robó en este turno para habilitar el botón "Pasar".
    private boolean yaRobe = false;

    // Variables para detectar cuándo ocurre un cambio de turno real o una jugada,
    // necesario para resetear el estado de los botones.
    private String ultimoJugadorActual = "";
    private Carta ultimaCartaRegistrada = null;

    /**
     * Constructor.
     * Registra esta vista en el controlador para recibir notificaciones del modelo remoto.
     */
    public VistaJavaFX(ControladorUNO controlador) {
        this.controlador = controlador;
        controlador.registrarVista(this);
    }

    /**
     * Configura e inicia la ventana de JavaFX.
     * Define la disposición (Layout) de los elementos en pantalla.
     */
    public void start(Stage stage) {
        this.stage = stage;
        stage.setTitle("UNO - Juego en Red");

        // --- 1. ZONA SUPERIOR (Barra de Información) ---
        VBox topPane = new VBox(10);
        topPane.setPadding(new Insets(15));
        topPane.setAlignment(Pos.CENTER);
        topPane.setStyle("-fx-background-color: #333333;"); // Fondo oscuro

        lblMiNombre.setTextFill(javafx.scene.paint.Color.WHITE);
        lblMiNombre.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        lblEstadoJuego.setTextFill(javafx.scene.paint.Color.LIGHTGRAY);
        lblEstadoJuego.setFont(Font.font("Arial", 16));

        topPane.getChildren().addAll(lblMiNombre, lblEstadoJuego);
        rootLayout.setTop(topPane);

        // --- 2. ZONA CENTRAL (La Mesa de Juego) ---
        HBox mesa = new HBox(40); // Espacio entre mazo y descarte
        mesa.setAlignment(Pos.CENTER);
        mesa.setPadding(new Insets(30));

        // Construimos el gráfico del mazo boca abajo
        construirMazoVisual();

        // Configuramos el espacio para la pila de descartes
        pilaDescartes.setPrefSize(100, 150);

        // Panel para mostrar el color actual (útil cuando se juega un +4 o Cambio Color)
        VBox panelCentralInfo = new VBox(10, new Label("Color Actual:"), indicadorColor);
        panelCentralInfo.setAlignment(Pos.CENTER);

        mesa.getChildren().addAll(mazoRobar, pilaDescartes, panelCentralInfo);
        rootLayout.setCenter(mesa);

        // --- 3. ZONA INFERIOR (Mano del Jugador) ---
        VBox bottomPane = new VBox(10);
        bottomPane.setPadding(new Insets(15));
        bottomPane.setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #cccccc; -fx-border-width: 2px 0 0 0;");

        Label lblTuMano = new Label("Tus Cartas (Click para jugar):");
        lblTuMano.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        // ScrollPane permite scrollear si el jugador acumula muchas cartas
        ScrollPane scrollMano = new ScrollPane(contenedorMano);
        scrollMano.setFitToHeight(true);
        scrollMano.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); // Ocultar barra vertical
        scrollMano.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        contenedorMano.setAlignment(Pos.CENTER_LEFT);
        contenedorMano.setPadding(new Insets(10));

        bottomPane.getChildren().addAll(lblTuMano, scrollMano);
        rootLayout.setBottom(bottomPane);

        // --- 4. ZONA DERECHA (Botones de Acción) ---
        VBox rightPane = new VBox(15);
        rightPane.setPadding(new Insets(20));
        rightPane.setAlignment(Pos.CENTER);

        // Configuración del botón Pasar Turno
        btnPasar.setStyle("-fx-background-color: #ff6b6b; -fx-text-fill: white; -fx-font-weight: bold;");
        btnPasar.setPrefWidth(100);
        btnPasar.setDisable(true); // Nace deshabilitado hasta que corresponda
        btnPasar.setOnAction(e -> accionPasarTurno());

        rightPane.getChildren().add(btnPasar);
        rootLayout.setRight(rightPane);

        // Configuración final de la escena
        Scene scene = new Scene(rootLayout, 900, 600);
        stage.setScene(scene);
        stage.show();

        // Forzamos una primera actualización para cargar el estado inicial
        this.actualizar();
    }

    /**
     * Método auxiliar que dibuja el mazo de robo (Rectángulo gris con texto "UNO").
     */
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

        // Asignamos el evento de clic para robar carta
        mazoRobar.setOnMouseClicked(e -> accionRobar());
    }

    // ================= MÉTODOS DE ACCIÓN (EVENTOS UI -> CONTROLADOR) =================

    private void accionRobar() {
        if (mazoRobar.isDisabled()) return; // Validación visual extra
        try {
            controlador.robarCarta();
            // Si el servidor valida el robo, actualizamos estado local
            yaRobe = true;
            actualizar(); // Refrescamos botones inmediatamente
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
            // No hacemos nada más, esperamos que el servidor nos mande el evento de cambio de turno
        } catch (Exception ex) {
            mostrarMensaje("Jugada Inválida", ex.getMessage());
        }
    }

    // ================= MÉTODO DE ACTUALIZACIÓN (OBSERVER) =================

    /**
     * Este método es llamado por el Controlador cada vez que llega un evento del Servidor.
     * Se encarga de redibujar toda la interfaz basándose en el estado actual del Modelo.
     */
    @Override
    public void actualizar() {
        // Platform.runLater es vital: Asegura que los cambios en la UI se hagan
        // en el "JavaFX Application Thread", evitando errores de concurrencia.
        Platform.runLater(() -> {
            try {
                // 1. Obtener estado fresco del controlador (que consulta al modelo remoto)
                boolean enCurso = controlador.isPartidaEnCurso();
                Jugador jugadorActual = controlador.obtenerJugadorActual();
                Carta ultima = controlador.obtenerUltimaCartaJugadas();
                Color color = controlador.obtenerColorActual();
                Jugador local = controlador.getJugadorLocal();

                if (local == null) return; // Aún no estamos sincronizados

                // 2. Lógica de Detección de Nuevo Turno
                // Necesitamos saber si el turno cambió o si hubo una jugada (incluso si es el mismo jugador, ej. Salto)
                // para resetear el estado de los botones (ej. volver a permitir robar).
                boolean cambioJugador = !jugadorActual.getNombre().equals(ultimoJugadorActual);
                boolean huboJugada = ultima != null && !ultima.equals(ultimaCartaRegistrada);

                if (cambioJugador || huboJugada) {
                    ultimoJugadorActual = jugadorActual.getNombre();
                    ultimaCartaRegistrada = ultima;
                    // Reseteamos bandera: en un nuevo estado de turno, aún no he robado.
                    yaRobe = false;
                }

                // 3. Actualizar textos informativos
                lblMiNombre.setText("Jugador: " + local.getNombre());
                boolean esMiTurno = local.getNombre().equals(jugadorActual.getNombre());

                if (!enCurso) {
                    lblEstadoJuego.setText("PARTIDA FINALIZADA");
                    deshabilitarTodo();
                    return;
                }

                // --- CORRECCIÓN AQUÍ: REACTIVAR LA MANO ---
                // Si llegamos acá, el juego está en curso. Aseguramos que la mano esté habilitada.
                contenedorMano.setDisable(false);
                // ------------------------------------------

                if (esMiTurno) {
                    lblEstadoJuego.setText("¡ES TU TURNO!");
                    lblEstadoJuego.setTextFill(javafx.scene.paint.Color.YELLOW);
                } else {
                    lblEstadoJuego.setText("Turno de: " + jugadorActual.getNombre());
                    lblEstadoJuego.setTextFill(javafx.scene.paint.Color.LIGHTGRAY);
                }

                // 4. Actualizar Mesa (Centro)
                actualizarPilaDescartes(ultima);
                indicadorColor.setColor(color);

                // 5. Gestión de Botones (Reglas de UI)
                // Mazo Robar: Habilitado solo si es mi turno Y no he robado aún.
                mazoRobar.setDisable(!esMiTurno || yaRobe);
                mazoRobar.setOpacity(mazoRobar.isDisabled() ? 0.5 : 1.0); // Feedback visual

                // Botón Pasar: Habilitado solo si es mi turno Y YA robé (regla obligatoria).
                btnPasar.setDisable(!esMiTurno || !yaRobe);

                // 6. Dibujar la Mano del Jugador (Cartas)
                contenedorMano.getChildren().clear();
                List<Carta> misCartas = local.getCartas();
                for (int i = 0; i < misCartas.size(); i++) {
                    Carta c = misCartas.get(i);
                    int indexFinal = i;

                    // Usamos la fábrica para crear el nodo visual de la carta
                    Node cartaVisual = CartaVisualFactory.crearCarta(c);

                    // Si es mi turno, agregamos interactividad
                    if (esMiTurno) {
                        cartaVisual.setOnMouseClicked(e -> accionJugarCarta(indexFinal));
                        cartaVisual.setCursor(javafx.scene.Cursor.HAND);
                        // Efecto de "levantar" carta al pasar el mouse
                        cartaVisual.setOnMouseEntered(e -> cartaVisual.setTranslateY(-10));
                        cartaVisual.setOnMouseExited(e -> cartaVisual.setTranslateY(0));
                    } else {
                        // Si no es mi turno, las cartas se ven opacas
                        cartaVisual.setOpacity(0.7);
                    }

                    contenedorMano.getChildren().add(cartaVisual);
                }

                // 7. Manejo de Comodines (Popup de Color)
                // Si el servidor espera color y soy yo, abro el diálogo.
                if (enCurso && controlador.isEstadoEsperandoColor() && esMiTurno && !dialogAbierto) {
                    pedirColor();
                }

            } catch (Exception ex) {
                // Si ocurre error de conexión al actualizar, fallamos silenciosamente en UI
            }
        });
    }

    private void deshabilitarTodo() {
        mazoRobar.setDisable(true);
        btnPasar.setDisable(true);
        contenedorMano.setDisable(true);
    }

    /**
     * Dibuja la última carta jugada en el centro de la mesa.
     */
    private void actualizarPilaDescartes(Carta c) {
        pilaDescartes.getChildren().clear();
        if (c != null) {
            // Reutilizamos la fábrica de cartas visuales
            Node cartaNode = CartaVisualFactory.crearCarta(c);
            pilaDescartes.getChildren().add(cartaNode);
        } else {
            // Si no hay carta (inicio), mostramos un recuadro punteado vacío
            Rectangle r = new Rectangle(100, 150, javafx.scene.paint.Color.TRANSPARENT);
            r.setStroke(javafx.scene.paint.Color.GRAY);
            r.getStrokeDashArray().addAll(5.0);
            pilaDescartes.getChildren().add(r);
        }
    }

    /**
     * Muestra un diálogo modal obligatorio para elegir color tras jugar un comodín.
     */
    private void pedirColor() {
        dialogAbierto = true;
        List<Color> colores = Arrays.asList(Color.ROJO, Color.AZUL, Color.VERDE, Color.AMARILLO);

        ChoiceDialog<Color> dlg = new ChoiceDialog<>(Color.ROJO, colores);
        dlg.setTitle("UNO");
        dlg.setHeaderText("¡Comodín jugado!");
        dlg.setContentText("Seleccioná el próximo color:");

        // Truco: Quitamos el botón Cancelar y bloqueamos el cierre con "X" para obligar a elegir.
        dlg.getDialogPane().lookupButton(ButtonType.CANCEL).setVisible(false);
        dlg.getDialogPane().getScene().getWindow().setOnCloseRequest(e -> e.consume());

        Optional<Color> result = dlg.showAndWait();
        result.ifPresent(color -> controlador.manejarCambioDeColor(color));
        dialogAbierto = false;
    }

    /**
     * Muestra alertas informativas (Ganador, Errores, UNO gritado) y tambien permite reiniciar la partida con los mismo jugadores
     */
    @Override
    public void mostrarMensaje(String titulo, String mensaje) {
        Platform.runLater(() -> {
            // Caso Especial: Si es el fin del juego, mostramos botones de acción
            if (titulo.equals("FIN DEL JUEGO")) {

                // --- NUEVO: Pedir el ranking al controlador ---
                List<String> top5 = controlador.getRankingTop5();
                StringBuilder sb = new StringBuilder();

                // Construimos el texto completo
                sb.append("--- TOP 5 MEJORES JUGADORES ---\n");
                for (String linea : top5) {
                    sb.append(linea).append("\n");
                }
                sb.append("\n¿Qué quieren hacer ahora?"); // Agregamos la pregunta al final del texto
                // ----------------------------------------------

                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Juego Terminado");
                alert.setHeaderText(mensaje); // "¡Ha ganado [Nombre]!"
                alert.setContentText(sb.toString()); // Mostramos Ranking + Pregunta

                ButtonType btnOtra = new ButtonType("Jugar Otra Vez");
                ButtonType btnSalir = new ButtonType("Salir", ButtonBar.ButtonData.CANCEL_CLOSE);

                alert.getButtonTypes().setAll(btnOtra, btnSalir);

                // Bloqueamos cierre con X para obligar a decidir
                alert.getDialogPane().getScene().getWindow().setOnCloseRequest(e -> e.consume());

                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == btnOtra) {
                    // Llamamos al reinicio
                    controlador.solicitarReiniciarPartida();
                } else {
                    // 1. AVISAR AL SERVIDOR QUE ME VOY
                    controlador.cerrarCesion();

                    // 2. CERRAR LOCALMENTE
                    Platform.exit();
                    System.exit(0);
                }
            }
            else {
                // Mensajes normales (UNO, Errores, etc.)
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Información");
                alert.setHeaderText(titulo);
                alert.setContentText(mensaje);
                alert.show();
            }
        });
    }

    // ================= CLASES INTERNAS (Helpers Visuales) =================

    /**
     * Fábrica estática encargada de "dibujar" las cartas usando formas geométricas de JavaFX.
     * Esto evita depender de archivos de imagen externos (.png/.jpg).
     */
    private static class CartaVisualFactory {
        public static Node crearCarta(Carta carta) {
            StackPane root = new StackPane();
            root.setPrefSize(100, 150);

            // 1. Base blanca (Borde)
            Rectangle base = new Rectangle(100, 150);
            base.setArcWidth(15);
            base.setArcHeight(15);
            base.setFill(javafx.scene.paint.Color.WHITE);
            base.setStroke(javafx.scene.paint.Color.BLACK);

            // 2. Interior con el color de la carta
            Rectangle interior = new Rectangle(90, 140);
            interior.setArcWidth(10);
            interior.setArcHeight(10);
            interior.setFill(obtenerPintura(carta.getColor()));

            // 3. Texto (Número o Símbolo)
            String texto = obtenerTextoCarta(carta);
            Text valor = new Text(texto);
            valor.setFont(Font.font("Arial", FontWeight.BOLD, 40));
            valor.setFill(javafx.scene.paint.Color.WHITE);
            valor.setStroke(javafx.scene.paint.Color.BLACK);
            valor.setStrokeWidth(1.5);

            // Sombra
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
                default -> javafx.scene.paint.Color.BLACK; // Negro para comodines
            };
        }

        private static String obtenerTextoCarta(Carta c) {
            Numero n = c.getValor();
            return switch (n) {
                case CERO->"0"; case UNO->"1"; case DOS->"2"; case TRES->"3"; case CUATRO->"4";
                case CINCO->"5"; case SEIS->"6"; case SIETE->"7"; case OCHO->"8"; case NUEVE->"9";
                case MASDOS -> "+2";
                case SALTARSE -> "⊘"; // Símbolo de bloqueo
                case CAMBIOSENTIDO -> "⇄"; // Flechas
                case MASCUATRO -> "+4";
                case CAMBIOCOLOR -> "🎨";
                default -> "?";
            };
        }
    }

    /**
     * Componente visual simple: Un círculo que muestra un color.
     */
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