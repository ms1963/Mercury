// LoopSmokeTest.java (ohne Netzwerk, mit Fake-LLM)

package mercury;
import java.nio.file.*;
import java.util.*;

// Läuft mit aktivierten Assertions, siehe Startbefehl unten
public final class LoopSmokeTest {
    public static void main(String[] a) throws Exception {
        Path tmp = Files.createTempDirectory("mercury-test");
        MemoryTool memory = new MemoryTool(tmp);
        ToolRegistry reg = new ToolRegistry().register(memory);

        // Fake-LLM: erst ein Tool-Aufruf, danach eine Abschlussantwort
        LLMPort fake = new LLMPort() {
            int call = 0;
            public Message chat(List<Message> history, List<ToolSpec> tools) {
                if (call++ == 0) return Message.assistant("", List.of(new ToolCall("c1", "memory",
                    "{\"action\":\"add\",\"content\":\"Nutzer mag knappe Antworten\"}")));
                return Message.assistant("Gespeichert.", List.of());
            }
        };
        String answer = new AgentLoop(fake, reg, "System", 5, System.err::println)
            .run("Merk dir: ich mag knappe Antworten.");
        assert answer.equals("Gespeichert.") : "Antwort falsch: " + answer;
        assert memory.snapshot().contains("knappe Antworten") : "Gedächtnis leer";

        // Limit-Test: Speicher darf nicht über 2200 Zeichen wachsen
        String res = reg.dispatch(new ToolCall("c2", "memory",
            "{\"action\":\"add\",\"content\":\"" + "x".repeat(2300) + "\"}"));
        assert res.startsWith("Fehler: Speicher voll") : "Limit nicht erzwungen: " + res;

        // Unbekanntes Werkzeug muss als Fehlertext zurückkommen, nicht als Exception
        assert reg.dispatch(new ToolCall("c3", "gibt_es_nicht", "{}")).startsWith("Fehler");
        System.out.println("Smoke-Test bestanden.");
    }
}

