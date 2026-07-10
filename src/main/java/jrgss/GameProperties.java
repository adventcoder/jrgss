package jrgss;

import java.awt.event.KeyEvent;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class GameProperties {
    public static final String LAUNCH_IN_FULLSCREEN_KEY = "LaunchInFullscreen";
    public static final String PLAY_MUSIC_KEY = "PlayMusic";
    public static final String PLAY_SOUND_KEY = "PlaySound";
    public static final String BUTTON_ASSIGN_KEY = "ButtonAssign";

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
        Preferences node = Preferences.userRoot().node("jrgss");
        launchInFullscreen = node.getBoolean(LAUNCH_IN_FULLSCREEN_KEY, launchInFullscreen);
        playMusic = node.getBoolean(PLAY_MUSIC_KEY, playMusic);
        playSound = node.getBoolean(PLAY_SOUND_KEY, playSound);
        buttonAssign = node.getByteArray(BUTTON_ASSIGN_KEY, buttonAssign);
    }

    public void store() throws BackingStoreException {
        Preferences node = Preferences.userRoot().node("jrgss");
        node.putBoolean(LAUNCH_IN_FULLSCREEN_KEY, launchInFullscreen);
        node.putBoolean(PLAY_MUSIC_KEY, playMusic);
        node.putBoolean(PLAY_SOUND_KEY, playSound);
        node.putByteArray(BUTTON_ASSIGN_KEY, buttonAssign);
        node.flush();
    }

    public void apply() {
        RubyInput.assignButtons(buttonAssign, keyCodes);
    }

    public static void main(String[] args) throws BackingStoreException {
        GameProperties props = new GameProperties();
        props.store();
    }
}
