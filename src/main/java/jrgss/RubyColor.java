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

    public RubyColor(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
    }

    public RubyColor(Ruby runtime, RubyClass metaClass, double red, double green, double blue, double alpha) {
        this(runtime, RGSS.colorClass);
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
    }

    public static RubyColor newColor(Ruby runtime, double r, double g, double b, double a) {
        return new RubyColor(runtime, RGSS.colorClass, r, g, b, a);
    }

    public static RubyColor newColor(Ruby runtime, double r, double g, double b) {
        return newColor(runtime, r, g, b, 255);
    }

    public static RubyColor newColor(Ruby runtime, int argb) {
        //NOTE: caching?
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return new RubyColor(runtime, RGSS.colorClass, r, g, b, a);
    }

    public int getARGB() {
        //NOTE: this assumes values are in range or components will overflow
        int r = (int) (red + 0.5);
        int g = (int) (green + 0.5);
        int b = (int) (blue + 0.5);
        int a = (int) (alpha + 0.5);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public void setRed(double val) {
        this.red = Math.min(Math.max(val, 0.0), 255.0);
    }

    public void setGreen(double val) {
        this.green = Math.min(Math.max(val, 0.0), 255.0);
    }

    public void setBlue(double val) {
        this.blue = Math.min(Math.max(val, 0.0), 255.0);
    }

    public void setAlpha(double val) {
        this.alpha = Math.min(Math.max(val, 0.0), 255.0);
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

    @JRubyMethod(name = "red=")
    public IRubyObject red_set(IRubyObject obj) {
        setRed(RubyNumeric.num2dbl(obj));
        return obj;
    }

    @JRubyMethod(name = "green=")
    public IRubyObject green_set(IRubyObject obj) {
        setGreen(RubyNumeric.num2dbl(obj));
        return obj;
    }

    @JRubyMethod(name = "blue=")
    public IRubyObject blue_set(IRubyObject obj) {
        setBlue(RubyNumeric.num2dbl(obj));
        return obj;
    }

    @JRubyMethod(name = "alpha=")
    public IRubyObject alpha_set(IRubyObject obj) {
        setAlpha(RubyNumeric.num2dbl(obj));
        return obj;
    }

    @JRubyMethod(visibility = Visibility.PRIVATE, rest = true)
    public void initialize(IRubyObject... args) {
        if (args.length == 0) {
            this.red = 0.0;
            this.green = 0.0;
            this.blue = 0.0;
            this.alpha = 0.0;
        } else {
            Arity.checkArgumentCount(getRuntime(), args, 3, 4);
            red_set(args[0]);
            green_set(args[1]);
            blue_set(args[2]);
            if (args.length >= 4) {
                alpha_set(args[3]);
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
        if (obj instanceof RubyColor color) {
            this.red = color.red;
            this.green = color.green;
            this.blue = color.blue;
            this.alpha = color.alpha;
        } else {
            throw obj.getRuntime().newTypeError(obj, RGSS.colorClass);
        }
        return this;
    }

    @JRubyMethod
    @Override
    public IRubyObject to_s() {
        return getRuntime().newString(String.format("(%f, %f, %f, %f)", red, green, blue, alpha));
    }

    public Color toJavaColor() {
        //NOTE: this could be cached, but we'd need to invalidate on mutate
        return new Color(getARGB(), true);
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
            color.setRed(buf.getDouble());
            color.setGreen(buf.getDouble());
            color.setBlue(buf.getDouble());
            color.setAlpha(buf.getDouble());
        } catch (BufferUnderflowException e) {
            throw recv.getRuntime().newArgumentError("not enough data");
        }
        return color;
    }

    @JRubyMethod(meta = true, required = 3, optional = 1)
    public static IRubyObject hsv(IRubyObject recv, IRubyObject... args) {
        double hue = RubyNumeric.num2dbl(args[0]);
        double saturation = Math.min(Math.max(RubyNumeric.num2dbl(args[1]), 0.0), 255.0);
        double brightness = Math.min(Math.max(RubyNumeric.num2dbl(args[2]), 0.0), 255.0);
        double alpha = args.length >= 4 ? Math.min(Math.max(RubyNumeric.num2dbl(args[3]), 0.0), 255.0) : 255.0;

        int rgb = Color.HSBtoRGB((float) (hue / 360.0), (float) (saturation / 255.0), (float) (brightness / 255.0));
        int a = (int) (alpha + 0.5);

        return RubyColor.newColor(recv.getRuntime(), (a << 24) | (rgb & 0xFFFFFF));
    }
}
