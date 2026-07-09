package jrgss;

import java.awt.event.KeyEvent;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class GameProperties {
    public static final int[] keyCodes = {
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        KeyEvent.VK_SPACE, KeyEvent.VK_ENTER, KeyEvent.VK_ESCAPE, KeyEvent.VK_NUMPAD0, KeyEvent.VK_SHIFT,
        KeyEvent.VK_Z, KeyEvent.VK_X, KeyEvent.VK_C, KeyEvent.VK_V, KeyEvent.VK_B,
        KeyEvent.VK_A, KeyEvent.VK_S, KeyEvent.VK_D,
        KeyEvent.VK_Q, KeyEvent.VK_W
    };

    public static byte[] defaultButtonAssign = {
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        RubyInput.C, RubyInput.C, RubyInput.B, RubyInput.B, RubyInput.A,
        RubyInput.C, RubyInput.B, 0, 0, 0,
        RubyInput.X, RubyInput.Y, RubyInput.Z,
        RubyInput.L, RubyInput.R
    };

    public boolean launchInFullscreen = false;
    public boolean playMusic = true;
    public boolean playSound = true;
    public byte[] buttonAssign = defaultButtonAssign;

    public void load() {
        Preferences node = Preferences.userRoot().node("com/example/jrgss");
        launchInFullscreen = node.getBoolean("launchInFullscreen", launchInFullscreen);
        playMusic = node.getBoolean("playMusic", playMusic);
        playSound = node.getBoolean("playSound", playSound);
        buttonAssign = node.getByteArray("buttonAssign", buttonAssign);
    }

    public void store() throws BackingStoreException {
        Preferences node = Preferences.userRoot().node("com/example/jrgss");
        node.putBoolean("launchInFullscreen", launchInFullscreen);
        node.putBoolean("playMusic", launchInFullscreen);
        node.putBoolean("playsound", launchInFullscreen);
        node.putByteArray("buttonAssign", buttonAssign);
        node.flush();
    }

    public void apply() {
        RubyInput.assignButtons(buttonAssign, keyCodes);
    }
}
