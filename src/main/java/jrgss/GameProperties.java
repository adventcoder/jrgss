package jrgss;

import java.awt.event.KeyEvent;

//TODO: wip
public class GameProperties {
    public static final int[] buttonKeyCodes = new int[] {
        KeyEvent.VK_SPACE, KeyEvent.VK_ENTER, KeyEvent.VK_ESCAPE, KeyEvent.VK_NUMPAD0, KeyEvent.VK_SHIFT,
        KeyEvent.VK_Z, KeyEvent.VK_X, KeyEvent.VK_C, KeyEvent.VK_V, KeyEvent.VK_B,
        KeyEvent.VK_A, KeyEvent.VK_S, KeyEvent.VK_D,
        KeyEvent.VK_Q, KeyEvent.VK_W
    };

    public boolean launchInFullscreen = false;
    public boolean playMusic = true;
    public boolean playSound = true;
    public byte[] buttonAssign = new byte[] { };

    public void save() {
        //Preferences.userRoot().node("JRGSS");
    }
}
