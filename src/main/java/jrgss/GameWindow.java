package jrgss;

import java.awt.Frame;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class GameWindow extends Frame {
    public final Thread gameThread;
    public final GameScreen screen;

    private boolean fpsShowing;
    private int fps;

    public GameWindow(String title) {
        gameThread = Thread.currentThread();

        setTitle(title);
        setResizable(false);

        screen = new GameScreen(this);
        add(screen);

        pack();
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                RubyGraphics.stop();

                gameThread.interrupt();
                try {
                    gameThread.join();
                } catch (InterruptedException ignored) {}

                GameWindow.this.dispose();
            }

            @Override
            public void windowClosed(WindowEvent e) {
                System.exit(0);
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
                RubyGraphics.pause();
            }

            @Override
            public void windowActivated(WindowEvent e) {
                RubyGraphics.unpause();
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_F1 -> {
                        PropertiesDialog dialog = new PropertiesDialog(GameWindow.this);
                        dialog.setVisible(true);
                    }
                    case KeyEvent.VK_F2 -> {
                        toggleFpsShowing();
                    }
                    case KeyEvent.VK_F12 -> {
                        // stop game thread
                        // start new game thread
                    }
                }
            }
        });

        setVisible(true);
    }

    public Point getCenterLocation() {
        Point pos = getLocation();
        return new Point(pos.x + getWidth() / 2, pos.y + getHeight() / 2);
    }

    public void setCenterLocation(Point center) {
        setLocation(center.x - getWidth() / 2, center.y - getHeight() / 2);
    }

    public String getTitleWithoutFps() {
        return fpsShowing ? removeFps(getTitle()) : getTitle();
    }

    public void toggleFpsShowing() {
        if (fpsShowing) {
            setTitle(removeFps(getTitle()));
            fpsShowing = false;
        } else {
            setTitle(addFps(getTitle(), fps));
            fpsShowing = true;
        }
    }

    public void setFps(int fps) {
        int oldFps = fps;
        this.fps = fps;
        if (fpsShowing && fps != oldFps)
            setTitle(addFps(removeFps(getTitle()), fps));
    }

    private static String addFps(String title, int fps) {
        return title + " - " + fps  + " FPS";
    }

    private static String removeFps(String title) {
        int i = title.lastIndexOf(" - ");
        return title.substring(0, i);
    }
}
