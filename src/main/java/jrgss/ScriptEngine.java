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

    public Integer runScripts(String scriptsPath) throws IOException, InterruptedException {
        Integer exitStatus = null;
        while (true) {
            try {
                for (int i = 0; i < 1; i++) {
                    runScript(i);
                }
                break;
            } catch (RaiseException re) {
                game.clear();
                RubyException exc = re.getException();
                if (RubySupport.rgssResetClass.isInstance(exc)) {
                    RubyGraphics.reset();
                    RubyInput.reset();
                } else if (runtime.getSystemExit().isInstance(exc)) {
                    IRubyObject status = ((RubySystemExit) exc).status();
                    if (status != null && !status.isNil())
                        exitStatus = RubyNumeric.fix2int(status);
                    break;
                } else {
                    rubyError(exc);
                }
            }
        }
        return exitStatus;
    }

    private void runScript(int i) throws IOException, InterruptedException {
        String file = String.format("{%04d}", i);
        String title = getScriptTitle(i);
        try (InputStream in = new FileInputStream("Scripts/" + file + " " + title + ".rb")) {
            String script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            runtime.executeScript(script, file);
        }
    }

    private void rubyError(RubyException exc) {
        String file = null;
        int line = 0;
        RubyStackTraceElement[] backtrace = exc.getBacktraceElements();
        for (RubyStackTraceElement el : backtrace) {
            if (el.getFileName().startsWith("{") && el.getFileName().endsWith("}")) {
                file = el.getFileName();
                line = el.getLineNumber();
            }
        }

        String message = exc.getMetaClass().getRealClass().getName() + " occurred.";
        int exitStatus = 0;
        if (file != null) {
            int scriptIndex = Integer.parseInt(file.substring(1, file.length() - 1));
            String prefix = "Script '" + getScriptTitle(scriptIndex) + "' line " + line + ": ";
            message = prefix + message;
            exitStatus = (scriptIndex << 16) | line;
        }

        game.showMessageDialog(message + "\n\n" + exc.getMessageAsJavaString(), JOptionPane.WARNING_MESSAGE);
        System.exit(exitStatus);
    }

    private String getScriptTitle(int i) {
        if (i == 0) return "Main";
        throw new ArrayIndexOutOfBoundsException(i);
    }
}
