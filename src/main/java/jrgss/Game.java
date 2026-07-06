package jrgss;

import java.awt.Canvas;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

import org.jruby.Ruby;
import org.jruby.RubyException;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubySystemExit;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.backtrace.RubyStackTraceElement;
import org.jruby.runtime.builtin.IRubyObject;

public class Game {
    public static Logger logger = Logger.getLogger(Game.class.getSimpleName());
    public static Thread thread;
    public static Ruby runtime;
    public static GameWindow window;
    public static Canvas screen;

    public static AtomicBoolean stopping = new AtomicBoolean(false);
    public static AtomicBoolean resetting = new AtomicBoolean(false);

    private static ReentrantLock activeLock = new ReentrantLock();
    private static Condition activated = activeLock.newCondition();
    private static volatile boolean active = false;

    public static boolean stop() {
        return stopping.compareAndSet(false, true);
    }

    public static boolean reset() {
        return resetting.compareAndSet(false, true);
    }

    public static boolean clearStopping() {
        return stopping.compareAndSet(true, false);
    }

    public static boolean clearResetting() {
        return resetting.compareAndSet(true, false);
    }

    public static void setActive(boolean newActive) {
        activeLock.lock();
        try {
            active = newActive;
            if (newActive)
                activated.signalAll();
        } finally {
            activeLock.unlock();
        }
    }

    public static void waitWhileInactive() throws InterruptedException {
        activeLock.lock();
        try {
            while (!active)
                activated.await();
        } finally {
            activeLock.unlock();
        }
    }

    public static void main(String[] args) throws Throwable {
        thread = Thread.currentThread();

        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        RubyInstanceConfig config = new RubyInstanceConfig();
        runtime = Ruby.newInstance(config);

        window = new GameWindow("Untitled");
        screen = window.screen;

        setGlobalVariables(args);
        RubySupport.bootstrap(runtime);

        // setupRTP();
        setupFonts();

        while (true) {
            try {
                runScripts();
            } catch (RaiseException re) {
                RubyGraphics.clearScreen();
                RubyException exc = re.getException();
                if (RubySupport.rgssResetClass.isInstance(exc)) {
                    RubyGraphics.reset();
                    RubyInput.reset();
                    continue;
                } else if (runtime.getSystemExit().isInstance(exc)) {
                    IRubyObject status = ((RubySystemExit) exc).status();
                    logger.log(Level.INFO, "Ruby exitted with status: " + status);
                } else {
                    rubyError(exc);
                }
            } catch (RGSSStop e) {
                RubyGraphics.clearScreen();
                break;
            }
        }

        System.exit(0);
    }

    public static void setGlobalVariables(String[] args) {
        boolean test = false;
        boolean btest = false;
        for (String arg : args) {
            if (arg.equalsIgnoreCase("test")) {
                test = true;
            } else if (arg.equalsIgnoreCase("btest")) {
                test = true;
                btest = true;
            }
        }
        RubySupport.setGlobalVariable(runtime, "$TEST", test);
        RubySupport.setGlobalVariable(runtime, "$BTEST", btest);
    }

    public static void setupFonts() {
        //TODO: integrate with RTP
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
                    logger.log(Level.WARNING, "Error creating font", e);
                    continue;
                }

                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            }
        }
    }

    private static void runScripts() throws IOException {
        try (InputStream in = new FileInputStream("Scripts/{0000} Main.rb")) {
            String script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            runtime.executeScript(script, "{0000}");
        }
    }

    private static void rubyError(RubyException exc) {
        String file = null;
        int line = 0;
        RubyStackTraceElement[] backtrace = exc.getBacktraceElements();
        for (RubyStackTraceElement el : backtrace) {
            if (el.getFileName().startsWith("{") && el.getFileName().endsWith("}")) {
                file = el.getFileName();
                line = el.getLineNumber();
                break;
            }
        }
        int index = Integer.parseInt(file.substring(1, file.length() - 1));

        String message = String.format("Script '%s' line %d: %s occurred.", "Main", line, exc.getMetaClass().getRealClass().getName());
        message += "\n\n" + exc.getMessageAsJavaString();

        JOptionPane.showMessageDialog(window, message, window.getTitle(), JOptionPane.WARNING_MESSAGE);
        System.exit((index << 16) | line);
    }
}
