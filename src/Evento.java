import java.util.HashMap;
import java.util.Map;

/**
 * Evento de log de Windows que viaja por el pipeline.
 * Cada decorador puede leer y modificar sus datos y su puntaje.
 */
public class Evento {

    private final int id;
    private final Map<String, String> datos = new HashMap<>();
    private int puntaje = 0;

    public Evento(int id) {
        this.id = id;
    }

    /** Event ID de Windows (4663, 4688, 1102, ...). */
    public int getId() {
        return id;
    }

    public String get(String clave) {
        return datos.get(clave);
    }

    public void put(String clave, String valor) {
        datos.put(clave, valor);
    }

    public int getPuntaje() {
        return puntaje;
    }

    public void sumarPuntaje(int puntos) {
        puntaje += puntos;
    }
}
