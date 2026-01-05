package RMI;

import Modelo.Partida;
import Modelo.IPartidaRemota;
import ar.edu.unlu.rmimvc.servidor.Servidor;
import ar.edu.unlu.rmimvc.RMIMVCException;

public class MainServidor {
    public static void main(String[] args) throws Exception {
        String host = "127.0.0.1";
        int port = 1099; // RMI registry del servidor

        Servidor servidor = new Servidor(host, port);
        IPartidaRemota stub = (IPartidaRemota) servidor.iniciar(new Partida());
        System.out.println("Servidor UNO publicado en " + host + ":" + port + " como MVCRMI/Modelo.");
    }
}
