package jrgss;

import java.awt.Color;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

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

    public double red;
    public double green;
    public double blue;
    public double alpha;
    private Color color;

    public RubyColor(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
    }

    public RubyColor(Ruby runtime) {
        super(runtime, RGSS.colorClass);
    }

    public static RubyColor newColor(Ruby runtime, double red, double green, double blue, double alpha) {
        RubyColor color = new RubyColor(runtime);
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

    @JRubyMethod
    public IRubyObject red() {
        return getRuntime().newFloat(red);
    }

    @JRubyMethod
    public IRubyObject green() {
        return getRuntime().newFloat(green);
    }

    @JRubyMethod
    public IRubyObject blue() {
        return getRuntime().newFloat(blue);
    }

    @JRubyMethod
    public IRubyObject alpha() {
        return getRuntime().newFloat(alpha);
    }

    @JRubyMethod(visibility = Visibility.PRIVATE, rest = true)
    public void initialize(IRubyObject... args) {
        if (args.length == 0) {
            this.red = 0.0;
            this.green = 0.0;
            this.blue = 0.0;
            this.alpha = 0.0;
            this.color = null;
        } else {
            Arity.checkArgumentCount(getRuntime(), args, 3, 4);
            set_red(args[0]);
            set_green(args[1]);
            set_blue(args[2]);
            if (args.length >= 4) {
                set_alpha(args[3]);
            } else {
                this.alpha = 255.0;
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
            set_red(args[0]);
            set_green(args[1]);
            set_blue(args[2]);
            if (args.length >= 4)
                set_alpha(args[3]);
            return this;
        }
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
            throw obj.getRuntime().newTypeError(obj, RGSS.colorClass);
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

    @JRubyMethod(name = "==", required = 1)
    @Override
    public IRubyObject op_equal(ThreadContext context, IRubyObject obj) {
        if (this == obj) return context.tru;
        if (!(obj instanceof RubyColor color))
            return context.fals;
        return red == color.red && blue == color.blue && green == color.green && alpha == color.alpha ? context.tru : context.fals;
    }

    @JRubyMethod
    public IRubyObject _dump(IRubyObject depth) {
        ByteBuffer buf = ByteBuffer.allocate(Double.BYTES*4).order(ByteOrder.LITTLE_ENDIAN);
        buf.putDouble(red);
        buf.putDouble(green);
        buf.putDouble(blue);
        buf.putDouble(alpha);
        return getRuntime().newString(new ByteList(buf.array(), false));
    }

    @JRubyMethod(meta = true)
    public static IRubyObject _load(IRubyObject recv, IRubyObject obj) {
        RubyString data = obj.convertToString();
        ByteBuffer buf = ByteBuffer.wrap(data.getBytes()).order(ByteOrder.LITTLE_ENDIAN);
        RubyColor color = (RubyColor) ((RubyClass) recv).allocate();
        try {
            color.red = clamp(buf.getDouble());
            color.green = clamp(buf.getDouble());
            color.blue = clamp(buf.getDouble());
            color.alpha = clamp(buf.getDouble());
        } catch (BufferUnderflowException e) {
            throw recv.getRuntime().newArgumentError("not enough data");
        }
        return color;
    }

    private static double clamp(double x) {
        return Math.min(Math.max(x, 0.0), 255.0);
    }
}
