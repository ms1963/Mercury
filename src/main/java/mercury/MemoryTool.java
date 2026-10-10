// MemoryTool.java


package mercury;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public final class MemoryTool implements Tool {
    private static final String SEP = "\n§\n";
    private static final int LIMIT = 2200;
    private final Path file;

    public MemoryTool(Path home) throws IOException {
        Files.createDirectories(home);
        this.file = home.resolve("MEMORY.md");
    }

    // Einmal pro Sitzung lesen und im Systemprompt einfrieren
    public String snapshot() throws IOException {
        return Files.exists(file) ? Files.readString(file) : "";
    }

    private List<String> load() throws IOException {
        String s = snapshot();
        return s.isBlank() ? new ArrayList<>() : new ArrayList<>(Arrays.asList(s.split(SEP, -1)));
    }

    private String save(List<String> entries) throws IOException {
        String joined = String.join(SEP, entries);
        if (joined.length() > LIMIT)
            return "Fehler: Speicher voll (" + joined.length() + "/" + LIMIT
                 + "). Einträge kürzen oder entfernen und erneut versuchen.";
        Files.writeString(file, joined);
        return "OK (" + joined.length() + "/" + LIMIT + " Zeichen belegt)";
    }

    @Override public ToolSpec spec() {
        return new ToolSpec("memory",
            "Verwaltet dauerhafte Notizen (max. 2200 Zeichen). Aktionen: add, replace, remove. "
          + "replace und remove adressieren per old_text (eindeutiger Teilstring); replace ersetzt den ganzen Eintrag.",
            "{\"type\":\"object\",\"properties\":{\"action\":{\"type\":\"string\",\"enum\":[\"add\",\"replace\",\"remove\"]},"
          + "\"content\":{\"type\":\"string\"},\"old_text\":{\"type\":\"string\"}},\"required\":[\"action\"]}");
    }

    @Override public synchronized String execute(JsonNode a) throws Exception {
        List<String> entries = load();
        String action = a.path("action").asText();
        // Das Trennzeichen darf nicht im Eintrag vorkommen, sonst zerfällt die Struktur
        String content = a.path("content").asText("").replace("§", "").trim();
        String old = a.path("old_text").asText("");

        if (action.equals("add")) {
            if (content.isBlank()) return "Fehler: content fehlt.";
            entries.add(content);
            return save(entries);
        }
        if (action.equals("replace") || action.equals("remove")) {
            if (old.isBlank()) return "Fehler: old_text fehlt.";
            List<Integer> hits = new ArrayList<>();
            for (int i = 0; i < entries.size(); i++) if (entries.get(i).contains(old)) hits.add(i);
            if (hits.size() != 1) return "Fehler: old_text trifft " + hits.size() + " Einträge, genau 1 nötig.";
            if (action.equals("replace")) {
                if (content.isBlank()) return "Fehler: content fehlt.";
                entries.set(hits.get(0), content);
            } else {
                entries.remove((int) hits.get(0));
            }
            return save(entries);
        }
        return "Fehler: unbekannte Aktion " + action;
    }
}
