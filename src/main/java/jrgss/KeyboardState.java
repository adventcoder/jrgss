package jrgss;

import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class KeyboardState implements KeyListener, WindowFocusListener {
    private final Set<Integer> pressed = ConcurrentHashMap.newKeySet();

    public KeyboardState(Window window) {
        window.addKeyListener(this);
        window.addWindowFocusListener(this);
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent event) {
        pressed.add(event.getKeyCode());
    }

    @Override
    public void keyReleased(KeyEvent event) {
        pressed.remove(event.getKeyCode());
    }

    @Override
    public void windowGainedFocus(WindowEvent e) {
    }

    @Override
    public void windowLostFocus(WindowEvent e) {
        pressed.clear();
    }

    public boolean isPressed(int keyCode) {
        return pressed.contains(keyCode);
    }
}
