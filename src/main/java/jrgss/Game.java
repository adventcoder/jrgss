package jrgss;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

import org.jruby.RubyException;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.backtrace.RubyStackTraceElement;

public class Game {
    private static Frame frame;

    public static void main(String[] args) throws Throwable {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        RGSS.init(args);

        frame = RubyGraphics.createFrame("Untitled");

        setupFonts();

        while (true) {
            try {
                for (int i = 0; i < 1; i++) {
                    runScript(i);
                }
            } catch (RaiseException re) {
                RubyGraphics.clear();
                RubyException exc = re.getException();
                if (RGSS.resetClass.isInstance(exc)) {
                    RGSS.reset();
                    continue;
                } else {
                    rubyError(exc);
                }
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private static void runScript(int index) throws IOException, InterruptedException {
        String file = String.format("{%04d}", index);
        try (InputStream in = new FileInputStream("Scripts/" + file + " Main.rb")) {
            String script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            RGSS.runtime.executeScript(script, file);
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

        JOptionPane.showMessageDialog(frame, message, frame.getTitle(), JOptionPane.WARNING_MESSAGE);
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
                ge.registerFont(font);
            }
        }
    }
}
