// AgentLoop.java


package mercury;
import java.util.*;
import java.util.function.Consumer;

public final class AgentLoop {
    private static final int MAX_CHARS = 60_000;   // ab hier alte Tool-Ausgaben entfernen
    private static final int KEEP_RECENT = 8;      // letzte Nachrichten bleiben unangetastet

    private final LLMPort llm;
    private final ToolRegistry registry;
    private final List<Message> history = new ArrayList<>();
    private final int maxSteps;
    private final Consumer<String> trace;

    public AgentLoop(LLMPort llm, ToolRegistry registry, String systemPrompt,
                     int maxSteps, Consumer<String> trace) {
        this.llm = llm; this.registry = registry; this.maxSteps = maxSteps; this.trace = trace;
        history.add(Message.system(systemPrompt));
    }

    public String run(String userInput) {
        history.add(Message.user(userInput));
        for (int step = 1; step <= maxSteps; step++) {
            compact();
            Message reply = llm.chat(history, registry.specs());
            history.add(reply);
            if (reply.toolCalls().isEmpty()) return reply.content();   // erledigt
            for (ToolCall call : reply.toolCalls()) {
                trace.accept("[" + step + "] " + call.name() + " " + shorten(call.argumentsJson()));
                history.add(Message.tool(call.id(), registry.dispatch(call)));
            }
        }
        // Limit erreicht: Zusammenfassung ohne Werkzeuge erzwingen
        history.add(Message.user("Schrittlimit erreicht. Fasse den bisherigen Stand zusammen."));
        return llm.chat(history, List.of()).content();
    }

    // Mikro-Verdichtung: Tool-Ausgaben außerhalb des jüngsten Fensters durch Platzhalter ersetzen.
    // Die Aufruf-IDs bleiben erhalten, die Struktur des Verlaufs bleibt gültig.
    private void compact() {
        int total = history.stream().mapToInt(m -> m.content().length()).sum();
        if (total <= MAX_CHARS) return;
        for (int i = 1; i < history.size() - KEEP_RECENT; i++) {
            Message m = history.get(i);
            if (m.role().equals("tool") && m.content().length() > 200)
                history.set(i, Message.tool(m.toolCallId(), "[Ausgabe aus Platzgründen entfernt]"));
        }
    }

    private static String shorten(String s) { return s.length() > 120 ? s.substring(0, 120) + "..." : s; }
}
