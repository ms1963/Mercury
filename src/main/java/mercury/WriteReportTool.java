// WriteReportTool.java


package mercury;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.*;

// Schreibt ausschließlich in ein festes Verzeichnis, mit validiertem Dateinamen
public final class WriteReportTool implements Tool {
    private final Path dir;
    public WriteReportTool(Path dir) { this.dir = dir; }

    @Override public ToolSpec spec() {
        return new ToolSpec("write_report", "Speichert einen Markdown-Bericht als <name>.md im Berichtsverzeichnis.",
            "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"},\"content\":{\"type\":\"string\"}},"
          + "\"required\":[\"name\",\"content\"]}");
    }

    @Override public String execute(JsonNode args) throws Exception {
        String name = args.path("name").asText();
        if (!name.matches("[a-zA-Z0-9_-]{1,64}")) return "Fehler: Name nur aus Buchstaben, Ziffern, _ und -.";
        Files.createDirectories(dir);
        Path f = dir.resolve(name + ".md");
        Files.writeString(f, args.path("content").asText());
        return "Bericht gespeichert: " + f.toAbsolutePath();
    }
}
