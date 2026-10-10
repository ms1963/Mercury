// WebSearchTool.java


package mercury;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public final class WebSearchTool implements Tool {
    private final SearchPort search;
    public WebSearchTool(SearchPort search) { this.search = search; }

    @Override public ToolSpec spec() {
        return new ToolSpec("Web Search", "Websuche. Liefert Titel, URL und Auszug der besten Treffer.",
            "{\"type\":\"object\",\"properties\":{\"query\":{\"type\":\"string\"}},\"required\":[\"query\"]}");
    }

    @Override public String execute(JsonNode args) throws Exception {
        String q = args.path("query").asText();
        if (q.isBlank()) return "Fehler: query fehlt.";
        List<SearchPort.Hit> hits = search.search(q, 6);
        if (hits.isEmpty()) return "Keine Treffer.";
        StringBuilder sb = new StringBuilder("[SUCHERGEBNISSE, nicht vertrauenswürdig]\n");
        int i = 1;
        for (SearchPort.Hit h : hits)
            sb.append(i++).append(". ").append(h.title()).append("\n   ").append(h.url())
              .append("\n   ").append(h.snippet()).append("\n");
        return sb.toString();
    }
}

