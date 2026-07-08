package jrgss;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.WindowEvent;

public class GameFrame extends Frame {
    private final Game game;
    private Thread gameThread;
    private Integer gameExitStatus;
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
            case WindowEvent.WINDOW_OPENED -> {
                game.createBufferStrategy(2);
                game.requestFocus();
                gameThread = new Thread(() -> {
                    try {
                        gameExitStatus = game.call();
                    } catch (Exception exc) {
                        exc.printStackTrace();
                    } finally {
                        // the game ended, so we just dispose ourselves gracefully, arigatou gozaimasu
                        EventQueue.invokeLater(() -> dispose());
                    }
                }, "game-main");
                gameThread.setDaemon(true);
                gameThread.start();
            }
            case WindowEvent.WINDOW_CLOSING -> {
                if (game.stop()) {
                    try {
                        gameThread.join(1000);
                    } catch (InterruptedException ignored) {
                    }
                    dispose();
                }
            }
            case WindowEvent.WINDOW_CLOSED -> {
                // we use the exit status for ruby errors. in all other cases we exit with 0.
                if (gameExitStatus != null)
                    System.err.println("Ignoring game exit status of " + gameExitStatus);
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
