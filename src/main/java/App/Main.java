package App;

import Controlador.ControladorUNO;
import Vista.VistaLoginJavaFX;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Clase principal (Entry Point) de la aplicación Cliente.
 * <p>
 * Su responsabilidad es:
 * 1. Iniciar el ciclo de vida de la aplicación JavaFX.
 * 2. Instanciar el Controlador local ({@link ControladorUNO}).
 * 3. Establecer la conexión de red RMI con el Servidor.
 * 4. Cargar la primera vista gráfica (Login).
 */
public class Main extends Application {

    /**
     * Método de inicio de JavaFX. Se ejecuta automáticamente al lanzar la aplicación.
     * Relaciona los componentes MVC locales con el sistema distribuido RMI.
     *
     * @param stage El escenario principal (ventana) provisto por JavaFX.
     */
    @Override
    public void start(Stage stage) {
        // 1. Instanciamos el Controlador local.
        // Este objeto gestionará la comunicación entre las Vistas (GUI) y el Modelo Remoto.
        ControladorUNO controlador = new ControladorUNO();

        // 2. Configuración de Red.
        // IP del cliente (localhost para pruebas).
        String hostCliente = "127.0.0.1";
        // Generamos un puerto aleatorio (2001-2500) para permitir ejecutar
        // múltiples clientes en la misma computadora sin conflicto de puertos.
        int portCliente = 2001 + (int)(Math.random() * 500);

        // Datos de conexión del Servidor (donde está alojado el Modelo).
        String hostServidor = "127.0.0.1";
        int portServidor = 1099; // Puerto por defecto del registro RMI.

        try {
            // 3. Conexión RMI.
            // Instanciamos el Cliente de la librería RMIMVC que se encarga de la red.
            ar.edu.unlu.rmimvc.cliente.Cliente cliente =
                    new ar.edu.unlu.rmimvc.cliente.Cliente(hostCliente, portCliente, hostServidor, portServidor);

            // Iniciamos la conexión:
            // Esto busca el Modelo remoto (IPartidaRemota) en el servidor y
            // se lo inyecta al controlador mediante setModeloRemoto().
            cliente.iniciar(controlador);

            System.out.println("✔ Cliente RMI iniciado y controlador ligado al modelo remoto.");
        } catch (Exception e) {
            e.printStackTrace();
            // Si falla la conexión, abortamos el inicio para no abrir una ventana sin lógica.
            return;
        }

        // 4. Iniciar la Interfaz Gráfica (Vista).
        // Le pasamos el controlador ya conectado para que la vista pueda enviar acciones.
        VistaLoginJavaFX login = new VistaLoginJavaFX(controlador);
        login.start(stage);
    }

    /**
     * Punto de entrada estándar de Java.
     * Llama a launch() para arrancar el thread de JavaFX.
     */
    public static void main(String[] args) {
        launch(args);
    }
}