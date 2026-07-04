package jrgss;

import java.awt.Window;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jruby.Ruby;
import org.jruby.RubyFixnum;
import org.jruby.RubyModule;
import org.jruby.RubySymbol;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.builtin.IRubyObject;

public class RubyInput {
    public static final int DOWN = 2, LEFT = 4, RIGHT = 6, UP = 8;
    public static final int A = 11, B = 12, C = 13, X = 14, Y = 15, Z = 16, L = 16, R = 18;
    public static final int SHIFT = 21, CTRL = 22, ALT = 23;
    public static final int F5 = 25, F6 = 26, F7 = 27, F8 = 28, F9 = 29;

    private static final int repeatDelay = 24;
    private static final int repeatRate = 6;

    private static Set<Integer> keyDown = ConcurrentHashMap.newKeySet();

    private final String name;
    private int[] keyCodes;
    private int pressCount;

    public RubyInput(String name, int[] keyCodes) {
        this.name = name;
        this.keyCodes = keyCodes;
    }

    public void resetInput() {
        pressCount = 0;
    }

    public void updateInput() {
        pressCount = asyncPressed() ? pressCount + 1 : 0;
    }

    private boolean asyncPressed() {
        for (int keyCode : keyCodes)
            if (keyDown.contains(keyCode))
                return true;
        return false;
    }

    public boolean isPressed() {
        return pressCount > 0;
    }

    public boolean isTriggered() {
        return pressCount == 1;
    }

    public boolean isRepeated() {
        return pressCount == 1 || (pressCount > repeatDelay && (pressCount - repeatDelay) % repeatRate == 0);
    }

    private static final Map<Integer, RubyInput> inputs = new HashMap<>();

    static {
        inputs.put(DOWN, new RubyInput("DOWN", new int[] { KeyEvent.VK_DOWN, KeyEvent.VK_NUMPAD2 }));
        inputs.put(LEFT, new RubyInput("LEFT", new int[] { KeyEvent.VK_LEFT, KeyEvent.VK_NUMPAD4 }));
        inputs.put(RIGHT, new RubyInput("RIGHT", new int[] { KeyEvent.VK_RIGHT, KeyEvent.VK_NUMPAD6 }));
        inputs.put(UP, new RubyInput("UP", new int[] { KeyEvent.VK_UP, KeyEvent.VK_NUMPAD8 }));

        inputs.put(A, new RubyInput("A", new int[0]));
        inputs.put(B, new RubyInput("B", new int[0]));
        inputs.put(C, new RubyInput("C", new int[0]));
        inputs.put(X, new RubyInput("X", new int[0]));
        inputs.put(Y, new RubyInput("Y", new int[0]));
        inputs.put(Z, new RubyInput("Z", new int[0]));
        inputs.put(L, new RubyInput("L", new int[0]));
        inputs.put(R, new RubyInput("R", new int[0]));

        inputs.put(SHIFT, new RubyInput("SHIFT", new int[] { KeyEvent.VK_SHIFT }));
        inputs.put(CTRL, new RubyInput("CTRL", new int[] { KeyEvent.VK_CONTROL }));
        inputs.put(UP, new RubyInput("ALT", new int[] { KeyEvent.VK_ALT }));

        inputs.put(F5, new RubyInput("F5", new int[] { KeyEvent.VK_F5 }));
        inputs.put(F6, new RubyInput("F6", new int[] { KeyEvent.VK_F6 }));
        inputs.put(F7, new RubyInput("F7", new int[] { KeyEvent.VK_F7 }));
        inputs.put(F8, new RubyInput("F8", new int[] { KeyEvent.VK_F8 }));
        inputs.put(F9, new RubyInput("F9", new int[] { KeyEvent.VK_F9 }));
    }

    private static final Map<String, RubyInput> inputsByName = new HashMap<>();

    static {
        for (RubyInput input : inputs.values())
            inputsByName.put(input.name, input);
    }

    private static final int[] buttonKeyCodes = new int[] {
        KeyEvent.VK_SPACE, KeyEvent.VK_ENTER, KeyEvent.VK_ESCAPE, KeyEvent.VK_NUMPAD0, KeyEvent.VK_SHIFT,
        KeyEvent.VK_Z, KeyEvent.VK_X, KeyEvent.VK_C, KeyEvent.VK_V, KeyEvent.VK_B,
        KeyEvent.VK_A, KeyEvent.VK_S, KeyEvent.VK_D,
        KeyEvent.VK_Q, KeyEvent.VK_W
    };

    public static void createInputModule(Ruby runtime) {
        RubyModule mod = runtime.defineModule("Input");
        RGSS.inputModule = mod;
        mod.defineAnnotatedMethods(RubyInput.class);

        for (RubyInput input : inputs.values())
            mod.setConstant(input.name, runtime.newSymbol(input.name));
    }

    public static void init(Window wnd) {
        wnd.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                keyDown.add(event.getKeyCode());
            }

            @Override
            public void keyReleased(KeyEvent event) {
                keyDown.remove(event.getKeyCode());
            }
        });
        wnd.addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowLostFocus(WindowEvent e) {
                keyDown.clear();
            }
        });
        assignButtons(new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, C, C, B, B, A, C, B, 0, 0, 0, X, Y, Z, L, R });
        reset();
    }

    public static void reset() {
        inputs.values().forEach(RubyInput::resetInput);
    }

    public static void assignButtons(byte[] buttonMap) {
        Map<Integer, List<Integer>> map = new HashMap<>();
        for (int i = 0; i < Math.min(buttonMap.length - 10, buttonKeyCodes.length); i++) {
            int input = Byte.toUnsignedInt(buttonMap[i + 10]);
            if (input >= A && input <= R)
                map.computeIfAbsent(input, k -> new ArrayList<>()).add(buttonKeyCodes[i]);
        }
        for (Map.Entry<Integer, List<Integer>> entry : map.entrySet())
            inputs.get(entry.getKey()).keyCodes = entry.getValue().stream().mapToInt(Integer::intValue).toArray();
    }

    @JRubyMethod(meta = true)
    public static void update(IRubyObject recv) {
        inputs.values().forEach(RubyInput::updateInput);
    }

    @JRubyMethod(meta = true, name = "press?")
    public static IRubyObject press_p(IRubyObject recv, IRubyObject obj) {
        RubyInput inp = getInput(obj);
        return recv.getRuntime().newBoolean(inp != null && inp.isPressed());
    }

    @JRubyMethod(meta = true, name = "trigger?")
    public static IRubyObject trigger_p(IRubyObject recv, IRubyObject obj) {
        RubyInput inp = getInput(obj);
        return recv.getRuntime().newBoolean(inp != null && inp.isRepeated());
    }

    @JRubyMethod(meta = true, name = "repeat?")
    public static IRubyObject repeat_p(IRubyObject recv, IRubyObject obj) {
        RubyInput inp = getInput(obj);
        return recv.getRuntime().newBoolean(inp != null && inp.isRepeated());
    }

    @JRubyMethod(meta = true, name = "dir8")
    public static IRubyObject dir8(IRubyObject recv) {
        int dx = (inputs.get(RIGHT).isPressed() ? 1 : 0) - (inputs.get(LEFT).isPressed() ? 1 : 0);
        int dy = (inputs.get(UP).isPressed() ? 1 : 0) - (inputs.get(DOWN).isPressed() ? 1 : 0);
        return recv.getRuntime().newFixnum(5 + dx - 3*dy);
    }

    @JRubyMethod(meta = true, name = "dir4")
    public static IRubyObject dir4(IRubyObject recv) {
        int d8 = ((RubyFixnum) dir8(recv)).getIntValue();
        return recv.getRuntime().newFixnum(switch (d8) {
            case 1 -> tieBreak(DOWN, LEFT);
            case 3 -> tieBreak(DOWN, RIGHT);
            case 7 -> tieBreak(UP, LEFT);
            case 9 -> tieBreak(UP, RIGHT);
            default -> d8;
        });
    }

    private static int tieBreak(int inp1, int inp2) {
        return inputs.get(inp1).pressCount >= inputs.get(inp2).pressCount ? inp1 : inp2;
    }

    private static RubyInput getInput(IRubyObject obj) {
        if (obj instanceof RubySymbol sym) {
            return inputsByName.get(sym.asJavaString());
        } else if (obj instanceof RubyFixnum n) {
            return inputs.get(n.getIntValue());
        } else {
            return null;
        }
    }
}
