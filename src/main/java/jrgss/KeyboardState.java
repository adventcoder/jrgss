package jrgss;

import java.util.HashSet;
import java.util.Set;

public interface KeyboardState {
    public boolean isPressed(int keyCode);

    public class Snapshot implements KeyboardState {
        public Set<Integer> pressed;

        public Snapshot(Set<Integer> pressed) {
            this.pressed = new HashSet<>(pressed);
        }

        @Override
        public boolean isPressed(int keyCode) {
            return pressed.contains(keyCode);
        }
    }
}
