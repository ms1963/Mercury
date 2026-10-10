// Message.java
package mercury;
import java.util.List;

public record Message(String role, String content, List<ToolCall> toolCalls, String toolCallId) {
    // Kompakter Konstruktor: content und toolCalls sind nie null
    public Message {
        content = content == null ? "" : content;
        toolCalls = toolCalls == null ? List.of() : toolCalls;
    }
    public static Message system(String c) { 
         return new Message("system", c, List.of(), null); 
    }
    public static Message user(String c)   { 
         return new Message("user", c, List.of(), null); 
    }
    public static Message assistant(String c, List<ToolCall> calls) { 
         return new Message("assistant", c, calls, null); 
    }
    public static Message tool(String id, String c) { 
         return new Message("tool", c, List.of(), id); 
    }
}

