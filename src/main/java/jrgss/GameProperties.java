package jrgss;

import java.awt.event.KeyEvent;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class GameProperties {
    public static final String LAUNCH_IN_FULLSCREEN_KEY = "LaunchInFullScreen";
    public static final String PLAY_MUSIC_KEY = "PlayMusic";
    public static final String PLAY_SOUND_KEY = "PlaySound";
    public static final String BUTTON_ASSIGN_KEY = "ButtonAssign";

    public static final boolean defaultLaunchInFullScreen = false;
    public static final boolean defaultPlayMusic = true;
    public static final boolean defaultPlaySound = true;
    public static final byte[] defaultButtonAssign = {
        RubyInput.A, RubyInput.B, RubyInput.C, RubyInput.X, RubyInput.Y, RubyInput.Z, RubyInput.L, RubyInput.R, 0, 0,
        RubyInput.C, RubyInput.C, RubyInput.B, RubyInput.B, RubyInput.A,
        RubyInput.C, RubyInput.B, 0, 0, 0,
        RubyInput.X, RubyInput.Y, RubyInput.Z,
        RubyInput.L, RubyInput.R
    };
    public static final int[] keyCodeAssign = {
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        KeyEvent.VK_SPACE, KeyEvent.VK_ENTER, KeyEvent.VK_ESCAPE, KeyEvent.VK_NUMPAD0, KeyEvent.VK_SHIFT,
        KeyEvent.VK_Z, KeyEvent.VK_X, KeyEvent.VK_C, KeyEvent.VK_V, KeyEvent.VK_B,
        KeyEvent.VK_A, KeyEvent.VK_S, KeyEvent.VK_D,
        KeyEvent.VK_Q, KeyEvent.VK_W
    };

    public boolean launchInFullScreen = defaultLaunchInFullScreen;
    public boolean playMusic = defaultPlayMusic;
    public boolean playSound = defaultPlaySound;
    public byte[] buttonAssign = defaultButtonAssign;

    public void load() {
        Preferences node = Preferences.userRoot().node("jrgss");
        launchInFullScreen = node.getBoolean(LAUNCH_IN_FULLSCREEN_KEY, defaultLaunchInFullScreen);
        playMusic = node.getBoolean(PLAY_MUSIC_KEY, defaultPlayMusic);
        playSound = node.getBoolean(PLAY_SOUND_KEY, defaultPlaySound);
        buttonAssign = node.getByteArray(BUTTON_ASSIGN_KEY, defaultButtonAssign);
    }

    public boolean save() {
        Preferences node = Preferences.userRoot().node("jrgss");
        node.putBoolean(LAUNCH_IN_FULLSCREEN_KEY, launchInFullScreen);
        node.putBoolean(PLAY_MUSIC_KEY, playMusic);
        node.putBoolean(PLAY_SOUND_KEY, playSound);
        node.putByteArray(BUTTON_ASSIGN_KEY, buttonAssign);
        try {
            node.flush();
        } catch (BackingStoreException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void apply(boolean launch) {
        RubyInput.assignButtons(buttonAssign, keyCodeAssign);
    }

    public static void main(String[] args) {
        GameProperties props = new GameProperties();
        props.save();
    }
}
