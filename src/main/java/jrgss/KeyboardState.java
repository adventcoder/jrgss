package jrgss;

import java.awt.Component;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

public class KeyboardState {
    protected final IntSet pressed = new IntOpenHashSet();

    private KeyboardState() {
    }

    private KeyboardState(KeyboardState other) {
        pressed.addAll(other.pressed);
    }

    public boolean isPressed(int keyCode) {
        return pressed.contains(keyCode);
    }

    public static class Async extends KeyboardState implements FocusListener, KeyListener {
        public Async(Component component) {
            component.addFocusListener(this);
            component.addKeyListener(this);
        }

        @Override
        public synchronized boolean isPressed(int keyCode) {
            return super.isPressed(keyCode);
        }

        public synchronized KeyboardState snapshot() {
            return new KeyboardState(this);
        }

        @Override
        public void keyTyped(KeyEvent e) {
        }

        @Override
        public synchronized void keyPressed(KeyEvent e) {
            pressed.add(e.getKeyCode());
        }

        @Override
        public synchronized void keyReleased(KeyEvent e) {
            pressed.remove(e.getKeyCode());
        }

        @Override
        public void focusGained(FocusEvent e) {
        }

        @Override
        public synchronized void focusLost(FocusEvent e) {
            pressed.clear();
        }
    }
}
