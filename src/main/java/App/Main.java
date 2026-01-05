package App;

import Controlador.ControladorUNO;
import Vista.VistaLoginJavaFX;
import ar.edu.unlu.rmimvc.cliente.Cliente;
import javafx.application.Application;
import javafx.stage.Stage;
import ar.edu.unlu.rmimvc.*;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        // 1) Crear el controlador local (aún sin modelo remoto)
        ControladorUNO controlador = new ControladorUNO();

        // 2) Conectar al servidor RMI usando la clase Cliente de la librería de la cátedra
        //    Ajustá host/puerto si necesitás (ahora usa localhost y 1099 por defecto)
        String hostCliente = "127.0.0.1";
        int portCliente = 2001 + (int)(Math.random() * 500);
        String hostServidor = "127.0.0.1";
        int portServidor = 1099;

        try {
            ar.edu.unlu.rmimvc.cliente.Cliente cliente =
                    new ar.edu.unlu.rmimvc.cliente.Cliente(hostCliente, portCliente, hostServidor, portServidor);

            // Este método inicia el RMI local, busca el modelo remoto y hace:
            // controlador.setModeloRemoto(modeloRemoto) y registra el controlador como observador.
            cliente.iniciar(controlador);

            System.out.println("✔ Cliente RMI iniciado y controlador ligado al modelo remoto.");
        } catch (Exception e) {
            e.printStackTrace();
            // Si no podés conectarte, conviene mostrar un diálogo o cerrar la app.
            // Por ahora mostramos un mensaje y salimos del start para no abrir la UI sin modelo.
            return;
        }

        // 3) Abrir la vista de login (ya con el controlador conectado al modelo remoto)
        VistaLoginJavaFX login = new VistaLoginJavaFX(controlador);
        login.start(stage);
    }


    public static void main(String[] args) {
        launch(args);
    }
}

