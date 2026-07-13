package jrgss;

import java.awt.Frame;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import lombok.Getter;

public class GameFrame extends Frame {
    public final Game game;

    private @Getter boolean fpsShowing;
    private @Getter int fps;

    private ReentrantLock activeLock = new ReentrantLock();
    private Condition activated = activeLock.newCondition();
    private boolean active = false;

    private ReentrantLock openLock = new ReentrantLock();
    private Condition opened = openLock.newCondition();
    private boolean open = false;

    public GameFrame(Game game) {
        this.game = game;

        if (game.frame != null)
            throw new IllegalStateException("game already framed");
        game.frame = this;

        setResizable(false);
        updateTitle();
        add(game);
        pack();
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                openLock.lock();
                try {
                    open = true;
                    opened.signalAll();
                } finally {
                    openLock.unlock();
                }
            }

            @Override
            public void windowClosing(WindowEvent e) {
                //TODO: we should tell the game thread to stop and let that handle cleanup
                dispose();
            }

            @Override
            public void windowClosed(WindowEvent e) {
                System.exit(0);
            }

            @Override
            public void windowActivated(WindowEvent e) {
                activeLock.lock();
                try {
                    active = true;
                    activated.signalAll();
                } finally {
                    activeLock.unlock();
                }
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
                activeLock.lock();
                try {
                    active = false;
                } finally {
                    activeLock.unlock();
                }
            }
        });
    }

    public void setFps(int fps) {
        int oldFps = this.fps;
        this.fps = fps;
        if (fps != oldFps)
            updateTitle();
    }

    public void setFpsShowing(boolean fpsShowing) {
        boolean oldFpsShowing = this.fpsShowing;
        this.fpsShowing = fpsShowing;
        if (fpsShowing != oldFpsShowing)
            updateTitle();
    }

    private void updateTitle() {
        if (fpsShowing) {
            setTitle(String.format("%s - %d FPS", game.getTitle(), fps));
        } else {
            setTitle(game.getTitle());
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

    public void awaitActive() throws InterruptedException {
        activeLock.lock();
        try {
            while (!active)
                activated.await();
        } finally {
            activeLock.unlock();
        }
    }

    public void awaitOpen() throws InterruptedException {
        openLock.lock();
        try {
            while (!open)
                opened.await();
        } finally {
            openLock.unlock();
        }
    }
}
