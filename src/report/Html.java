package report;

/**
 * Utilidades para escapar texto y producir HTML seguro.
 */
public final class Html {

    private Html() {
    }

    public static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
