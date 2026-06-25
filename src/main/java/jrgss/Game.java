package jrgss;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.jruby.embed.ScriptingContainer;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.backtrace.RubyStackTraceElement;

public class Game {

    public static void main(String[] args) throws IOException {
        ScriptingContainer container = new ScriptingContainer();
        RGSS.bootstrap(container.getProvider().getRuntime());

        try (InputStream in = new FileInputStream("Scripts/test.rb")) {
            container.runScriptlet(in, "{0000}");
        } catch (RaiseException e) {
            RubyStackTraceElement elm = e.getException().getBacktraceElements()[0];
            int scriptIndex = Integer.parseInt(elm.getFileName().substring(1, elm.getFileName().length() - 1));
            String scriptName = List.of("Test Script").get(scriptIndex);
            System.err.println(String.format("Script '%s' line %d: %s occurred.", scriptName, elm.getLineNumber(), e.getException().getClass().getName()));
            System.err.println();
            System.err.println(e.getMessage());
        }
    }
}
