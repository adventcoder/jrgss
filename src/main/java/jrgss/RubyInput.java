package jrgss;

import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.builtin.IRubyObject;

public class RubyInput {
    public static final int KEY_DOWN = 2;
    public static final int KEY_LEFT = 4;
    public static final int KEY_RIGHT = 6;
    public static final int KEY_UP = 8;

    public static void createInputModule(Ruby runtime) {
        RubyModule mod = runtime.defineModule("Input");
        RGSS.inputModule = mod;
        mod.defineAnnotatedMethods(RubyInput.class);
    }

    private static Map<String, Collection<Integer>> buttonMap = Map.of(
        "A", Set.of(KeyEvent.VK_SHIFT),
        "B", Set.of(KeyEvent.VK_ESCAPE, KeyEvent.VK_NUMPAD0, KeyEvent.VK_X),
        "C", Set.of(KeyEvent.VK_SPACE, KeyEvent.VK_ENTER, KeyEvent.VK_Z),
        "X", Set.of(KeyEvent.VK_A),
        "Y", Set.of(KeyEvent.VK_S),
        "Z", Set.of(KeyEvent.VK_D),
        "L", Set.of(KeyEvent.VK_Q),
        "R", Set.of(KeyEvent.VK_W)
    );

    private static Set<Integer> wasPressed = new HashSet<>();
    private static Set<Integer> pressed = new HashSet<>();

    public static void reset() {
        wasPressed.clear();
        pressed.clear();
    }

    @JRubyMethod(meta = true)
    public static void update(IRubyObject recv) {
        wasPressed = pressed;
        // could swap here to avoid copy
        pressed = new HashSet<>();
    }

    @JRubyMethod(meta = true, name = "press?")
    public static IRubyObject press_p(IRubyObject recv, IRubyObject obj) {
        Collection<Integer> keyCodes = buttonMap.get(obj.asJavaString());
        return recv.getRuntime().newBoolean(keyCodes.stream().anyMatch(RubyInput::keyPressed));
    }

    public static boolean keyPressed(int keyCode) {
        return pressed.contains(keyCode);
    }

    @JRubyMethod(meta = true, name = "trigger?")
    public static IRubyObject trigger_p(IRubyObject recv, IRubyObject obj) {
        Collection<Integer> keyCodes = buttonMap.get(obj.asJavaString());
        return recv.getRuntime().newBoolean(keyCodes.stream().anyMatch(RubyInput::keyTriggered));
    }

    public static boolean keyTriggered(int keyCode) {
        return pressed.contains(keyCode) && !wasPressed.contains(keyCode);
    }

    @JRubyMethod(meta = true, name = "release?")
    public static IRubyObject release_p(IRubyObject recv, IRubyObject obj) {
        Collection<Integer> keyCodes = buttonMap.get(obj.asJavaString());
        return recv.getRuntime().newBoolean(keyCodes.stream().anyMatch(RubyInput::keyReleased));
    }

    public static boolean keyReleased(int keyCode) {
        return wasPressed.contains(keyCode) && !pressed.contains(keyCode);
    }
}
