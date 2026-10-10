// LLMPort.java
package mercury;
import java.util.List;

// Einzige Abstraktion für Sprachmodelle. Ein VLMPort für Bilder folgt demselben Muster.
public interface LLMPort {
    Message chat(List<Message> history, List<ToolSpec> tools);
}
