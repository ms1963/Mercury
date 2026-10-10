package mercury;
import com.fasterxml.jackson.databind.JsonNode;

public interface Tool {
    ToolSpec spec();   // Name nur aus Buchstaben, Ziffern, _ und -
    String execute(JsonNode args) throws Exception;
}


