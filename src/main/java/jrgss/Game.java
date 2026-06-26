package jrgss;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.jruby.embed.ScriptingContainer;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.backtrace.RubyStackTraceElement;

public class Game {

    public static void main(String[] args) throws IOException {
        setupFonts();

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

    public static void setupFonts() {
        File fontsDir = new File("Fonts");

        String[] fileNames = fontsDir.list();
        if (fileNames == null) return;

        for (String fileName : fileNames) {
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex <= 0) continue;

            String suffix = fileName.substring(dotIndex + 1);
            if (suffix.equalsIgnoreCase("ttf") || suffix.equalsIgnoreCase("otf")) {
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
