/**
 * Decorator abstracto: envuelve otra FuenteEventos.
 * Cada decorador concreto pide el evento a esta fuente interior y le añade un comportamiento.
 */
public abstract class DecoradorFuente implements FuenteEventos {

    protected final FuenteEventos fuente;

    protected DecoradorFuente(FuenteEventos fuente) {
        this.fuente = fuente;
    }
}
