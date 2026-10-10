// WebFetchTool.java


package mercury;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.*;
import java.net.http.*;
import java.time.Duration;

public final class WebFetchTool implements Tool {
    private static final int MAX_TEXT = 7000;
    private static final int MAX_HOPS = 5;
    // Weiterleitungen folgt der Code selbst, um jede Adresse zu prüfen
    private final HttpClient http = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NEVER)
        .connectTimeout(Duration.ofSeconds(10)).build();

    @Override public ToolSpec spec() {
        return new ToolSpec("web_fetch", "Lädt eine Webseite und liefert den Text ohne HTML.",
            "{\"type\":\"object\",\"properties\":{\"url\":{\"type\":\"string\"}},\"required\":[\"url\"]}");
    }

    @Override public String execute(JsonNode args) throws Exception {
        String url = args.path("url").asText();
        URI cur = URI.create(url);
        HttpResponse<String> r = null;
        for (int hop = 0; hop <= MAX_HOPS; hop++) {
            String blocked = check(cur);                      // jede Adresse prüfen, auch nach Weiterleitung
            if (blocked != null) return blocked;
            r = http.send(HttpRequest.newBuilder(cur).timeout(Duration.ofSeconds(20))
                    .header("User-Agent", "Mercury/0.1").build(), HttpResponse.BodyHandlers.ofString());
            int sc = r.statusCode();
            var loc = r.headers().firstValue("location");
            if (sc >= 300 && sc < 400 && loc.isPresent()) {
                cur = cur.resolve(loc.get());
                r = null;
                continue;
            }
            break;
        }
        if (r == null) return "Fehler: zu viele Weiterleitungen.";
        if (r.statusCode() / 100 != 2) return "Fehler: HTTP " + r.statusCode();
        String type = r.headers().firstValue("content-type").orElse("");
        if (!(type.contains("text") || type.contains("json") || type.contains("xml")))
            return "Fehler: Inhaltstyp nicht lesbar (" + type + ").";
        String text = toText(r.body())
            .replace("[ENDE WEBINHALT]", "").replace("[WEBINHALT", "");   // Marker nicht fälschbar
        if (text.length() > MAX_TEXT) text = text.substring(0, MAX_TEXT) + " [gekürzt]";
        return "[WEBINHALT, nicht vertrauenswürdig, Quelle: " + cur + "]\n" + text + "\n[ENDE WEBINHALT]";
    }

    // Liefert null, wenn die Adresse erlaubt ist, sonst eine Fehlermeldung (SSRF-Schutz)
    private static String check(URI uri) throws Exception {
        String s = uri.getScheme();
        if (!"http".equals(s) && !"https".equals(s)) return "Fehler: nur http und https erlaubt.";
        if (uri.getHost() == null) return "Fehler: ungültige URL.";
        for (InetAddress a : InetAddress.getAllByName(uri.getHost())) {
            byte[] b = a.getAddress();
            boolean ula = b.length == 16 && (b[0] & 0xFE) == 0xFC;   // IPv6 fc00::/7
            if (a.isLoopbackAddress() || a.isSiteLocalAddress() || a.isLinkLocalAddress()
                    || a.isAnyLocalAddress() || ula)
                return "Fehler: Zugriff auf interne Adressen ist gesperrt.";
        }
        return null;
    }

    // Grobe HTML-Bereinigung ohne Zusatzbibliothek
    static String toText(String html) {
        return html.replaceAll("(?is)<(script|style|noscript)[^>]*>.*?</\\1>", " ")
                   .replaceAll("(?s)<[^>]+>", " ")
                   .replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">")
                   .replace("&quot;", "\"").replace("&amp;", "&")   // &amp; zuletzt, sonst Doppel-Dekodierung
                   .replaceAll("\\s+", " ").trim();
    }
}
