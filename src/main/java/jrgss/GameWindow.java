package jrgss;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.Timer;

public class GameWindow extends Frame {
    public final Canvas screen;

    private boolean fpsShowing;
    private String originalTitle;
    private int fps;

    private Timer fpsTimer;
    private long lastFrameCount;
    private long lastFrameTime;

    private Set<Integer> keyboardState = ConcurrentHashMap.newKeySet();

    public GameWindow(String title) {
        setOriginalTitle(title);
        setResizable(false);

        screen = new Canvas();
        screen.setFocusable(false);
        screen.setIgnoreRepaint(true);
        screen.setBackground(Color.BLACK);
        screen.setPreferredSize(new Dimension(544, 416));
        add(screen);
        pack();
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                Game.setActive(true);
                if (fpsShowing) {
                    lastFrameCount = RubyGraphics.frameCount;
                    lastFrameTime = System.nanoTime();
                    fpsTimer.start();
                }
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
                Game.setActive(false);
                if (fpsShowing) fpsTimer.stop();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                Game.stop();
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_F1 -> {
                        Dialog dialog = new GamePropertiesDialog(GameWindow.this);
                        dialog.setVisible(true);
                    }
                    case KeyEvent.VK_F2 -> {
                        toggleFpsShowing();
                    }
                    case KeyEvent.VK_F12 -> {
                        Game.reset();
                    }
                }
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                keyboardState.add(e.getKeyCode());
            }

            @Override
            public void keyReleased(KeyEvent e) {
                keyboardState.remove(e.getKeyCode());
            }
        });

        addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowLostFocus(WindowEvent e) {
                keyboardState.clear();
            }
        });

        fpsTimer = new Timer(500, ae -> {
            long currentFrameCount = RubyGraphics.frameCount;
            long currentFrameTime = System.nanoTime();

            long frames = currentFrameCount - lastFrameCount;
            double seconds = (currentFrameTime - lastFrameTime) * 1e-9;
            setFps((int) Math.round(frames / seconds));

            lastFrameCount = currentFrameCount;
            lastFrameTime = currentFrameTime;
        });

        BufferedImage transparentImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Cursor transparentCursor = Toolkit.getDefaultToolkit().createCustomCursor(transparentImage, new Point(0, 0), "transparent");

        Timer hideCursorTimer = new Timer(500, ae -> screen.setCursor(transparentCursor));
        hideCursorTimer.setRepeats(false);

        MouseAdapter mouseAdapter = new MouseAdapter() {
            public @Override void mouseEntered(MouseEvent e) { mouseActivated(e); }
            public @Override void mousePressed(MouseEvent e) { mouseActivated(e); }
            public @Override void mouseReleased(MouseEvent e) { mouseActivated(e); }
            public @Override void mouseMoved(MouseEvent e) { mouseActivated(e); }
            public @Override void mouseDragged(MouseEvent e) { mouseActivated(e); }

            private void mouseActivated(MouseEvent e) {
                screen.setCursor(null);
                hideCursorTimer.restart();
            }
        };
        screen.addMouseListener(mouseAdapter);
        screen.addMouseMotionListener(mouseAdapter);

        // 

        setVisible(true);
        screen.createBufferStrategy(2);
    }

    public String getOriginalTitle() {
        return originalTitle;
    }

    public void setOriginalTitle(String originalTitle) {
        this.originalTitle = originalTitle;
        updateTitle();
    }

    public void setFps(int fps) {
        this.fps = fps;
        updateTitle();
    }

    public void toggleFpsShowing() {
        this.fpsShowing = !fpsShowing;
        updateTitle();
        if (fpsShowing) {
            lastFrameCount = RubyGraphics.frameCount;
            lastFrameTime = System.nanoTime();
            fpsTimer.start();
        } else {
            fpsTimer.stop();
        }
    }

    private void updateTitle() {
        setTitle(fpsShowing ? String.format("%s - %d FPS", originalTitle, fps) : originalTitle);
    }

    public Set<Integer> getKeyboardState() {
        return new HashSet<>(keyboardState);
    }

    public void clearScreen() {
        Graphics g = screen.getGraphics();
        screen.update(g);
        g.dispose();
    }

    public void resizeScreen(int width, int height) {
        screen.setPreferredSize(new Dimension(width, height));
        Point center = getCenterLocation();
        pack();
        setCenterLocation(center);
    }

    private Point getCenterLocation() {
        Point pos = getLocation();
        return new Point(pos.x + getWidth() / 2, pos.y + getHeight() / 2);
    }

    private void setCenterLocation(Point center) {
        setLocation(center.x - getWidth() / 2, center.y - getHeight() / 2);
    }
}
