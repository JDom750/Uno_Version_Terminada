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
 * Provee una interfaz de texto retro (tipo terminal) para jugar UNO mediante comandos escritos.
 * Implementa {@link VistaObserver} para reaccionar a los eventos del juego.
 * <p>
 * Diferencia con una consola real (System.out):
 * - No bloquea el hilo principal al esperar input (usa eventos de JavaFX).
 * - Permite actualizaciones asíncronas del servidor sin "romper" la línea de entrada.
 */
public class VistaConsola implements VistaObserver {

    private final ControladorUNO controlador;
    private Stage stage;

    // Componentes de la interfaz simulada
    private TextArea outputArea; // Pantalla de salida (Logs del juego)
    private TextField inputField; // Línea de comandos (Input del usuario)

    // Máquina de estados interna para saber cómo interpretar lo que escribe el usuario.
    private enum EstadoConsola { LOGIN, ESPERA, JUEGO, ELIGIENDO_COLOR }
    private EstadoConsola estadoActual = EstadoConsola.LOGIN;

    private boolean soyYo = false; // Indicador visual de turno

    public VistaConsola(ControladorUNO controlador) {
        this.controlador = controlador;
        // Nos registramos para escuchar al modelo
        controlador.registrarVista(this);
    }

    /**
     * Inicia la ventana de la terminal.
     */
    public void start(Stage stage) {
        this.stage = stage;
        stage.setTitle("UNO - Terminal Mode");

        // Configuración visual estilo "Matrix / Hacker" (Fondo negro, letras verdes)
        outputArea = new TextArea();
        outputArea.setEditable(false); // El usuario no puede borrar el historial
        outputArea.setStyle("-fx-control-inner-background: black; -fx-text-fill: #00ff00; -fx-font-family: 'Consolas', 'Monospaced'; -fx-font-size: 14px;");
        outputArea.setWrapText(true);

        inputField = new TextField();
        inputField.setStyle("-fx-background-color: #222; -fx-text-fill: #00ff00; -fx-font-family: 'Consolas', 'Monospaced'; -fx-font-size: 14px;");
        inputField.setPromptText("Escribí tu comando aquí...");

        // Evento: Al presionar ENTER, enviamos el comando al procesador
        inputField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                procesarEntrada(inputField.getText());
                inputField.clear();
            }
        });

        // Layout simple: Salida al centro, Entrada abajo
        BorderPane layout = new BorderPane();
        layout.setCenter(outputArea);
        layout.setBottom(inputField);
        BorderPane.setMargin(inputField, new Insets(5));

        Scene scene = new Scene(layout, 600, 500);
        stage.setScene(scene);
        stage.show();

        imprimir(">>> SISTEMA UNO INICIADO v1.0");
        imprimir(">>> Por favor, ingresá tu nombre para conectarte:");
        imprimir("------------------------------------------------");
        inputField.requestFocus();
    }

    // ================= LÓGICA DE COMANDOS (PARSER) =================

    /**
     * Interpreta el texto ingresado por el usuario según el estado actual.
     */
    private void procesarEntrada(String texto) {
        String cmd = texto.trim();
        if (cmd.isEmpty()) return;

        imprimir("> " + cmd); // Echo: mostramos lo que escribió el usuario

        try {
            switch (estadoActual) {
                case LOGIN -> {
                    // El primer comando es el nombre de usuario
                    controlador.setNombreLocal(cmd);
                    controlador.registrarJugador(cmd);
                    estadoActual = EstadoConsola.ESPERA;
                    imprimir(">>> Conectado. Esperando jugadores...");
                    imprimir(">>> Escribí 'START' para iniciar si hay suficientes jugadores.");
                }
                case ESPERA -> {
                    // En el lobby, solo esperamos el comando de inicio
                    if (cmd.equalsIgnoreCase("START")) {
                        controlador.solicitarInicioPartida();
                    } else {
                        imprimir("Comando desconocido en espera. Usá 'START'.");
                    }
                }
                case JUEGO -> procesarComandoJuego(cmd); // Comandos de partida (JUGAR, ROBAR)
                case ELIGIENDO_COLOR -> procesarSeleccionColor(cmd); // Input específico tras comodín
            }
        } catch (Exception e) {
            imprimir("ERROR: " + e.getMessage());
        }
    }

    /**
     * Parsea los comandos principales del juego.
     * Ejemplos: "JUGAR 0", "ROBAR", "PASAR".
     */
    private void procesarComandoJuego(String cmd) {
        String[] partes = cmd.split(" ");
        String accion = partes[0].toUpperCase();

        try {
            switch (accion) {
                case "JUGAR", "PLAY", "P" -> {
                    if (partes.length < 2) {
                        imprimir("Uso: JUGAR <numero_indice> (Ej: JUGAR 0)");
                        return;
                    }
                    int idx = Integer.parseInt(partes[1]);
                    controlador.jugarCarta(idx);
                }
                case "ROBAR", "DRAW", "R" -> controlador.robarCarta();
                case "PASAR", "PASS" -> controlador.pasarTurno();
                case "AYUDA", "HELP", "?" -> mostrarAyuda();
                case "MANO", "HAND" -> mostrarMano(); // Ver mano sin esperar turno
                default -> imprimir("Comando no reconocido. Escribí 'AYUDA'.");
            }
        } catch (NumberFormatException e) {
            imprimir("Error: El índice debe ser un número.");
        } catch (Exception e) {
            imprimir("Error del servidor: " + e.getMessage());
        }
    }

    /**
     * Parsea la elección de color.
     */
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
            estadoActual = EstadoConsola.JUEGO; // Volvemos al modo normal

        } catch (Exception e) {
            imprimir("Error al cambiar color: " + e.getMessage());
        }
    }

    private void mostrarAyuda() {
        imprimir("--- COMANDOS DISPONIBLES ---");
        imprimir(" JUGAR <n> : Tira la carta en la posición n (0, 1, 2...)");
        imprimir(" ROBAR     : Agarra una carta del mazo");
        imprimir(" PASAR     : Pasa el turno (si ya robaste)");
        imprimir(" MANO      : Muestra tus cartas de nuevo");
    }

    // ================= ACTUALIZACIONES DEL SERVIDOR =================

    /**
     * Recibe la notificación de cambio del modelo y actualiza el texto en pantalla.
     */
    @Override
    public void actualizar() {
        Platform.runLater(() -> {
            try {
                if (!controlador.isPartidaEnCurso()) {
                    if (estadoActual == EstadoConsola.LOGIN) return; // Aún no entramos
                    return;
                }

                // Transición automática de Espera a Juego
                if (estadoActual == EstadoConsola.ESPERA) {
                    estadoActual = EstadoConsola.JUEGO;
                    imprimir("\n>>> ¡LA PARTIDA HA COMENZADO! <<<\n");
                }

                imprimirEstadoJuego();

            } catch (Exception e) {
                // Manejo de errores de conexión silencioso
            }
        });
    }

    /**
     * Imprime el estado actual de la mesa (Carta en el pozo, Color, Turno).
     */
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

        // Chequear si debo elegir color
        if (soyYo && controlador.isEstadoEsperandoColor()) {
            estadoActual = EstadoConsola.ELIGIENDO_COLOR;
            imprimir(">>> ¡COMODÍN JUGADO! Escribí el color (ROJO, AZUL, VERDE, AMARILLO):");
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
        imprimir(""); // linea vacia
    }

    @Override
    public void mostrarMensaje(String titulo, String mensaje) {
        Platform.runLater(() -> imprimir("\n************\n" + titulo + ": " + mensaje + "\n************\n"));
    }

    /**
     * Agrega texto al área de salida (Log).
     */
    private void imprimir(String texto) {
        outputArea.appendText(texto + "\n");
    }
}