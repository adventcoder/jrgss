package jrgss;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = false)
public class RubyTone extends RubyData {
    public static void createToneClass(Ruby runtime) {
        RubyClass cls = runtime.defineClass("Tone", runtime.getObject(), RubyTone::new);
        RubySupport.Tone = cls;

        cls.defineAnnotatedMethods(RubyData.class);
        cls.defineAnnotatedMethods(RubyTone.class);
    }

    public double red;
    public double green;
    public double blue;
    public double gray;

    public RubyTone(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
    }

    public static RubyTone newTone(Ruby runtime) {
        return new RubyTone(runtime, RubySupport.Tone);
    }

    @Override
    public String toString() {
        return String.format("(%f, %f, %f, %f)", red, green, blue, gray);
    }

    @Override
    public void dump(DataOutputStream out) throws IOException {
        out.writeDouble(red);
        out.writeDouble(green);
        out.writeDouble(blue);
        out.writeDouble(gray);
    }

    @Override
    public void load(DataInputStream in) throws IOException {
        this.red = in.readDouble();
        this.green = in.readDouble();
        this.blue = in.readDouble();
        this.gray = in.readDouble();
    }

    @JRubyMethod
    public IRubyObject red() {
        return getRuntime().newFloat(red);
    }

    @JRubyMethod(name = "red=")
    public void set_red(IRubyObject obj) {
        this.red = RubySupport.numToDoubleInRangeClamped(obj, -255.0, 255.0);
    }

    @JRubyMethod
    public IRubyObject green() {
        return getRuntime().newFloat(green);
    }

    @JRubyMethod(name = "green=")
    public void set_green(IRubyObject obj) {
        this.green = RubySupport.numToDoubleInRangeClamped(obj, -255.0, 255.0);
    }

    @JRubyMethod(name = "blue")
    public IRubyObject blue() {
        return getRuntime().newFloat(blue);
    }

    @JRubyMethod(name = "blue=")
    public void set_blue(IRubyObject obj) {
        this.blue = RubySupport.numToDoubleInRangeClamped(obj, -255.0, 255.0);
    }

    @JRubyMethod(name = "gray")
    public IRubyObject gray() {
        return getRuntime().newFloat(gray);
    }

    @JRubyMethod(name = "gray=")
    public void set_gray(IRubyObject obj) {
        this.gray = RubySupport.numToDoubleInRangeClamped(obj, 0.0, 255.0);
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
        this.gray = 0.0;
        return this;
    }

    private IRubyObject set(IRubyObject obj) {
        RubyTone tone = RubySupport.checkType(obj, RubyTone.class);
        this.red = tone.red;
        this.green = tone.green;
        this.blue = tone.blue;
        this.gray = tone.gray;
        return this;
    }

    private IRubyObject set(IRubyObject red, IRubyObject green, IRubyObject blue, IRubyObject gray) {
        set_red(red);
        set_green(green);
        set_blue(blue);
        if (gray == null) {
            this.gray = 0.0;
        } else {
            set_gray(gray);
        }
        return this;
    }
}
