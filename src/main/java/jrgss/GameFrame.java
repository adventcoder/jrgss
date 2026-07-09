package jrgss;

import java.awt.AWTEvent;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.WindowEvent;

public class GameFrame extends Frame {
    private final Game game;
    private boolean fpsShowing;
    private int fps;

    public GameFrame(Game game) {
        this.game = game;

        if (game.frame != null)
            throw new IllegalStateException("frame already created");
        game.frame = this;

        setResizable(false);
        updateTitle();
        add(game);
        pack();
        setLocationRelativeTo(null);
        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
    }

    @Override
    public void processWindowEvent(WindowEvent e) {
        super.processWindowEvent(e);
        switch (e.getID()) {
            case WindowEvent.WINDOW_CLOSING -> {
                System.exit(0);
            }
            case WindowEvent.WINDOW_ACTIVATED -> {
                game.setActive(true);
            }
            case WindowEvent.WINDOW_DEACTIVATED -> {
                game.setActive(false);
            }
        }
    }

    public void repack() {
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

    public void setFps(int fps) {
        if (this.fps == fps) return;
        this.fps = fps;
        updateTitle();
    }

    public void toggleFpsShowing() {
        fpsShowing = !fpsShowing;
        updateTitle();
    }

    private void updateTitle() {
        if (fpsShowing) {
            setTitle(String.format("%s - %d FPS", game.title, fps));
        } else {
            setTitle(game.title);
        }
    }
}
