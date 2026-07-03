package jrgss;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;

import javax.swing.Timer;

public class GameScreen extends Canvas {
    public final GameWindow window;

    public GameScreen(GameWindow window) {
        this.window = window;
        RubyGraphics.screen = this;

        setFocusable(false);
        setBackground(Color.BLACK);
        setIgnoreRepaint(true);
        setPreferredSize(new Dimension(544, 416));

        initCursor();
    }

    private void initCursor() {
        BufferedImage transparentImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Cursor transparentCursor = Toolkit.getDefaultToolkit().createCustomCursor(transparentImage, new Point(), "transparent");

        Timer timer = new Timer(500, ae -> setCursor(transparentCursor));
        timer.setRepeats(false);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                timer.start();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                timer.stop();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                setCursor(Cursor.getDefaultCursor());
                timer.restart();
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                setCursor(Cursor.getDefaultCursor());
                timer.restart();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                setCursor(Cursor.getDefaultCursor());
                timer.restart();
            }
        });
    }

    public void resize(int width, int height) {
        setPreferredSize(new Dimension(width, height));
        Point center = window.getCenterLocation();
        window.pack();
        window.setCenterLocation(center);
    }

    public void clear() {
        Graphics g = getGraphics();
        g.clearRect(0, 0, getWidth(), getHeight());
        g.dispose();
    }
}
