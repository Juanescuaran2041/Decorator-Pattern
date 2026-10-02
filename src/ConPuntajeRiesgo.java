import java.util.HashMap;
import java.util.Map;

/**
 * Decorador con estado que suma puntaje de riesgo de ransomware a cada evento.
 */
public class ConPuntajeRiesgo extends DecoradorFuente {

    private static final int UMBRAL_ACCESOS = 100;

    // Conteo acumulado de accesos por proceso, sin ventana de tiempo (simplificación intencional).
    private final Map<String, Integer> accesosPorProceso = new HashMap<>();

    public ConPuntajeRiesgo(FuenteEventos fuente) {
        super(fuente);
    }

    @Override
    public Evento siguiente() {
        Evento evento = fuente.siguiente();
        if (evento == null) {
            return null;
        }

        String categoria = evento.get("categoria");
        String proceso = evento.get("proceso");
        String archivo = evento.get("archivo");

        if ("ACCESO_ARCHIVO".equals(categoria) && proceso != null) {
            int total = accesosPorProceso.getOrDefault(proceso, 0) + 1;
            accesosPorProceso.put(proceso, total);
            if (total > UMBRAL_ACCESOS) {
                evento.sumarPuntaje(50);
            }
        }

        if (archivo != null && tieneExtensionCifrada(archivo.toLowerCase())) {
            evento.sumarPuntaje(30);
        }

        if ("LOG_BORRADO".equals(categoria)) {
            evento.sumarPuntaje(50);
        }
        return evento;
    }

    private boolean tieneExtensionCifrada(String archivo) {
        return archivo.endsWith(".locked")
                || archivo.endsWith(".encrypted")
                || archivo.endsWith(".crypt");
    }
}
