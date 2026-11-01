import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Java agent used by run_matrix.py. Gradle's runClient/runServer is started once with
 * -javaagent:capture.jar=out.json (see build.gradle, autotest_agent). Before the game's main class
 * runs, this records how Loom launched the JVM and halts, so instances can then be started directly.
 * Compiled by run_matrix.py; JDK only.
 */
public final class CaptureAgent {
    public static void premain(String out, Instrumentation inst) throws IOException {
        StringBuilder b = new StringBuilder("{\n");
        field(b, "javaHome", System.getProperty("java.home"));
        field(b, "classpath", System.getProperty("java.class.path"));
        field(b, "command", System.getProperty("sun.java.command"));
        field(b, "cwd", System.getProperty("user.dir"));
        b.append("  \"jvmArgs\": [");
        List<String> args = ManagementFactory.getRuntimeMXBean().getInputArguments();
        String sep = "";
        for (String a : args) {
            if (a.startsWith("-javaagent:") && a.endsWith("=" + out)) continue; // only this agent
            b.append(sep).append(quote(a));
            sep = ", ";
        }
        b.append("],\n  \"env\": {");
        sep = "";
        for (Map.Entry<String, String> e : System.getenv().entrySet()) {
            b.append(sep).append(quote(e.getKey())).append(": ").append(quote(e.getValue()));
            sep = ", ";
        }
        b.append("}\n}\n");
        Files.writeString(Path.of(out), b, StandardCharsets.UTF_8);
        Runtime.getRuntime().halt(0);
    }

    private static void field(StringBuilder b, String k, String v) {
        b.append("  ").append(quote(k)).append(": ").append(quote(v)).append(",\n");
    }

    private static String quote(String s) {
        if (s == null) return "null";
        StringBuilder q = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            if (c == '"' || c == '\\') q.append('\\').append(c);
            else if (c < 0x20) q.append(String.format("\\u%04x", (int) c));
            else q.append(c);
        }
        return q.append('"').toString();
    }
}
