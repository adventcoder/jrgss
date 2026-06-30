package jrgss;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.builtin.IRubyObject;

public class RubyGraphics {
    public static void createGraphicsModule(Ruby runtime) {
        RubyModule mod = runtime.defineModule("Graphics");
        RGSS.graphicsModule = mod;
        mod.defineAnnotatedMethods(RubyGraphics.class);
    }

    @JRubyMethod(meta = true)
    public static void update(IRubyObject recv) {

    }
}
