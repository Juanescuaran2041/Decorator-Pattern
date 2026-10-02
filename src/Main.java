/**
 * Cliente de consola: ensambla el pipeline, lo recorre e imprime las alertas.
 *
 * Sin argumentos usa los datos de prueba. Con un argumento lee eventos reales de Windows:
 *   java -cp out Main Security              (requiere consola de administrador)
 *   java -cp out Main C:\ruta\archivo.evtx
 */
public class Main {

    private static final int MAXIMO_EVENTOS_REALES = 5000;

    public static void main(String[] args) {
        FuenteEventos fuenteBase;
        if (args.length > 0) {
            fuenteBase = new FuenteLogWindows(args[0], MAXIMO_EVENTOS_REALES);
        } else {
            fuenteBase = new FuenteEnMemoria(DatosPrueba.generar());
        }

        // De afuera hacia adentro: puntaje -> filtro -> enriquecimiento -> normalización -> fuente.
        FuenteEventos pipeline =
                new ConPuntajeRiesgo(
                        new ConFiltro(
                                new ConEnriquecimiento(
                                        new ConNormalizacion(fuenteBase))));

        int procesados = 0;
        int alertas = 0;
        Evento evento;
        try {
            while ((evento = pipeline.siguiente()) != null) {
                procesados++;
                if (evento.getPuntaje() >= 50) {
                    alertas++;
                    System.out.printf("ALERTA puntaje=%d categoria=%s proceso=%s archivo=%s%n",
                            evento.getPuntaje(), evento.get("categoria"),
                            evento.get("proceso"), evento.get("archivo"));
                }
            }
        } catch (IllegalStateException e) {
            // Por ejemplo, leer el log Security sin permisos de administrador.
            System.out.println("Error: " + e.getMessage());
            return;
        }

        System.out.println("----------------------------------------");
        System.out.println("Eventos procesados: " + procesados);
        System.out.println("Alertas: " + alertas);
    }
}
