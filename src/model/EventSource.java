package model;

/**
 * Componente del patron Decorator: entrega el siguiente evento o null cuando no hay mas.
 */
public interface EventSource {

    Event next();
}
