package jrgss;

import java.awt.Rectangle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

public class RubyRect extends RubyObject {
    public static void createRectClass(Ruby runtime) {
        RubyClass cls = runtime.defineClass("Rect", runtime.getObject(), RubyRect::new);
        RubySupport.Rect = cls;
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

    public static RubyRect newRect(Ruby runtime, int x, int y, int width, int height) {
        RubyRect rect = new RubyRect(runtime, RubySupport.Rect);
        rect.x = x;
        rect.y = y;
        rect.width = width;
        rect.height = height;
        return rect;
    }

    public static RubyRect newRectLight(Ruby runtime, int x, int y, int width, int height) {
        RubyRect rect = new RubyRect(runtime, RubySupport.Rect, false);
        rect.x = x;
        rect.y = y;
        rect.width = width;
        rect.height = height;
        return rect;
    }

    public static RubyRect newRectLight(Ruby runtime, IRubyObject[] args, int i) {
        return newRectLight(runtime, RubyNumeric.num2int(args[i]), RubyNumeric.num2int(args[i+1]), RubyNumeric.num2int(args[i+2]), RubyNumeric.num2int(args[i+3]));
    }

    public Rectangle toAwtRectangle() {
        return new Rectangle(x, y, width, height);
    }

    @JRubyMethod
    public IRubyObject x() {
        return  getRuntime().newFixnum(x);
    }

    @JRubyMethod(name = "x=")
    public void set_x(IRubyObject obj) {
        this.x = RubyNumeric.num2int(obj);
    }

    @JRubyMethod
    public IRubyObject y() {
        return  getRuntime().newFixnum(y);
    }

    @JRubyMethod(name = "y=")
    public void set_y(IRubyObject obj) {
        this.y = RubyNumeric.num2int(obj);
    }

    @JRubyMethod
    public IRubyObject width() {
        return  getRuntime().newFixnum(width);
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
        if (obj instanceof RubyRect rect) {
            this.x = rect.x;
            this.y = rect.y;
            this.width = rect.width;
            this.height = rect.height;
        } else {
            throw obj.getRuntime().newTypeError(obj, RubySupport.Rect);
        }
        return this;
    }

    private IRubyObject set(IRubyObject x, IRubyObject y, IRubyObject width, IRubyObject height) {
        set_x(x);
        set_y(y);
        set_width(width);
        set_height(height);
        return this;
    }

    @JRubyMethod
    @Override
    public IRubyObject to_s() {
        return getRuntime().newString(String.format("(%d, %d, %d, %d)", x, y, width, height));
    }

    @JRubyMethod(name = "==")
    @Override
    public IRubyObject op_equal(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyRect rect)
            return context.runtime.newBoolean(x == rect.x && y == rect.y && width == rect.width && height == rect.height);
        return super.op_equal(context, other);
    }

    @JRubyMethod(name = "eql?")
    @Override
    public IRubyObject eql_p(IRubyObject other) {
        return op_equal(getRuntime().getCurrentContext(), other);
    }

    @JRubyMethod
    @Override
    public RubyFixnum hash() {
        int h = 1;
        h = h*31 + x;
        h = h*31 + y;
        h = h*31 + width;
        h = h*31 + height;
        return getRuntime().newFixnum(h);
    }

    @JRubyMethod
    public IRubyObject _dump(IRubyObject arg) {
        ByteBuffer buf = ByteBuffer.allocate(Integer.BYTES*4).order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(x);
        buf.putInt(y);
        buf.putInt(width);
        buf.putInt(height);
        return getRuntime().newString(new ByteList(buf.array(), false));
    }

    @JRubyMethod(meta = true)
    public static IRubyObject _load(IRubyObject recv, IRubyObject obj) {
        ByteList byteList = obj.convertToString().getByteList();
        ByteBuffer buf = ByteBuffer.wrap(byteList.getUnsafeBytes(), byteList.getBegin(), byteList.getRealSize()).order(ByteOrder.LITTLE_ENDIAN);
        RubyRect rect = (RubyRect) ((RubyClass) recv).allocate();
        rect.x = buf.getInt();
        rect.y = buf.getInt();
        rect.width = buf.getInt();
        rect.height = buf.getInt();
        return rect;
    }
}
