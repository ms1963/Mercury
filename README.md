# Mercury Agent

Mercury ist ein stark vereinfachtes Agentic AI System, das primär Forschung nach Fachgebieten implementiert, aber auch als allgemeines Agentic AI System fungieren kann. Es ist Teil bzw. Beispiel eines Artikels für JavaSPEKTRUM über Agentic AI. Ich nutze Ollama und für das untere Beispiel qwen3.8 mit 27B

## Projektverzeichnis

```
mercury/
├── pom.xml
└── src/
    └── main/
        └── java/
            └── mercury/
                ├── Main.java
                ├── AgentLoop.java
                ├── LLMPort.java
                ├── Message.java
                ├── ToolCall.java
                ├── ToolSpec.java
                ├── Tool.java
                ├── SearchPort.java
                ├── ToolRegistry.java
                ├── OpenAiCompatibleLLM.java
                ├── ShellTool.java
                ├── MemoryTool.java
                ├── SkillTools.java
                ├── SearxngSearch.java
                ├── WebSearchTool.java
                ├── WebFetchTool.java
                ├── WriteReportTool.java
                ├── DelegateTool.java
                └── LoopSmokeTest.java
```

## Einlesen in Intellij IDEA
Öffnen Sie IntelliJ IDEA und wählen im Startdialog oder im Menü unter „File" den Punkt „Open". Wählen Sie die pom.xml aus, nicht nur den Ordner. IntelliJ fragt dann „Open as Project?" und Sie bestätigen dies. Damit importiert IntelliJ das Projekt als vollwertiges Maven-Projekt, lädt die Jackson-Abhängigkeit automatisch aus dem Maven Central Repository und legt die Modulstruktur an. Beim ersten Mal kann der Download ein bis zwei Minuten dauern, unten rechts sehen Sie den Fortschritt.

Zwei Dinge sollten Sie anschließend prüfen. Erstens das JDK: Unter „File", dann „Project Structure" und dort „Project" muss ein SDK mit Java 21 ausgewählt sein. Falls IntelliJ kein JDK 21 findet, laden Sie  es direkt in diesem Dialog nach, IntelliJ kann das automatisch herunterladen. Zweitens das Maven-Fenster: Über „View", „Tool Windows" und dann „Maven" öffnen Sie die Seitenleiste, in der Sie später die Lebenszyklus-Schritte wie compile direkt anklicken können, statt die Kommandozeile zu bemühen.

Zum Starten erstellen Sie Run-Konfigurationen über „Run", dann „Edit Configurations". Für den Chat legen Sie eine „Application"-Konfiguration mit Hauptklasse mercury.Main an, für den Research Agent eine zweite mit denselben Einstellungen plus Ihrer Forschungsfrage  im Feld „Program arguments". 

Die Umgebungsvariablen 

MERCURY_BASE_URL, 


MERCURY_API_KEY, 


MERCURY_MODEL und 


MERCURY_SEARXNG_URL 

tragen Sie ebenfalls dort ein, im Feld „Environment variables", jeweils durch Semikolon getrennt. Für den Smoke-Test brauchen Sie  eine dritte Konfiguration mit Hauptklasse mercury.LoopSmokeTest und -ea im Feld „VM options", sonst bleiben die assert-Prüfungen verschwiegen.

Ein letzter Hinweis, damit sich niemand später wundert: Wie Berichtsdateien landen unter reports/ im Projektverzeichnis, wenn Sie die Konfigurationen aus IntelliJ starten, weil das Arbeitsverzeichnis dort standardmäßig das Projektverzeichnis ist.

Unten zeigt wie sich über Umgebungsvariablen Informationen einstellen lassen, etwa die URL eines Ollama-Servers oder das gewünschte LLM-Modell.

Der Smoke-Test erfolgt ohne Netzwerk, der Aufruf mit der Researchfrage aktiviert das System als Researchagent. Im Chat-Profil verhält sich das System als Shell.

# Shell-Script


export MERCURY_BASE_URL=http://localhost:11434/v1


export MERCURY_MODEL=<Modellname mit Tool-Calling>


export MERCURY_SEARXNG_URL=http://localhost:8080



**Smoke-Test (ohne Netzwerk)>:** mvn -q compile exec:java -Dexec.mainClass=mercury.LoopSmokeTest -Dexec.classpathScope=compile


**Research Agent:** mvn -q compile exec:java -Dexec.mainClass=mercury.Main -Dexec.args="research Deine Forschungsfrage"


**Chat-Profil:** mvn -q compile exec:java -Dexec.mainClass=mercury.Main
