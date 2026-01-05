package App;

import Controlador.ControladorUNO;
import Vista.VistaConsola;
import ar.edu.unlu.rmimvc.cliente.Cliente;
import javafx.application.Application;
import javafx.stage.Stage;

// Esta es la clase que extiende Application
public class MainConsola extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // 1. Iniciar Controlador
            ControladorUNO controlador = new ControladorUNO();

            // 2. Configurar Cliente RMI
            String hostCliente = "127.0.0.1";
            int portCliente = 3000 + (int)(Math.random() * 1000);
            String hostServidor = "127.0.0.1";
            int portServidor = 1099;

            Cliente cliente = new Cliente(hostCliente, portCliente, hostServidor, portServidor);
            cliente.iniciar(controlador);

            System.out.println("✔ Consola conectada al servidor RMI.");

            // 3. Iniciar Vista Consola
            VistaConsola vista = new VistaConsola(controlador);
            vista.start(primaryStage);

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}