// OpenAiCompatibleLLM.java


package mercury;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

// Einziger Ort im Projekt, der HTTP und das OpenAI-Chat-Completions-Format kennt
public final class OpenAiCompatibleLLM implements LLMPort {
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    private final ObjectMapper om = new ObjectMapper();
    private final String baseUrl, apiKey, model;
    public OpenAiCompatibleLLM(String baseUrl, String apiKey, String model) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override public Message chat(List<Message> history, List<ToolSpec> tools) {
        try {
            String body = om.writeValueAsString(buildBody(history, tools));
            HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/chat/completions"))
                .timeout(Duration.ofSeconds(180))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> resp = null;
            for (int attempt = 0; attempt < 3; attempt++) {      // einfacher Retry bei 429 und 5xx
                resp = http.send(req, HttpResponse.BodyHandlers.ofString());
                int sc = resp.statusCode();
                if (sc / 100 == 2 || (sc != 429 && sc / 100 != 5)) break;
                Thread.sleep(1000L * (attempt + 1));
            }
            if (resp.statusCode() / 100 != 2)
                throw new IllegalStateException("LLM-Fehler " + resp.statusCode() + ": " + resp.body());
            return parse(om.readTree(resp.body()).path("choices").path(0).path("message"));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ObjectNode buildBody(List<Message> history, List<ToolSpec> tools) throws Exception {
        ObjectNode body = om.createObjectNode();
        body.put("model", model);
        ArrayNode msgs = body.putArray("messages");
        for (Message m : history) msgs.add(toJson(m));
        if (!tools.isEmpty()) {
            ArrayNode ts = body.putArray("tools");
            for (ToolSpec t : tools) {
                ObjectNode fn = om.createObjectNode();
                fn.put("name", t.name());
                fn.put("description", t.description());
                fn.set("parameters", om.readTree(t.parametersJson()));
                ObjectNode wrap = ts.addObject();
                wrap.put("type", "function");
                wrap.set("function", fn);
            }
        }
        return body;
    }

    private Message parse(JsonNode msg) {
        List<ToolCall> calls = new ArrayList<>();
        int i = 0;
        for (JsonNode c : msg.path("tool_calls")) {
            JsonNode f = c.path("function");
            // Manche Server liefern keine id: dann selbst eine vergeben
            String id = c.path("id").asText("");
            if (id.isBlank()) id = "call_" + System.nanoTime() + "_" + i;
            // Manche Server liefern arguments als Objekt statt als String
            JsonNode args = f.path("arguments");
            String argJson = args.isMissingNode() || args.isNull() ? "{}"
                           : args.isTextual() ? args.asText() : args.toString();
            calls.add(new ToolCall(id, f.path("name").asText(), argJson));
            i++;
        }
        JsonNode content = msg.path("content");
        return Message.assistant(content.isTextual() ? content.asText() : "", calls);
    }

    private ObjectNode toJson(Message m) {
        ObjectNode n = om.createObjectNode();
        n.put("role", m.role());
        n.put("content", m.content());
        if (m.toolCallId() != null) n.put("tool_call_id", m.toolCallId());
        if (!m.toolCalls().isEmpty()) {
            ArrayNode arr = n.putArray("tool_calls");
            for (ToolCall c : m.toolCalls()) {
                ObjectNode o = arr.addObject();
                o.put("id", c.id());
                o.put("type", "function");
                ObjectNode f = o.putObject("function");
                f.put("name", c.name());
                f.put("arguments", c.argumentsJson());
            }
        }
        return n;
    }
}
