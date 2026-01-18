package Vista;

import Controlador.ControladorUNO;
import Controlador.VistaObserver;
import Modelo.Carta;
import Modelo.Color;
import Modelo.Jugador;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.util.List;

/**
 * Vista de Consola "Falsa" (Simulada con JavaFX).
 * <p>
 * Versión Final: Incluye Ranking, Reconexión y Gestión de Lobby.
 */
public class VistaConsola implements VistaObserver {

    private final ControladorUNO controlador;
    private Stage stage;

    // Componentes visuales
    private TextArea outputArea;
    private TextField inputField;

    // Estados
    private enum EstadoConsola { LOGIN, ESPERA, JUEGO, ELIGIENDO_COLOR, FIN_JUEGO }
    private EstadoConsola estadoActual = EstadoConsola.LOGIN;

    private boolean soyYo = false;

    public VistaConsola(ControladorUNO controlador) {
        this.controlador = controlador;
        controlador.registrarVista(this);
    }

    public void start(Stage stage) {
        this.stage = stage;
        stage.setTitle("UNO - Terminal Mode");

        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setStyle("-fx-control-inner-background: black; -fx-text-fill: #00ff00; -fx-font-family: 'Consolas', 'Monospaced'; -fx-font-size: 14px;");
        outputArea.setWrapText(true);

        inputField = new TextField();
        inputField.setStyle("-fx-background-color: #222; -fx-text-fill: #00ff00; -fx-font-family: 'Consolas', 'Monospaced'; -fx-font-size: 14px;");
        inputField.setPromptText("Escribí tu comando aquí...");

        inputField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                procesarEntrada(inputField.getText());
                inputField.clear();
            }
        });

        BorderPane layout = new BorderPane();
        layout.setCenter(outputArea);
        layout.setBottom(inputField);
        BorderPane.setMargin(inputField, new Insets(5));

        Scene scene = new Scene(layout, 600, 500);
        stage.setScene(scene);
        stage.show();

        imprimir(">>> SISTEMA UNO INICIADO v2.0");
        imprimir(">>> Por favor, ingresá tu nombre para conectarte:");
        imprimir("------------------------------------------------");
        inputField.requestFocus();
    }

    // ================= LÓGICA DE COMANDOS =================

    private void procesarEntrada(String texto) {
        String cmd = texto.trim();
        if (cmd.isEmpty()) return;

        imprimir("> " + cmd);

        try {
            switch (estadoActual) {
                case LOGIN -> {
                    controlador.setNombreLocal(cmd);
                    controlador.registrarJugador(cmd);
                    estadoActual = EstadoConsola.ESPERA;

                    imprimir(">>> Conectado al Lobby.");

                    // --- NUEVO: Mostrar quiénes están conectados ---
                    List<String> actuales = controlador.obtenerNombresJugadores();
                    imprimir(">>> Jugadores en sala: " + actuales);
                    // -----------------------------------------------

                    imprimir(">>> Escribí 'START' para iniciar si hay suficientes jugadores.");
                }
                case ESPERA -> {
                    if (cmd.equalsIgnoreCase("START")) {
                        controlador.solicitarInicioPartida();
                    } else {
                        imprimir("Comando desconocido en espera. Usá 'START'.");
                    }
                }
                case JUEGO -> procesarComandoJuego(cmd);
                case ELIGIENDO_COLOR -> procesarSeleccionColor(cmd);

                // --- MODO FIN DE JUEGO (Replay Loop) ---
                case FIN_JUEGO -> {
                    if (cmd.equalsIgnoreCase("REINICIAR") || cmd.equalsIgnoreCase("R")) {
                        controlador.solicitarReiniciarPartida();
                        // Si falla, el controlador mandará mensaje de error.
                        // Si funciona, llegará evento INICIO_PARTIDA y 'actualizar' cambiará el estado.
                    } else if (cmd.equalsIgnoreCase("SALIR") || cmd.equalsIgnoreCase("EXIT")) {
                        // Desconexión limpia
                        controlador.cerrarCesion();
                        Platform.exit();
                        System.exit(0);
                    } else {
                        imprimir("Opción inválida. Escribí 'REINICIAR' o 'SALIR'.");
                    }
                }
            }
        } catch (Exception e) {
            imprimir("ERROR LOCAL: " + e.getMessage());
        }
    }

    private void procesarComandoJuego(String cmd) {
        String[] partes = cmd.split(" ");
        String accion = partes[0].toUpperCase();

        try {
            switch (accion) {
                case "JUGAR", "PLAY", "P" -> {
                    if (partes.length < 2) {
                        imprimir("Uso: JUGAR <numero_indice>");
                        return;
                    }
                    int idx = Integer.parseInt(partes[1]);
                    controlador.jugarCarta(idx);
                }
                case "ROBAR", "DRAW", "R" -> controlador.robarCarta();
                case "PASAR", "PASS" -> controlador.pasarTurno();
                case "MANO", "HAND" -> mostrarMano();
                case "AYUDA", "HELP", "?" -> mostrarAyuda();
                case "SALIR", "EXIT" -> {
                    controlador.cerrarCesion();
                    Platform.exit();
                    System.exit(0);
                }
                default -> imprimir("Comando no reconocido. Escribí 'AYUDA'.");
            }
        } catch (NumberFormatException e) {
            imprimir("Error: El índice debe ser un número.");
        } catch (Exception e) {
            imprimir("Error: " + e.getMessage());
        }
    }

    private void procesarSeleccionColor(String colorStr) {
        try {
            Color c = switch (colorStr.toUpperCase()) {
                case "ROJO", "RED", "1" -> Color.ROJO;
                case "AZUL", "BLUE", "2" -> Color.AZUL;
                case "VERDE", "GREEN", "3" -> Color.VERDE;
                case "AMARILLO", "YELLOW", "4" -> Color.AMARILLO;
                default -> null;
            };

            if (c == null) {
                imprimir("Color inválido. Opciones: ROJO, AZUL, VERDE, AMARILLO.");
                return;
            }
            controlador.manejarCambioDeColor(c);
            estadoActual = EstadoConsola.JUEGO;

        } catch (Exception e) {
            imprimir("Error al cambiar color: " + e.getMessage());
        }
    }

    private void mostrarAyuda() {
        imprimir("--- COMANDOS ---");
        imprimir(" JUGAR <n> : Tira carta n");
        imprimir(" ROBAR     : Toma carta");
        imprimir(" PASAR     : Pasa turno (tras robar)");
        imprimir(" MANO      : Ver cartas");
        imprimir(" SALIR     : Desconectar");
    }

    // ================= ACTUALIZACIONES =================

    @Override
    public void actualizar() {
        Platform.runLater(() -> {
            try {
                // 1. ESCUDO DE LOGIN (Esto es lo que faltaba)
                // Si el usuario todavía está escribiendo su nombre, ignoramos cualquier evento del servidor.
                if (estadoActual == EstadoConsola.LOGIN) return;

                // 2. Si la partida NO está en curso...
                if (!controlador.isPartidaEnCurso()) {
                    // Si estoy en espera, me quedo tranquilo (el controlador actualiza la lista interna si hace falta)
                    // Si estaba jugando y la partida se cortó (ej. todos se desconectaron), no hacemos nada especial aquí.
                    return;
                }

                // 3. Transición Automática (Inicio o Reinicio)
                // Si la partida SÍ está en curso, pero yo estoy en modo "Espera" o "Fin de Juego"...
                if (estadoActual == EstadoConsola.ESPERA || estadoActual == EstadoConsola.FIN_JUEGO) {
                    estadoActual = EstadoConsola.JUEGO; // ¡Fuerzo el estado a JUEGO!
                    imprimir("\n>>> ¡LA PARTIDA HA COMENZADO (O REINICIADO)! <<<\n");
                }

                // 4. Si llegué acá, es porque estoy jugando. Imprimo la mesa.
                imprimirEstadoJuego();

            } catch (Exception e) {
                // Manejo silencioso de errores de UI
            }
        });
    }

    private void imprimirEstadoJuego() {
        Jugador actual = controlador.obtenerJugadorActual();
        Carta ultima = controlador.obtenerUltimaCartaJugadas();
        Color color = controlador.obtenerColorActual();
        Jugador yo = controlador.getJugadorLocal();

        if (yo == null) return;

        soyYo = yo.getNombre().equals(actual.getNombre());

        StringBuilder sb = new StringBuilder();
        sb.append("\n========================================\n");
        sb.append(" MESA: [").append(ultima).append("]  |  COLOR: ").append(color).append("\n");
        sb.append(" TURNO: ").append(actual.getNombre());
        if (soyYo) sb.append(" (¡SOS VOS!)");
        sb.append("\n========================================\n");

        imprimir(sb.toString());
        mostrarMano();

        if (soyYo && controlador.isEstadoEsperandoColor()) {
            estadoActual = EstadoConsola.ELIGIENDO_COLOR;
            imprimir(">>> ¡COMODÍN! Escribí el color (ROJO, AZUL, VERDE, AMARILLO):");
        }
    }

    private void mostrarMano() {
        Jugador yo = controlador.getJugadorLocal();
        if (yo == null) return;

        imprimir("TUS CARTAS:");
        List<Carta> mano = yo.getCartas();
        for (int i = 0; i < mano.size(); i++) {
            imprimir(" [" + i + "] " + mano.get(i));
        }
        imprimir("");
    }

    @Override
    public void mostrarMensaje(String titulo, String mensaje) {
        Platform.runLater(() -> {

            // --- MOSTRAR RANKING AL FINALIZAR ---
            if (titulo.equals("FIN DEL JUEGO")) {
                imprimir("\n*********************************");
                imprimir("       " + titulo + ": " + mensaje);
                imprimir("*********************************");

                // Pedimos el Ranking Top 5
                List<String> top5 = controlador.getRankingTop5();
                imprimir("\n--- 🏆 TOP 5 MEJORES JUGADORES 🏆 ---");
                for (String linea : top5) {
                    imprimir(" " + linea);
                }
                imprimir("---------------------------------------");

                estadoActual = EstadoConsola.FIN_JUEGO;
                imprimir("\n>>> ¿Qué querés hacer?");
                imprimir(">>> Escribí 'REINICIAR' para jugar otra vez.");
                imprimir(">>> Escribí 'SALIR' para cerrar.");
            }
            else {
                // Mensajes normales (UNO, Avisos)
                imprimir("\n*** " + titulo + ": " + mensaje + " ***\n");
            }
        });
    }

    private void imprimir(String texto) {
        outputArea.appendText(texto + "\n");
    }
}