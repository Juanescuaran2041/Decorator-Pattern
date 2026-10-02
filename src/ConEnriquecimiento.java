/**
 * Decorador que añade el campo "categoria" según el Event ID de Windows.
 */
public class ConEnriquecimiento extends DecoradorFuente {

    public ConEnriquecimiento(FuenteEventos fuente) {
        super(fuente);
    }

    @Override
    public Evento siguiente() {
        Evento evento = fuente.siguiente();
        if (evento == null) {
            return null;
        }

        evento.put("categoria", categoriaDe(evento.getId()));
        return evento;
    }

    private String categoriaDe(int id) {
        switch (id) {
            case 4663:
                return "ACCESO_ARCHIVO";
            case 4660:
                return "BORRADO_ARCHIVO";
            case 4688:
                return "CREACION_PROCESO";
            case 1102:
                return "LOG_BORRADO";
            default:
                return "OTRO";
        }
    }
}
