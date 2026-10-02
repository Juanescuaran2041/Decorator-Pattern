import java.util.ArrayList;
import java.util.List;

/**
 * Genera los eventos de prueba del caso de estudio.
 */
public class DatosPrueba {

    /** Devuelve una lista nueva en cada llamada, porque los decoradores modifican los eventos. */
    public static List<Evento> generar() {
        List<Evento> eventos = new ArrayList<>();

        // Ransomware: acceso masivo a archivos ya cifrados.
        for (int i = 1; i <= 120; i++) {
            eventos.add(crear(4663, "C:\\Users\\victima\\AppData\\Local\\Temp\\EVIL.EXE",
                    "documento_" + i + ".docx.locked"));
        }

        // Uso normal de Word.
        for (int i = 1; i <= 5; i++) {
            eventos.add(crear(4663, "C:\\Program Files\\Office\\WINWORD.EXE",
                    "informe_" + i + ".docx"));
        }

        // Creación de procesos.
        for (int i = 1; i <= 3; i++) {
            eventos.add(crear(4688, "C:\\Windows\\System32\\CMD.EXE", null));
        }

        // Borrado del log de auditoría.
        eventos.add(crear(1102, null, null));

        // Inicios de sesión: deben quedar filtrados por ser OTRO.
        for (int i = 1; i <= 10; i++) {
            eventos.add(crear(4624, null, null));
        }
        return eventos;
    }

    private static Evento crear(int id, String proceso, String archivo) {
        Evento evento = new Evento(id);
        if (proceso != null) {
            evento.put("proceso", proceso);
        }
        if (archivo != null) {
            evento.put("archivo", archivo);
        }
        return evento;
    }
}
