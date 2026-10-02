/**
 * Component del patrón Decorator.
 * Tanto las fuentes reales como los decoradores implementan esta interfaz.
 */
public interface FuenteEventos {

    /** Devuelve el siguiente evento, o null cuando no hay más eventos. */
    Evento siguiente();
}
