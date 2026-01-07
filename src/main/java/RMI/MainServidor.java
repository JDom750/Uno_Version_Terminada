package RMI;

import Modelo.Partida;
import Modelo.IPartidaRemota;
import ar.edu.unlu.rmimvc.servidor.Servidor;

/**
 * Clase principal (Entry Point) del Servidor.
 * <p>
 * Su única responsabilidad es iniciar la infraestructura de red RMI:
 * 1. Levantar el "RMI Registry" (el directorio telefónico de objetos remotos).
 * 2. Instanciar el Modelo real ({@link Partida}).
 * 3. "Publicar" ese modelo para que sea accesible remotamente por los clientes.
 * <p>
 * Una vez ejecutado, este proceso debe quedar corriendo para que el juego funcione.
 */
public class MainServidor {

    public static void main(String[] args) throws Exception {
        // IP donde escuchará el servidor (localhost para pruebas locales).
        // Si se juega en LAN, aquí iría la IP real de esta máquina (ej. 192.168.1.X).
        String host = "127.0.0.1";

        // Puerto estándar para el registro RMI (1099 es el default de Java).
        int port = 1099;

        // 1. Instanciamos la clase Servidor de la librería RMIMVC.
        // Esta clase encapsula la complejidad de configurar RMI manualmente.
        Servidor servidor = new Servidor(host, port);

        // 2. Iniciamos el servicio publicando una nueva instancia de 'Partida'.
        // El método iniciar():
        //    a) Crea el objeto Partida (el Modelo).
        //    b) Genera un "Stub" (un objeto falso que representa al modelo en la red).
        //    c) Lo registra con un nombre para que los clientes lo encuentren.
        IPartidaRemota stub = (IPartidaRemota) servidor.iniciar(new Partida());

        System.out.println("Servidor UNO publicado en " + host + ":" + port + " como MVCRMI/Modelo.");
        System.out.println("Esperando conexiones de clientes...");
    }
}
