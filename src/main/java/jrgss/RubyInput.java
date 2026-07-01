package jrgss;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.builtin.IRubyObject;

public class RubyInput {
    public static void createInputModule(Ruby runtime) {
        RubyModule mod = runtime.defineModule("Input");
        RGSS.inputModule = mod;
        mod.defineAnnotatedMethods(RubyInput.class);
    }

    public static void reset() {

    }

    @JRubyMethod(meta = true)
    public static void update(IRubyObject recv) {

    }
}
