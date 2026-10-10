// ShellTool.java


package mercury;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.regex.Pattern;

public final class ShellTool implements Tool {
    public interface Approver { boolean approve(String command); }

    private static final Pattern RISKY = Pattern.compile(
        "rm\\s+-[a-z]*r|\\bsudo\\b|mkfs|dd\\s+if=|\\|\\s*(ba)?sh|chmod\\s+-R|>\\s*/dev/|\\bdel\\s+/|format\\s+[a-z]:",
        Pattern.CASE_INSENSITIVE);
    private final Approver approver;

    public ShellTool(Approver approver) { this.approver = approver; }

    @Override public ToolSpec spec() {
        return new ToolSpec("shell", "Führt einen Shell-Befehl aus und liefert die Ausgabe.",
            "{\"type\":\"object\",\"properties\":{\"command\":{\"type\":\"string\"}},\"required\":[\"command\"]}");
    }

    @Override public String execute(JsonNode args) throws Exception {
        String cmd = args.path("command").asText();
        if (cmd.isBlank()) return "Fehler: leerer Befehl.";
        // Riskante Befehle nur nach menschlicher Freigabe (Sicherheitsnetz, keine Sandbox)
        if (RISKY.matcher(cmd).find() && !approver.approve(cmd)) return "Abgelehnt vom Benutzer.";
        boolean win = System.getProperty("os.name").toLowerCase().contains("win");
        Process p = new ProcessBuilder(win ? new String[]{"cmd", "/c", cmd} : new String[]{"sh", "-c", cmd})
            .redirectErrorStream(true).start();
        // Ausgabe parallel lesen, sonst blockiert die Pipe bei viel Output
        CompletableFuture<String> out = CompletableFuture.supplyAsync(() -> {
            try { return new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8); }
            catch (IOException e) { return ""; }
        });
        if (!p.waitFor(30, TimeUnit.SECONDS)) { p.destroyForcibly(); return "Fehler: Zeitlimit (30 s)."; }
        return out.get(5, TimeUnit.SECONDS) + "\n[Exit " + p.exitValue() + "]";
    }
}
