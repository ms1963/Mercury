// ToolRegistry.java


package mercury;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

public final class ToolRegistry {
    private static final ObjectMapper OM = new ObjectMapper();
    private static final int MAX_OUT = 8000;
    private final Map<String, Tool> tools = new LinkedHashMap<>();

    public ToolRegistry register(Tool t) { tools.put(t.spec().name(), t); return this; }

    public List<ToolSpec> specs() { return tools.values().stream().map(Tool::spec).toList(); }

    // Eingeschränkte Kopie, z. B. für Subagenten
    public ToolRegistry subset(Set<String> names) {
        ToolRegistry r = new ToolRegistry();
        tools.forEach((n, t) -> { if (names.contains(n)) r.register(t); });
        return r;
    }

    public String dispatch(ToolCall call) {
        Tool t = tools.get(call.name());
        if (t == null) return "Fehler: unbekanntes Werkzeug " + call.name();
        try {
            String json = call.argumentsJson().isBlank() ? "{}" : call.argumentsJson();
            String out = t.execute(OM.readTree(json));
            return out.length() > MAX_OUT ? out.substring(0, MAX_OUT) + "\n[gekürzt]" : out;
        } catch (Exception e) {
            return "Fehler: " + e.getMessage();   // Modell sieht den Fehler und kann reagieren
        }
    }
}
