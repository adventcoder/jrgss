package jrgss;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jruby.embed.ScriptingContainer;

public class Game {
    public static Frame frame;
    public static Canvas screen;
    public static Set<Integer> pressed = ConcurrentHashMap.newKeySet();

    public static void main(String[] args) throws Exception {
        ScriptingContainer container = new ScriptingContainer();
        RGSS.bootstrap(container.getProvider().getRuntime());
        initEnv(container, args);

        createScreen();
        createFrame();
        setupFonts();

        RubyGraphics.reset();
        RubyInput.reset();
        runScripts(container);
    }

    public static void initEnv(ScriptingContainer container, String[] args) {
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
        container.put("$TEST", test);
        container.put("$BTEST", btest);
    }

    private static void createScreen() {
        screen = new Canvas();
        screen.setBackground(Color.BLACK);
        screen.setIgnoreRepaint(true);
        screen.setPreferredSize(new Dimension(544, 416));
    }

    private static void createFrame() {
        frame = new Frame();
        frame.setTitle("Untitled");
        frame.setResizable(false);
        frame.add(screen);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        frame.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                pressed.add(e.getKeyCode());
            }

            public void keyReleased(KeyEvent e) {
                pressed.remove(e.getKeyCode());
            }
        });
        frame.addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
                pressed.clear();
            }
        });
        frame.setVisible(true);
    }

    public static void repackFrame() {
        int centerX = frame.getX() + frame.getWidth() / 2;
        int centerY = frame.getY() + frame.getHeight() / 2;
        frame.pack();
        frame.setLocation(centerX - frame.getWidth() / 2, centerY - frame.getHeight() / 2);
    }

    public static BufferStrategy getBufferStrategy() {
        BufferStrategy bs = screen.getBufferStrategy();
        if (bs == null) {
            screen.createBufferStrategy(2);
            bs = screen.getBufferStrategy();
        }
        return bs;
    }

    public static void runScripts(ScriptingContainer container) throws IOException {
        try (InputStream in = new FileInputStream("Scripts/{0000} Main.rb")) {
            container.runScriptlet(in, "{0000}");
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
