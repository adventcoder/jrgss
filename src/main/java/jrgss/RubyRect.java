package jrgss;

import java.awt.Rectangle;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyNumeric;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = false)
public class RubyRect extends RubyData {
    public static void createRectClass(Ruby runtime) {
        RubyClass cls = runtime.defineClass("Rect", runtime.getObject(), RubyRect::new);
        RubySupport.Rect = cls;

        cls.defineAnnotatedMethods(RubyData.class);
        cls.defineAnnotatedMethods(RubyRect.class);
    }

    public int x;
    public int y;
    public int width;
    public int height;

    public RubyRect(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
    }

    public RubyRect(Ruby runtime, RubyClass metaClass, boolean useObjectSpace) {
        super(runtime, metaClass, useObjectSpace);
    }

    public static RubyRect newRect(Ruby runtime) {
        return new RubyRect(runtime, RubySupport.Rect);
    }

    public static RubyRect newRect(Ruby runtime, int x, int y, int width, int height) {
        RubyRect rect = newRect(runtime);
        rect.x = x;
        rect.y = y;
        rect.width = width;
        rect.height = height;
        return rect;
    }

    public static RubyRect newRectLight(Ruby runtime) {
        return new RubyRect(runtime, RubySupport.Rect, false);
    }

    public static RubyRect newRectLight(Ruby runtime, int x, int y, int width, int height) {
        RubyRect rect = newRectLight(runtime, x, y, width, height);
        rect.x = x;
        rect.y = y;
        rect.width = width;
        rect.height = height;
        return rect;
    }

    public static RubyRect newRectLightFromArgs(Ruby runtime, IRubyObject[] args, int i) {
        return newRectLight(runtime, RubyNumeric.num2int(args[i]), RubyNumeric.num2int(args[i+1]), RubyNumeric.num2int(args[i+2]), RubyNumeric.num2int(args[i+3]));
    }

    public Rectangle toAwtRectangle() {
        return new Rectangle(x, y, width, height);
    }

    @Override
    public String toString() {
        return String.format("(%d, %d, %d, %d)", x, y, width, height);
    }

    @Override
    public void dump(DataOutputStream out) throws IOException {
        out.writeInt(x);
        out.writeInt(y);
        out.writeInt(width);
        out.writeInt(height);
    }

    public void load(DataInputStream in) throws IOException {
        x = in.readInt();
        y = in.readInt();
        width = in.readInt();
        height = in.readInt();
    }

    @JRubyMethod
    public IRubyObject x() {
        return getRuntime().newFixnum(x);
    }

    @JRubyMethod(name = "x=")
    public void set_x(IRubyObject obj) {
        this.x = RubyNumeric.num2int(obj);
    }

    @JRubyMethod
    public IRubyObject y() {
        return getRuntime().newFixnum(y);
    }

    @JRubyMethod(name = "y=")
    public void set_y(IRubyObject obj) {
        this.y = RubyNumeric.num2int(obj);
    }

    @JRubyMethod
    public IRubyObject width() {
        return getRuntime().newFixnum(width);
    }

    @JRubyMethod(name = "width=")
    public void set_width(IRubyObject obj) {
        this.width = RubyNumeric.num2int(obj);
    }

    @JRubyMethod
    public IRubyObject height() {
        return getRuntime().newFixnum(height);
    }

    @JRubyMethod(name = "height=")
    public void set_height(IRubyObject obj) {
        this.height = RubyNumeric.num2int(obj);
    }

    @JRubyMethod(visibility = Visibility.PRIVATE, rest = true)
    public void initialize(IRubyObject... args) {
        if (args.length == 0) {
            empty();
        } else {
            Arity.checkArgumentCount(getRuntime(), args, 4, 4);
            set(args[0], args[1], args[2], args[3]);
        }
    }

    @JRubyMethod(visibility = Visibility.PRIVATE)
    @Override
    public IRubyObject initialize_copy(IRubyObject orig) {
        return set(orig);
    }

    @JRubyMethod
    public IRubyObject empty() {
        this.x = 0;
        this.y = 0;
        this.width = 0;
        this.height = 0;
        return this;
    }

    @JRubyMethod(rest = true)
    public IRubyObject set(IRubyObject... args) {
        if (args.length == 1) {
            return set(args[0]);
        } else {
            Arity.checkArgumentCount(getRuntime(), args, 4, 4);
            return set(args[0], args[1], args[2], args[3]);
        }
    }

    private IRubyObject set(IRubyObject obj) {
        RubyRect rect = RubySupport.checkType(obj, RubyRect.class);
        this.x = rect.x;
        this.y = rect.y;
        this.width = rect.width;
        this.height = rect.height;
        return this;
    }

    private IRubyObject set(IRubyObject x, IRubyObject y, IRubyObject width, IRubyObject height) {
        set_x(x);
        set_y(y);
        set_width(width);
        set_height(height);
        return this;
    }
}
