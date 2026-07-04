package jrgss;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class GameWindow extends Frame {
    private boolean fpsShowing;
    private int fps;

    public GameWindow() {
        setTitle("Untitled");
        setResizable(false);

        // add(RubyGraphics.screen);

        pack();
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // RubyGraphics.stop();

                // we don't want to dispose until until the game thread has terminated or there could be errors trying to access disposed resources
                // however we give up if the game thread is taking too long
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {}

                GameWindow.this.dispose();
            }

            @Override
            public void windowClosed(WindowEvent e) {
                System.exit(0);
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
                        // gameThread.interrupt();
                    }
                }
            }
        });

        setVisible(true);
    }

    @Override
    public void processWindowEvent(WindowEvent e) {
            super.processWindowEvent(e);
        switch (e.getID()) {
            case WindowEvent.WINDOW_ACTIVATED:
            case WindowEvent.WINDOW_DEACTIVATED:
        }
    }

    @Override
    public void processKeyEvent(KeyEvent e) {

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
