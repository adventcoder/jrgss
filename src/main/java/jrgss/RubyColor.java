package jrgss;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

public class RubyColor extends RubyObject {
    public static void createColorClass(Ruby runtime) {
        RubyClass cls = runtime.defineClass("Color", runtime.getObject(), RubyColor::new);
        RGSS.colorClass = cls;
        cls.defineAnnotatedMethods(RubyColor.class);
    }

    public final double[] rgba = new double[4];

    public RubyColor(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
    }

    public RubyColor(Ruby runtime) {
        this(runtime, RGSS.colorClass);
    }

    public void set(int i, double val) {
        rgba[i] = Math.min(Math.max(val, 0.0), 255.0);
    }

    @JRubyMethod
    public IRubyObject red() {
        return getRuntime().newFloat(rgba[0]);
    }

    @JRubyMethod
    public IRubyObject green() {
        return getRuntime().newFloat(rgba[1]);
    }

    @JRubyMethod
    public IRubyObject blue() {
        return getRuntime().newFloat(rgba[2]);
    }

    @JRubyMethod
    public IRubyObject alpha() {
        return getRuntime().newFloat(rgba[3]);
    }

    @JRubyMethod(name = "red=")
    public IRubyObject red_set(IRubyObject obj) {
        set(0, RubyNumeric.num2dbl(obj));
        return obj;
    }

    @JRubyMethod(name = "green=")
    public IRubyObject green_set(IRubyObject obj) {
        set(1, RubyNumeric.num2dbl(obj));
        return obj;
    }

    @JRubyMethod(name = "blue=")
    public IRubyObject blue_set(IRubyObject obj) {
        set(2, RubyNumeric.num2dbl(obj));
        return obj;
    }

    @JRubyMethod(name = "alpha=")
    public IRubyObject alpha_set(IRubyObject obj) {
        set(3, RubyNumeric.num2dbl(obj));
        return obj;
    }

    @JRubyMethod(visibility = Visibility.PRIVATE, rest = true)
    public void initialize(IRubyObject... args) {
        if (args.length == 0) {
            Arrays.fill(rgba, 0.0);
        } else {
            Arity.checkArgumentCount(getRuntime(), args, 3, 4);
            red_set(args[0]);
            green_set(args[1]);
            blue_set(args[2]);
            if (args.length >= 4) {
                alpha_set(args[3]);
            } else {
                rgba[3] = 255.0;
            }
        }
    }

    @JRubyMethod(visibility = Visibility.PRIVATE)
    @Override
    public IRubyObject initialize_copy(IRubyObject orig) {
        return set(orig);
    }

    @JRubyMethod(rest = true)
    public IRubyObject set(IRubyObject... args) {
        if (args.length == 1) {
            return set(args[0]);
        } else {
            Arity.checkArgumentCount(getRuntime(), args, 3, 4);
            red_set(args[0]);
            green_set(args[1]);
            blue_set(args[2]);
            if (args.length >= 4)
                alpha_set(args[3]);
            return this;
        }
    }

    private IRubyObject set(IRubyObject obj) {
        if (obj == this) return this;
        if (!(obj instanceof RubyColor color))
            throw obj.getRuntime().newTypeError(obj, RGSS.colorClass);
        rgba[0] = color.rgba[0];
        rgba[1] = color.rgba[1];
        rgba[2] = color.rgba[2];
        rgba[3] = color.rgba[3];
        return this;
    }

    @JRubyMethod
    @Override
    public IRubyObject to_s() {
        return getRuntime().newString(String.format("(%f, %f, %f, %f)", rgba[0], rgba[1], rgba[2], rgba[3]));
    }

    public Color toAwtColor() {
        // this could be cached, but we'd need to invalidate on mutate
        int r = (int) (rgba[0] + 0.5);
        int g = (int) (rgba[1] + 0.5);
        int b = (int) (rgba[2] + 0.5);
        int a = (int) (rgba[3] + 0.5);
        return new Color(r, g, b, a);
    }

    @JRubyMethod(name = "==", required = 1)
    @Override
    public IRubyObject op_equal(ThreadContext context, IRubyObject obj) {
        if (this == obj) return context.tru;
        if (!(obj instanceof RubyColor color))
            return context.fals;
        return Arrays.equals(rgba, color.rgba) ? context.tru : context.fals;
    }

    @JRubyMethod
    public IRubyObject _dump(IRubyObject depth) {
        ByteBuffer buf = ByteBuffer.allocate(Double.BYTES*rgba.length).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < rgba.length; i++)
            buf.putDouble(rgba[i]);
        return getRuntime().newString(new ByteList(buf.array(), false));
    }

    @JRubyMethod(meta = true)
    public static IRubyObject _load(IRubyObject recv, IRubyObject obj) {
        RubyString data = obj.convertToString();
        ByteBuffer buf = ByteBuffer.wrap(data.getBytes()).order(ByteOrder.LITTLE_ENDIAN);
        RubyColor color = (RubyColor) ((RubyClass) recv).allocate();
        for (int i = 0; i < color.rgba.length && buf.remaining() >= Double.BYTES; i++)
            color.rgba[i] = buf.getDouble();
        return color;
    }
}
