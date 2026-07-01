package jrgss;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.swing.JOptionPane;

import org.jruby.Ruby;
import org.jruby.RubyException;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.ValueAccessor;
import org.jruby.internal.runtime.GlobalVariable.Scope;
import org.jruby.runtime.backtrace.RubyStackTraceElement;

public class Game {
    private static Ruby runtime;
    private static GameWindow window;

    public static void main(String[] args) throws Throwable {
        initRuby();
        setEnv(args);

        window = new GameWindow("Untitled");

        setupFonts();

        gameMain();
    }

    public static void initRuby() {
        runtime = Ruby.newInstance();
        RGSS.bootstrap(runtime);
    }

    public static void setEnv(String[] args) {
        boolean test = false;
        boolean btest = false;
        for (String arg : args) {
            if (arg.equalsIgnoreCase("test")) {
                test = true;
            } else if (arg.equalsIgnoreCase("best")) {
                test = true;
                btest = true;
            }
        }
        runtime.getGlobalVariables().define("$TEST", new ValueAccessor(runtime.newBoolean(test)), Scope.GLOBAL);
        runtime.getGlobalVariables().define("$BTEST", new ValueAccessor(runtime.newBoolean(btest)), Scope.GLOBAL);
    }

    public static void gameMain() throws IOException {
        while (true) {
            RubyGraphics.clear();
            RubyInput.clear();
            try {
                runScript(0, "Main");
            } catch (RaiseException re) {
                rubyError(re.getException(), 0, "Main");
            } catch (StopException e) {
                break;
            }
        }
    }

    private static void runScript(int index, String title) throws IOException {
        String file = String.format("{%04d}", index);
        try (InputStream in = new FileInputStream("Scripts/" + file + " " + title + ".rb")) {
            String script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            runtime.executeScript(script, file);
        }
    }

    private static void rubyError(RubyException exc, int index, String title) {
        String file = String.format("{%04d}", index);
        int line = 0;
        RubyStackTraceElement[] backtrace = exc.getBacktraceElements();
        for (RubyStackTraceElement el : backtrace) {
            if (el.getFileName().equals(file)) {
                line = el.getLineNumber();
                break;
            }
        }

        String message = String.format("Script '%s' line %d: %s occurred.", title, line, exc.getMetaClass().getRealClass().getName());
        message += "\n\n" + exc.getMessageAsJavaString();

        JOptionPane.showMessageDialog(window, message, window.getTitleWithoutFps(), JOptionPane.WARNING_MESSAGE);
        System.exit((index << 16) | line);
    }

    public static void setupFonts() {
        File fontsDir = new File("Fonts");

        String[] fileNames = fontsDir.list();
        if (fileNames == null) return;

        for (String fileName : fileNames) {
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex <= 0) continue;

            String suffix = fileName.substring(dotIndex + 1);
            if (suffix.equalsIgnoreCase("ttf")) {
                File fontFile = new File(fontsDir, fileName);

                Font font = null;
                try {
                    font = Font.createFont(Font.TRUETYPE_FONT, fontFile);
                } catch (IOException | FontFormatException e) {
                    e.printStackTrace();
                    continue;
                }

                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                if (ge.registerFont(font)) {
                    System.out.println("Registered: " + font.getName());
                }
            }
        }
    }
}
