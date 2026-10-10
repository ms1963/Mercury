// SearxngSearch.java


package mercury;
import com.fasterxml.jackson.databind.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

// Adapter für eine SearXNG-Instanz. In deren settings.yml muss das Format "json" freigeschaltet sein.
public final class SearxngSearch implements SearchPort {
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper om = new ObjectMapper();
    private final String base;

    public SearxngSearch(String baseUrl) {
        this.base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @Override public List<Hit> search(String query, int max) throws Exception {
        URI uri = URI.create(base + "/search?format=json&q=" + URLEncoder.encode(query, StandardCharsets.UTF_8));
        HttpResponse<String> r = http.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(20)).build(),
                                           HttpResponse.BodyHandlers.ofString());
        if (r.statusCode() / 100 != 2) throw new IllegalStateException("Suche: HTTP " + r.statusCode());
        List<Hit> hits = new ArrayList<>();
        for (JsonNode n : om.readTree(r.body()).path("results")) {
            if (hits.size() >= max) break;
            hits.add(new Hit(n.path("title").asText(), n.path("url").asText(), n.path("content").asText()));
        }
        return hits;
    }
}


