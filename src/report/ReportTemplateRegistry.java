package report;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registro de plantillas: guarda los prototipos y entrega copias clonadas.
 */
public class ReportTemplateRegistry {

    private final Map<String, ReportTemplate> templates = new LinkedHashMap<>();

    public void register(String key, ReportTemplate template) {
        templates.put(key, template);
    }

    /**
     * Devuelve una copia clonada para que quien la use no modifique el prototipo original.
     */
    public ReportTemplate get(String key) {
        ReportTemplate template = templates.get(key);
        if (template == null) {
            throw new IllegalArgumentException("Unknown report template: " + key);
        }
        return template.clone();
    }

    public List<String> keys() {
        return new ArrayList<>(templates.keySet());
    }
}
