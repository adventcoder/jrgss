package jrgss;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import javax.swing.Timer;

public class GameScreen extends Canvas {
    private static Cursor transparentCursor;
    private Timer hideCursorTimer;

    public GameScreen() {
        setBackground(Color.BLACK);
        setFocusable(false);
        setIgnoreRepaint(true);
        setPreferredSize(new Dimension(544, 416));

        hideCursorTimer = new Timer(500, ae -> hideCursor());
        hideCursorTimer.setRepeats(false);
    }

    public void processMouseEvent(MouseEvent e) {
        super.processMouseEvent(e);
        System.out.println("mOUSE EVENT!");
        switch (e.getID()) {
            case MouseEvent.MOUSE_ENTERED -> {
                hideCursorTimer.start();
            }
            case MouseEvent.MOUSE_EXITED -> {
                hideCursorTimer.stop();
            }
            case MouseEvent.MOUSE_PRESSED, MouseEvent.MOUSE_RELEASED -> {
                showCursor();
                hideCursorTimer.restart();
            }
        }
    }

    public void processMouseMotionEvent(MouseEvent e) {
        super.processMouseMotionEvent(e);
        switch (e.getID()) {
            case MouseEvent.MOUSE_MOVED, MouseEvent.MOUSE_DRAGGED -> {
                showCursor();
                hideCursorTimer.restart();
            }
        }
    }

    private void showCursor() {
        setCursor(Cursor.getDefaultCursor());
    }

    private void hideCursor() {
        setCursor(getTransparentCursor());
    }

    private static Cursor getTransparentCursor() {
        if (transparentCursor == null) {
            BufferedImage transparentImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            transparentCursor = Toolkit.getDefaultToolkit().createCustomCursor(transparentImage, new Point(0, 0), "transparent");
        }
        return transparentCursor;
    }
}
