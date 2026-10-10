// SkillTools.java


package mercury;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.file.*;
import java.util.stream.*;

public final class SkillTools {
    private static final int MAX_SKILL = 8000;
    private final Path dir;

    public SkillTools(Path home) throws IOException {
        this.dir = home.resolve("skills");
        Files.createDirectories(dir);
    }

    // Legt einen mitgelieferten Skill an, falls er noch fehlt
    public void seedIfAbsent(String name, String content) throws IOException {
        Path d = dir.resolve(safe(name));
        if (Files.exists(d.resolve("SKILL.md"))) return;
        Files.createDirectories(d);
        Files.writeString(d.resolve("SKILL.md"), content);
    }

    private static String schema(String props, String req) {
        return "{\"type\":\"object\",\"properties\":{" + props + "},\"required\":[" + req + "]}";
    }

    // Nur Name und erste Zeile: progressive Offenlegung
    public Tool list() { return new Tool() {
        public ToolSpec spec() { return new ToolSpec("skill_list", "Listet verfügbare Skills mit Kurzbeschreibung.", schema("", "")); }
        public String execute(JsonNode a) throws IOException {
            try (Stream<Path> s = Files.list(dir)) {
                String r = s.filter(Files::isDirectory).sorted().map(p -> {
                    try {
                        String first = Files.readAllLines(p.resolve("SKILL.md")).stream().findFirst().orElse("");
                        return p.getFileName() + ": " + first;
                    } catch (IOException e) { return p.getFileName() + ": (unlesbar)"; }
                }).collect(Collectors.joining("\n"));
                return r.isEmpty() ? "(keine Skills vorhanden)" : r;
            }
        }
    }; }

    public Tool view() { return new Tool() {
        public ToolSpec spec() { return new ToolSpec("skill_view", "Liest einen Skill vollständig.",
            schema("\"name\":{\"type\":\"string\"}", "\"name\"")); }
        public String execute(JsonNode a) throws IOException {
            Path f = dir.resolve(safe(a.path("name").asText())).resolve("SKILL.md");
            return Files.exists(f) ? Files.readString(f) : "Fehler: Skill nicht gefunden.";
        }
    }; }

    public Tool save() { return new Tool() {
        public ToolSpec spec() { return new ToolSpec("skill_save",
            "Speichert oder überschreibt einen Skill. Die erste Zeile ist die Kurzbeschreibung.",
            schema("\"name\":{\"type\":\"string\"},\"content\":{\"type\":\"string\"}", "\"name\",\"content\"")); }
        public String execute(JsonNode a) throws IOException {
            String c = a.path("content").asText();
            if (c.isBlank()) return "Fehler: content fehlt.";
            if (c.length() > MAX_SKILL) return "Fehler: Skill zu lang (max. " + MAX_SKILL + " Zeichen).";
            Path d = dir.resolve(safe(a.path("name").asText()));
            Files.createDirectories(d);
            Files.writeString(d.resolve("SKILL.md"), c);
            return "Skill gespeichert.";
        }
    }; }

    // Verhindert Pfadtricks wie "../.."
    private static String safe(String n) {
        if (!n.matches("[a-zA-Z0-9_-]{1,64}")) throw new IllegalArgumentException("Ungültiger Skill-Name");
        return n;
    }
}
