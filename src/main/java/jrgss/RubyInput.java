package jrgss;

import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Predicate;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyFixnum;
import org.jruby.RubyModule;
import org.jruby.RubySymbol;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.builtin.IRubyObject;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

public class RubyInput {
    public static void createInputModule(Ruby runtime) {
        RubyModule mod = runtime.defineModule("Input");
        RubySupport.inputModule = mod;
        mod.defineAnnotatedMethods(RubyInput.class);

        for (RubyInput input : inputs.values())
            mod.setConstant(input.symbol, runtime.fastNewSymbol(input.symbol));
    }

    public static final int DOWN = 2, LEFT = 4, RIGHT = 6, UP = 8;
    public static final int A = 11, B = 12, C = 13, X = 14, Y = 15, Z = 16, L = 17, R = 18;
    public static final int SHIFT = 21, CTRL = 22, ALT = 23;
    public static final int F5 = 25, F6 = 26, F7 = 27, F8 = 28, F9 = 29;

    private static final int repeatDelay = 24;
    private static final int repeatRate = 6;

    private final String symbol;
    private int[] keyCodes;
    private int pressCount;

    public RubyInput(String symbol, int[] keyCodes) {
        this.symbol = symbol;
        this.keyCodes = keyCodes;
    }

    public void resetInput() {
        pressCount = 0;
    }

    public void updateInput(KeyboardState keyboardState) {
        pressCount = isPressed(keyboardState) ? pressCount + 1 : 0;
    }

    private boolean isPressed(KeyboardState keyboardState) {
        for (int keyCode : keyCodes)
            if (keyboardState.isPressed(keyCode))
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
        inputs.put(DOWN, new RubyInput("DOWN", new int[] { KeyEvent.VK_DOWN, KeyEvent.VK_NUMPAD2, KeyEvent.VK_KP_DOWN }));
        inputs.put(LEFT, new RubyInput("LEFT", new int[] { KeyEvent.VK_LEFT, KeyEvent.VK_NUMPAD4, KeyEvent.VK_KP_LEFT }));
        inputs.put(RIGHT, new RubyInput("RIGHT", new int[] { KeyEvent.VK_RIGHT, KeyEvent.VK_NUMPAD6, KeyEvent.VK_KP_RIGHT }));
        inputs.put(UP, new RubyInput("UP", new int[] { KeyEvent.VK_UP, KeyEvent.VK_NUMPAD8, KeyEvent.VK_KP_UP }));

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
        inputs.put(ALT, new RubyInput("ALT", new int[] { KeyEvent.VK_ALT }));

        inputs.put(F5, new RubyInput("F5", new int[] { KeyEvent.VK_F5 }));
        inputs.put(F6, new RubyInput("F6", new int[] { KeyEvent.VK_F6 }));
        inputs.put(F7, new RubyInput("F7", new int[] { KeyEvent.VK_F7 }));
        inputs.put(F8, new RubyInput("F8", new int[] { KeyEvent.VK_F8 }));
        inputs.put(F9, new RubyInput("F9", new int[] { KeyEvent.VK_F9 }));
    }

    private static final Map<String, RubyInput> inputsByName = new IdentityHashMap<>();

    static {
        for (RubyInput input : inputs.values())
            inputsByName.put(input.symbol, input);
    }

    private static final int[] buttonKeyCodes = new int[] {
        KeyEvent.VK_SPACE, KeyEvent.VK_ENTER, KeyEvent.VK_ESCAPE, KeyEvent.VK_NUMPAD0, KeyEvent.VK_SHIFT,
        KeyEvent.VK_Z, KeyEvent.VK_X, KeyEvent.VK_C, KeyEvent.VK_V, KeyEvent.VK_B,
        KeyEvent.VK_A, KeyEvent.VK_S, KeyEvent.VK_D,
        KeyEvent.VK_Q, KeyEvent.VK_W
    };

    public static void assignButtons(byte[] buttonMap) {
        for (int inp = A; inp <= R; inp++) {
            IntList keyCodes = new IntArrayList();
            int limit = 10 + Math.min(buttonMap.length - 10, buttonKeyCodes.length);
            for (int i = 10; i < limit; i++) {
                if (Byte.toUnsignedInt(buttonMap[i]) == inp)
                    keyCodes.add(buttonKeyCodes[i - 10]);
            }
            if (inp == L) keyCodes.add(KeyEvent.VK_PAGE_UP);
            if (inp == R) keyCodes.add(KeyEvent.VK_PAGE_DOWN);
            inputs.get(inp).keyCodes = keyCodes.toIntArray();
        }
    }

    static {
        assignButtons(new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, C, C, B, B, A, C, B, 0, 0, 0, X, Y, Z, L, R });
    }

    public static void reset() {
        for (RubyInput input : inputs.values())
            input.resetInput();
    }

    @JRubyMethod(meta = true)
    public static void update(IRubyObject recv) {
        Game game = RubySupport.getGame(recv.getRuntime());
        KeyboardState keyboardState = game.getKeyboardState();
        for (RubyInput input : inputs.values())
            input.updateInput(keyboardState);
    }

    @JRubyMethod(meta = true, name = "press?")
    public static IRubyObject press_p(IRubyObject recv, IRubyObject obj) {
        return test(recv, obj, RubyInput::isPressed);
    }

    @JRubyMethod(meta = true, name = "trigger?")
    public static IRubyObject trigger_p(IRubyObject recv, IRubyObject obj) {
        return test(recv, obj, RubyInput::isTriggered);
    }

    @JRubyMethod(meta = true, name = "repeat?")
    public static IRubyObject repeat_p(IRubyObject recv, IRubyObject obj) {
        return test(recv, obj, RubyInput::isRepeated);
    }

    private static IRubyObject test(IRubyObject recv, IRubyObject obj, Predicate<RubyInput> pred) {
        RubyInput inp = getInput(obj);
        return recv.getRuntime().newBoolean(inp != null && pred.test(inp));
    }

    @JRubyMethod(meta = true)
    public static IRubyObject pressed(IRubyObject recv) {
        return list(recv, RubyInput::isPressed);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject triggered(IRubyObject recv) {
        return list(recv, RubyInput::isTriggered);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject repeated(IRubyObject recv) {
        return list(recv, RubyInput::isRepeated);
    }

    private static IRubyObject list(IRubyObject recv, Predicate<RubyInput> pred) {
        RubySymbol[] inps = inputs.values().stream()
            .filter(pred)
            .map(inp -> recv.getRuntime().fastNewSymbol(inp.symbol))
            .toArray(RubySymbol[]::new);
        return RubyArray.newArrayMayCopy(recv.getRuntime(), inps);
    }

    @JRubyMethod(meta = true, name = "dir8")
    public static IRubyObject dir8(IRubyObject recv) {
        int dx = (inputs.get(RIGHT).isPressed() ? 1 : 0) - (inputs.get(LEFT).isPressed() ? 1 : 0);
        int dy = (inputs.get(UP).isPressed() ? 1 : 0) - (inputs.get(DOWN).isPressed() ? 1 : 0);
        return recv.getRuntime().newFixnum(dx == 0 && dy == 0 ? 0 : 5 + 3*dy + dx);
    }

    @JRubyMethod(meta = true, name = "dir4")
    public static IRubyObject dir4(IRubyObject recv) {
        RubyFixnum d8 = (RubyFixnum) dir8(recv);
        return switch (d8.getIntValue()) {
            case 1 -> recv.getRuntime().newFixnum(tieBreak(DOWN, LEFT));
            case 3 -> recv.getRuntime().newFixnum(tieBreak(DOWN, RIGHT));
            case 7 -> recv.getRuntime().newFixnum(tieBreak(UP, LEFT));
            case 9 -> recv.getRuntime().newFixnum(tieBreak(UP, RIGHT));
            default -> d8;
        };
    }

    private static int tieBreak(int inp1, int inp2) {
        return inputs.get(inp1).pressCount <= inputs.get(inp2).pressCount ? inp1 : inp2;
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
