package jrgss;

import java.awt.Graphics2D;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.builtin.IRubyObject;

public abstract class RubyGraphic extends RubyObject {
    protected IRubyObject viewport;
    protected int z;
    protected boolean visible;

    public RubyGraphic(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
        this.viewport = runtime.getNil();
    }

    public @JRubyMethod IRubyObject viewport() { return viewport; }
    public @JRubyMethod IRubyObject z() { return getRuntime().newFixnum(z); }
    public @JRubyMethod IRubyObject visible() { return getRuntime().newBoolean(visible); }

    public void initialize(IRubyObject viewport) {
        set_viewport(viewport);
        z = 0;
        visible = false;
    }

    @JRubyMethod(name = "viewport=")
    public IRubyObject set_viewport(IRubyObject obj) {
        if (obj == viewport) return obj;

        if (viewport.isNil()) {
            // Graphics.remove(this);
        } else {
            // viewport.remove(this);
        }

        if (obj.isNil()) {
            // Graphics.add(this);
        } else {
            viewport = obj;
            // viewport.add(this);
        }

        return obj;
    }

    @JRubyMethod(name = "z=")
    public IRubyObject set_z(IRubyObject obj) {
        this.z = RubyNumeric.fix2int(obj);
        return obj;
    }

    @JRubyMethod(name = "visible=")
    public IRubyObject set_visible(IRubyObject obj) {
        this.visible = obj.isTrue();
        return obj;
    }

    public abstract void render(Graphics2D g);
}
