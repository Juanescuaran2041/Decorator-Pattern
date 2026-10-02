package report;

import java.util.List;
import model.Event;

/**
 * Abstract Factory: familia de formateadores del cuerpo (eventos) de un reporte.
 */
public interface BodyFormatter {

    String formatBody(List<Event> events, ReportMetadata metadata);
}
