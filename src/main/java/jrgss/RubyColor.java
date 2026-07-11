package jrgss;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

public class RubyColor extends RubyObject {
    public static void createColorClass(Ruby runtime) {
        RubyClass cls = runtime.defineClass("Color", runtime.getObject(), RubyColor::new);
        RubySupport.Color = cls;
        cls.defineAnnotatedMethods(RubyColor.class);
    }

    public double red;
    public double green;
    public double blue;
    public double alpha;
    private transient Color color;

    public RubyColor(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
    }

    public static RubyColor newColor(Ruby runtime, double red, double green, double blue, double alpha) {
        RubyColor color = new RubyColor(runtime, RubySupport.Color);
        color.red = red;
        color.green = green;
        color.blue = blue;
        color.alpha = alpha;
        return color;
    }

    public static RubyColor newColor(Ruby runtime, double red, double green, double blue) {
        return newColor(runtime, red, green, blue, 255.0);
    }

    public static RubyColor newColor(Ruby runtime, int argb) {
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return newColor(runtime, r, g, b, a);
    }

    @JRubyMethod(meta = true, required = 3, optional = 1)
    public static IRubyObject hsv(IRubyObject recv, IRubyObject... args) {
        float h = (float) (RubyNumeric.num2dbl(args[0]) / 360.0);
        float s = (float) (clamp(RubyNumeric.num2dbl(args[1])) / 255.0);
        float b = (float) (clamp(RubyNumeric.num2dbl(args[2])) / 255.0);
        RubyColor color = newColor(recv.getRuntime(), Color.HSBtoRGB(h, s, b));
        if (args.length >= 4)
            color.set_alpha(args[3]);
        return color;
    }

    public int getARGB() {
        int r = (int) (red + 0.5);
        int g = (int) (green + 0.5);
        int b = (int) (blue + 0.5);
        int a = (int) (alpha + 0.5);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public Color getColor() {
        if (color == null)
            color = new Color(getARGB(), true);
        return color;
    }

    public @JRubyMethod IRubyObject red() { return getRuntime().newFloat(red); }
    public @JRubyMethod IRubyObject green() { return getRuntime().newFloat(green); }
    public @JRubyMethod IRubyObject blue() { return getRuntime().newFloat(blue); }
    public @JRubyMethod IRubyObject alpha() { return getRuntime().newFloat(alpha); }

    @JRubyMethod(visibility = Visibility.PRIVATE, rest = true)
    public void initialize(IRubyObject... args) {
        if (args.length == 0) {
            set();
        } else if (args.length == 1) {
            set(args[0]);
        } else {
            Arity.checkArgumentCount(getRuntime(), args, 3, 4);
            set(args[0], args[1], args[2], args.length == 3 ? null : args[3]);
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
            return set(args[0], args[1], args[2], args.length == 3 ? null : args[3]);
        }
    }

    public void set() {
        this.red = 0.0;
        this.green = 0.0;
        this.blue = 0.0;
        this.alpha = 0.0;
        this.color = null;
    }

    public IRubyObject set(IRubyObject obj) {
        if (obj == this) return this;
        if (obj instanceof RubyColor color) {
            this.red = color.red;
            this.green = color.green;
            this.blue = color.blue;
            this.alpha = color.alpha;
            this.color = color.color;
        } else {
            throw obj.getRuntime().newTypeError(obj, RubySupport.Color);
        }
        return this;
    }

    public IRubyObject set(IRubyObject red, IRubyObject green, IRubyObject blue, IRubyObject alpha) {
        set_red(red);
        set_green(green);
        set_blue(blue);
        if (alpha == null) {
            this.alpha = 255.0;
        } else {
            set_alpha(alpha);
        }
        return this;
    }

    @JRubyMethod(name = "red=")
    public IRubyObject set_red(IRubyObject obj) {
        this.red = clamp(RubyNumeric.num2dbl(obj));
        this.color = null;
        return obj;
    }

    @JRubyMethod(name = "green=")
    public IRubyObject set_green(IRubyObject obj) {
        this.green = clamp(RubyNumeric.num2dbl(obj));
        this.color = null;
        return obj;
    }

    @JRubyMethod(name = "blue=")
    public IRubyObject set_blue(IRubyObject obj) {
        this.blue = clamp(RubyNumeric.num2dbl(obj));
        this.color = null;
        return obj;
    }

    @JRubyMethod(name = "alpha=")
    public IRubyObject set_alpha(IRubyObject obj) {
        this.alpha = clamp(RubyNumeric.num2dbl(obj));
        this.color = null;
        return obj;
    }

    @JRubyMethod
    @Override
    public IRubyObject to_s() {
        return getRuntime().newString(String.format("(%f, %f, %f, %f)", red, green, blue, alpha));
    }

    @JRubyMethod(name = "==")
    @Override
    public IRubyObject op_equal(ThreadContext context, IRubyObject other) {
        Ruby runtime = context.runtime;
        if (this == other) return runtime.getTrue();
        if (other instanceof RubyColor c)
            return runtime.newBoolean(red == c.red && green == c.green && blue == c.blue && alpha == c.alpha);
        return runtime.getFalse();
    }

    @JRubyMethod(name = "eql?")
    @Override
    public IRubyObject eql_p(IRubyObject other) {
        return op_equal(other.getRuntime().getCurrentContext(), other);
    }

    @JRubyMethod
    @Override
    public RubyFixnum hash() {
        long h = Helpers.hashStart(getRuntime(), Double.doubleToLongBits(red));
        h = Helpers.murmurCombine(h, Double.doubleToLongBits(green));
        h = Helpers.murmurCombine(h, Double.doubleToLongBits(blue));
        h = Helpers.murmurCombine(h, Double.doubleToLongBits(alpha));        
        return getRuntime().newFixnum(Helpers.hashEnd(h));
    }

    @JRubyMethod
    public IRubyObject _dump(IRubyObject arg) {
        ByteBuffer buf = ByteBuffer.allocate(Double.BYTES*4).order(ByteOrder.LITTLE_ENDIAN);
        buf.putDouble(red);
        buf.putDouble(green);
        buf.putDouble(blue);
        buf.putDouble(alpha);
        return RubySupport.newString(getRuntime(), buf, false);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject _load(IRubyObject recv, IRubyObject obj) {
        ByteBuffer buf = RubySupport.getByteBuffer(obj.convertToString());
        RubyColor color = (RubyColor) ((RubyClass) recv).allocate();
        color.red = buf.getDouble();
        color.green = buf.getDouble();
        color.blue = buf.getDouble();
        color.alpha = buf.getDouble();
        return color;
    }

    private static double clamp(double x) {
        return Math.min(Math.max(x, 0.0), 255.0);
    }
}
