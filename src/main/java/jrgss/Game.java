package jrgss;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.TrayIcon.MessageType;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JOptionPane;

import org.jruby.RubyException;
import org.jruby.embed.EvalFailedException;
import org.jruby.embed.ScriptingContainer;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.backtrace.RubyStackTraceElement;

public class Game {
    public static Frame frame;
    public static Canvas screen;

    private static boolean fpsShowing = false;
    private static int fps;

    public static AtomicBoolean resetting = new AtomicBoolean(false);

    private static ReentrantLock lock = new ReentrantLock();
    private static Condition activated = lock.newCondition();
    private static boolean active = false;

    public static void main(String[] args) throws Throwable {
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
        screen.setFocusable(false);
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

            @Override
            public void windowActivated(WindowEvent e) {
                lock.lock();
                active = true;
                activated.signalAll();
                lock.unlock();
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
                lock.lock();
                active = false;
                lock.unlock();
            }
        });
        frame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_F1 -> {
                        JOptionPane.showMessageDialog(frame, "hi");
                    }
                    case KeyEvent.VK_F2 -> {
                        toggleFps();
                    }
                    case KeyEvent.VK_F12 -> {
                        resetting.set(true);
                    }
                }
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

    public static void waitIfNotActive() throws InterruptedException {
        if (!active) {
            lock.lock();
            if (!active)
                activated.await();
            lock.unlock();
        }
    }

    private static void toggleFps() {
        if (fpsShowing) {
            frame.setTitle(frame.getTitle().substring(0, frame.getTitle().lastIndexOf(" -")));
            fpsShowing = false;
        } else {
            frame.setTitle(frame.getTitle() + " - " + fps + " FPS");
            fpsShowing = true;
        }
    }

    public static void updateFps(int fps) {
        EventQueue.invokeLater(() -> {
            Game.fps = fps;
            if (fpsShowing)
                frame.setTitle(frame.getTitle().substring(0, frame.getTitle().lastIndexOf(" -")) + " - " + fps + " FPS");
        });
    }

    public static void runScripts(ScriptingContainer container) throws Throwable {
        int index = 0;
        String file = String.format("{%04d}", index);
        String title = "Main";
        try (InputStream in = new FileInputStream("Scripts/" + file + " " + title + ".rb")) {
            container.runScriptlet(in, file);
        } catch (EvalFailedException e) {
            if (e.getCause() instanceof RaiseException re) {
                RubyException exc = re.getException();

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

                JOptionPane.showMessageDialog(frame, message, frame.getTitle(), JOptionPane.WARNING_MESSAGE);
                System.exit((index << 16) | line);
            }
            throw e.getCause();
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
