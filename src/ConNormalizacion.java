/**
 * Decorador que deja en "proceso" solo el nombre del ejecutable, en minúsculas.
 * Ejemplo: C:\Windows\System32\CMD.EXE -> cmd.exe
 */
public class ConNormalizacion extends DecoradorFuente {

    public ConNormalizacion(FuenteEventos fuente) {
        super(fuente);
    }

    @Override
    public Evento siguiente() {
        Evento evento = fuente.siguiente();
        if (evento == null) {
            return null;
        }

        String proceso = evento.get("proceso");
        if (proceso != null) {
            String ejecutable = proceso.substring(proceso.lastIndexOf('\\') + 1);
            evento.put("proceso", ejecutable.toLowerCase());
        }
        return evento;
    }
}
