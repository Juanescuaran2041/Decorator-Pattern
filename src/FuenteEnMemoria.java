import java.util.Iterator;
import java.util.List;

/**
 * ConcreteComponent: entrega los eventos de una lista en memoria.
 * Simula la lectura del Event Log con datos de prueba.
 */
public class FuenteEnMemoria implements FuenteEventos {

    private final Iterator<Evento> iterador;

    public FuenteEnMemoria(List<Evento> eventos) {
        this.iterador = eventos.iterator();
    }

    @Override
    public Evento siguiente() {
        return iterador.hasNext() ? iterador.next() : null;
    }
}
