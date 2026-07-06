package jrgss;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

import javax.swing.Timer;

public class GameWindow extends Frame {
    public final Canvas screen;
    public final KeyboardState keyboardState;

    public GameWindow(String title) {
        super(title);
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
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
                Game.setActive(false);
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
                        //toggleFpsShowing();
                    }
                    case KeyEvent.VK_F12 -> {
                        Game.reset();
                    }
                }
            }
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

        keyboardState = new KeyboardState(this);

        setVisible(true);
        screen.createBufferStrategy(2);
    }

    public void repack() {
        Point pos = getLocation();
        Point center = new Point(pos.x + getWidth() / 2, pos.y + getHeight() / 2);
        pack();
        setLocation(center.x - getWidth() / 2, center.y - getHeight() / 2);
    }
}
