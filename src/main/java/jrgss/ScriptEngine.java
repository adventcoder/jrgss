package jrgss;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.swing.JOptionPane;

import org.jruby.Ruby;
import org.jruby.RubyException;
import org.jruby.RubyNumeric;
import org.jruby.RubySystemExit;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.GlobalVariable.Scope;
import org.jruby.internal.runtime.ValueAccessor;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.backtrace.RubyStackTraceElement;
import org.jruby.runtime.builtin.IRubyObject;

public class ScriptEngine {
    private final Game game;
    private final Ruby runtime;

    public ScriptEngine(Game game) {
        this.game = game;
        this.runtime = Ruby.newInstance();
        RubySupport.game = game;
        RubySupport.bootstrap(runtime);
    }

    public void setGlobalVariable(String name, Object obj) {
        ValueAccessor var = new ValueAccessor(JavaEmbedUtils.javaToRuby(runtime, obj));
        runtime.getGlobalVariables().define(name, var, Scope.GLOBAL);
    }

    public Integer runScripts() throws IOException, InterruptedException {
        while (true) {
            for (int i = 0; i < 1; i++) {
                try {
                    runScript(i);
                } catch (RaiseException re) {
                    game.clear();
                    RubyException exc = re.getException();
                    if (RubySupport.rgssResetClass.isInstance(exc)) {
                        Thread.sleep(1000L);
                        RubyGraphics.reset();
                        RubyInput.reset();
                    } else if (runtime.getSystemExit().isInstance(exc)) {
                        IRubyObject status = ((RubySystemExit) exc).status();
                        if (status != null && !status.isNil())
                            return RubyNumeric.fix2int(status);
                        return null;
                    } else {
                        rubyError(exc, i);
                    }
                } catch (RGSSStop e) {
                    return null;
                }
            }
        }
    }

    private void runScript(int i) throws IOException {
        String file = String.format("{%04d}", i);
        try (InputStream in = new FileInputStream("Scripts/" + file + " Main.rb")) {
            String script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            runtime.executeScript(script, file);
        }
    }

    private void rubyError(RubyException exc, int scriptIndex) {
        String file = String.format("{%04d}", scriptIndex);
        int line = getErrorLine(exc, file);

        String message = String.format("Script '%s' line %d: %s occurred.", "Main", line, exc.getMetaClass().getRealClass().getName());
        message += "\n\n" + exc.getMessageAsJavaString();

        game.messageBox(message, JOptionPane.WARNING_MESSAGE);
        System.exit((scriptIndex << 16) | line);
    }

    private int getErrorLine(RubyException exc, String file) {
        RubyStackTraceElement[] backtrace = exc.getBacktraceElements();
        for (RubyStackTraceElement el : backtrace) {
            if (el.getFileName().equals(file)) {
                return el.getLineNumber();
            }
        }
        return 0;
    }
}
