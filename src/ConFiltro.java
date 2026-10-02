/**
 * Decorador que descarta los eventos con categoría OTRO.
 * Pide eventos a la fuente interior hasta encontrar uno válido o llegar a null.
 */
public class ConFiltro extends DecoradorFuente {

    public ConFiltro(FuenteEventos fuente) {
        super(fuente);
    }

    @Override
    public Evento siguiente() {
        Evento evento = fuente.siguiente();
        while (evento != null && "OTRO".equals(evento.get("categoria"))) {
            evento = fuente.siguiente();
        }
        return evento;
    }
}
