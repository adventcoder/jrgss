package jrgss;

import java.awt.Color;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyNumeric;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = false)
public class RubyColor extends RubyData {
    public static void createColorClass(Ruby runtime) {
        RubyClass cls = runtime.defineClass("Color", runtime.getObject(), RubyColor::new);
        RubySupport.Color = cls;

        cls.defineAnnotatedMethods(RubyData.class);
        cls.defineAnnotatedMethods(RubyColor.class);
    }

    public double red;
    public double green;
    public double blue;
    public double alpha;

    public RubyColor(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
    }

    public static RubyColor newColor(Ruby runtime) {
        return new RubyColor(runtime, RubySupport.Color);
    }

    public static RubyColor newColor(Ruby runtime, double red, double green, double blue, double alpha) {
        RubyColor color = newColor(runtime);
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

    public int getARGB() {
        int r = (int) (red + 0.5);
        int g = (int) (green + 0.5);
        int b = (int) (blue + 0.5);
        int a = (int) (alpha + 0.5);
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    public Color toAwtColor() {
        return new Color(getARGB(), true);
    }

    @Override
    public String toString() {
        return String.format("(%f, %f, %f, %f)", red, green, blue, alpha);
    }

    @Override
    public void dump(DataOutputStream out) throws IOException {
        out.writeDouble(red);
        out.writeDouble(green);
        out.writeDouble(blue);
        out.writeDouble(alpha);
    }

    @Override
    public void load(DataInputStream in) throws IOException {
        this.red = in.readDouble();
        this.green = in.readDouble();
        this.blue = in.readDouble();
        this.alpha = in.readDouble();
    }

    @JRubyMethod
    public IRubyObject red() {
        return getRuntime().newFloat(red);
    }

    @JRubyMethod(name = "red=")
    public void set_red(IRubyObject obj) {
        this.red = RubySupport.numToDoubleInRangeClamped(obj, 0.0, 255.0);
    }

    @JRubyMethod
    public IRubyObject green() {
        return getRuntime().newFloat(green);
    }

    @JRubyMethod(name = "green=")
    public void set_green(IRubyObject obj) {
        this.green = RubySupport.numToDoubleInRangeClamped(obj, 0.0, 255.0);
    }

    @JRubyMethod(name = "blue")
    public IRubyObject blue() {
        return getRuntime().newFloat(blue);
    }

    @JRubyMethod(name = "blue=")
    public void set_blue(IRubyObject obj) {
        this.blue = RubySupport.numToDoubleInRangeClamped(obj, 0.0, 255.0);
    }

    @JRubyMethod(name = "alpha")
    public IRubyObject alpha() {
        return getRuntime().newFloat(alpha);
    }

    @JRubyMethod(name = "alpha=")
    public void set_alpha(IRubyObject obj) {
        this.alpha = RubySupport.numToDoubleInRangeClamped(obj, 0.0, 255.0);
    }

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

    private IRubyObject set() {
        this.red = 0.0;
        this.green = 0.0;
        this.blue = 0.0;
        this.alpha = 0.0;
        return this;
    }

    private IRubyObject set(IRubyObject obj) {
        RubyColor color = RubySupport.checkType(obj, RubyColor.class);
        this.red = color.red;
        this.green = color.green;
        this.blue = color.blue;
        this.alpha = color.alpha;
        return this;
    }

    private IRubyObject set(IRubyObject red, IRubyObject green, IRubyObject blue, IRubyObject alpha) {
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

    @JRubyMethod(meta = true, required = 3, optional = 1)
    public static IRubyObject hsv(IRubyObject recv, IRubyObject... args) {
        float h = (float) (RubyNumeric.num2dbl(args[0]) / 360.0);
        float s = (float) (RubySupport.numToDoubleInRangeClamped(args[1], 0.0, 255.0) / 255.0);
        float b = (float) (RubySupport.numToDoubleInRangeClamped(args[2], 0.0, 255.0) / 255.0);
        int rgb = Color.HSBtoRGB(h, s, b);
        RubyColor color = newColor(recv.getRuntime(), rgb);
        if (args.length >= 4)
            color.set_alpha(args[3]);
        return color;
    }

    @JRubyMethod
    public IRubyObject hsv() {
        int r = (int) (red + 0.5);
        int g = (int) (green + 0.5);
        int b = (int) (blue + 0.5);
        float[] hsb = Color.RGBtoHSB(r & 0xFF, g & 0xFF, b & 0xFF, null);
        IRubyObject hue = getRuntime().newFloat(hsb[0] * 360.0);
        IRubyObject sat = getRuntime().newFloat(hsb[1] * 255.0);
        IRubyObject val = getRuntime().newFloat(hsb[2] * 255.0);
        return RubyArray.newArrayMayCopy(getRuntime(), hue, sat, val);
    }
}
