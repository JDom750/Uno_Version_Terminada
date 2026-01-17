package Modelo;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class SerializadorRanking implements Serializable {
    private static final String ARCHIVO_RANKING = "ranking_uno.dat";
    private Map<String, Integer> puntuaciones;

    public SerializadorRanking() {
        this.puntuaciones = cargarRanking();
    }

    // Suma una victoria al jugador y guarda en disco
    public synchronized void registrarVictoria(String nombre) {
        puntuaciones.put(nombre, puntuaciones.getOrDefault(nombre, 0) + 1);
        guardarRanking();
    }

    // Devuelve el Top 5 formateado como texto
    public synchronized List<String> getTop5() {
        return puntuaciones.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue())) // Ordenar descendente
                .limit(5) // Solo los 5 mejores
                .map(e -> e.getKey() + " - " + e.getValue() + " Victorias")
                .collect(Collectors.toList());
    }

    // Guarda el Map en un archivo
    private void guardarRanking() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(ARCHIVO_RANKING))) {
            oos.writeObject(puntuaciones);
        } catch (IOException e) {
            System.err.println("Error al guardar ranking: " + e.getMessage());
        }
    }

    // Carga el Map desde el archivo (o crea uno nuevo si no existe)
    @SuppressWarnings("unchecked")
    private Map<String, Integer> cargarRanking() {
        File archivo = new File(ARCHIVO_RANKING);
        if (!archivo.exists()) {
            return new HashMap<>();
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(archivo))) {
            return (Map<String, Integer>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return new HashMap<>();
        }
    }
}