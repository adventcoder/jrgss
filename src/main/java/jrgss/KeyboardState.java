package jrgss;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Set;

public class KeyboardState implements KeyListener {
    public static Set<Integer> pressed;

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        pressed.add(e.getKeyCode());
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }
}
