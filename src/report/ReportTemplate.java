package report;

import java.util.ArrayList;
import java.util.List;

/**
 * Prototype: plantilla de reporte clonable con configuracion por defecto.
 */
public abstract class ReportTemplate implements Cloneable {

    protected String title;
    protected ReportFactory factory;
    protected boolean includeRiskAnalysis;
    protected List<String> filters;
    protected List<String> metrics;

    protected ReportTemplate(String title, ReportFactory factory, boolean includeRiskAnalysis,
                             List<String> filters, List<String> metrics) {
        this.title = title;
        this.factory = factory;
        this.includeRiskAnalysis = includeRiskAnalysis;
        this.filters = filters;
        this.metrics = metrics;
    }

    /**
     * Copia profunda: clona la plantilla y duplica las listas mutables.
     */
    @Override
    public ReportTemplate clone() {
        try {
            ReportTemplate copy = (ReportTemplate) super.clone();
            copy.filters = new ArrayList<>(filters);
            copy.metrics = new ArrayList<>(metrics);
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("The report template could not be cloned", e);
        }
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public ReportFactory getFactory() {
        return factory;
    }

    public void setFactory(ReportFactory factory) {
        this.factory = factory;
    }

    public boolean isIncludeRiskAnalysis() {
        return includeRiskAnalysis;
    }

    public void setIncludeRiskAnalysis(boolean includeRiskAnalysis) {
        this.includeRiskAnalysis = includeRiskAnalysis;
    }

    public List<String> getFilters() {
        return filters;
    }

    public List<String> getMetrics() {
        return metrics;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{title='" + title + "', factory="
                + factory.getFormatName() + ", includeRiskAnalysis=" + includeRiskAnalysis
                + ", filters=" + filters + ", metrics=" + metrics + "}";
    }
}
