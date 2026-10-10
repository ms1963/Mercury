// SearchPort.java
package mercury;
import java.util.List;

// Abstraktion für Websuche: Der Dienst dahinter ist austauschbar
public interface SearchPort {
    record Hit(String title, String url, String snippet) {}
    List<Hit> search(String query, int max) throws Exception;
}
