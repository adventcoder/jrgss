package jrgss;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jruby.embed.ScriptingContainer;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.backtrace.RubyStackTraceElement;

public class Game {

    public static void main(String[] args) throws Exception {
        AtomicBoolean running = new AtomicBoolean(true);
        AtomicBoolean active = new AtomicBoolean(false);

        Canvas canvas = new Canvas();
        canvas.setBackground(Color.BLACK);
        canvas.setIgnoreRepaint(true);
        canvas.setPreferredSize(new Dimension(544, 416));

        Frame frame = new Frame();
        frame.setTitle("Untitled");
        frame.add(canvas);
        frame.pack();
        frame.setLocationByPlatform(true);
        frame.setResizable(false);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                running.set(false);
            }
        });
        frame.addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                System.out.println("Window Gained Focus");
                active.set(true);
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
                System.out.println("Window Lost Focus");
                active.set(false);
            }
        });
        frame.setVisible(true);

        canvas.createBufferStrategy(2);
        BufferStrategy bs = canvas.getBufferStrategy();

        int frameCount = 0;
        while (running.get()) {
            if (active.get()) {
                Graphics g = bs.getDrawGraphics();
                try {
                    render(g, frameCount);
                } finally {
                    g.dispose();
                }
                bs.show();
                frameCount++;
            }
            Thread.sleep(16);
        }

        System.exit(0);
    }

    private static void render(Graphics g, int frameCount) {
        g.setColor(Color.getHSBColor(frameCount % 360 / 360.0f, 1.0f, 1.0f));
        g.fillOval(16, 16, 544 - 32, 416 - 32);
    }

    public static void runScripts() throws IOException {

        // try (InputStream in = new FileInputStream("Scripts/test.rb")) {
        //     container.runScriptlet(in, "{0000}");
        // } catch (RaiseException e) {
        //     RubyStackTraceElement elm = e.getException().getBacktraceElements()[0];
        //     int scriptIndex = Integer.parseInt(elm.getFileName().substring(1, elm.getFileName().length() - 1));
        //     String scriptName = List.of("Test Script").get(scriptIndex);
        //     System.err.println(String.format("Script '%s' line %d: %s occurred.", scriptName, elm.getLineNumber(), e.getException().getClass().getName()));
        //     System.err.println();
        //     System.err.println(e.getMessage());
        // }
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
