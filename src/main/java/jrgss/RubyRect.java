package jrgss;

import java.awt.Rectangle;
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

public class RubyRect extends RubyObject {
    public static void createRectClass(Ruby runtime) {
        RubyClass cls = runtime.defineClass("Rect", runtime.getObject(), RubyRect::new);
        RGSS.rectClass = cls;
        cls.defineAnnotatedMethods(RubyRect.class);
    }

    public int x;
    public int y;
    public int width;
    public int height;

    public RubyRect(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
    }

    public RubyRect(Ruby runtime, RubyClass metaClass, int x, int y, int width, int height, boolean objectSpace) {
        super(runtime, metaClass, objectSpace);
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public static RubyRect newRect(Ruby runtime, int x, int y, int width, int height) {
        return new RubyRect(runtime, RGSS.rectClass, x, y, width, height, true);
    }

    public static RubyRect newRectLight(Ruby runtime, int x, int y, int width, int height) {
        return new RubyRect(runtime, RGSS.rectClass, x, y, width, height, false);
    }

    public static RubyRect newRectLight(Ruby runtime, IRubyObject[] args, int i) {
        return newRectLight(runtime,
            RubyNumeric.num2int(args[i]),
            RubyNumeric.num2int(args[i + 1]),
            RubyNumeric.num2int(args[i + 2]),
            RubyNumeric.num2int(args[i + 3])
        );
    }

    @JRubyMethod
    public IRubyObject x() {
        return RubyNumeric.int2fix(getRuntime(), x);
    }

    @JRubyMethod
    public IRubyObject y() {
        return RubyNumeric.int2fix(getRuntime(), y);
    }

    @JRubyMethod
    public IRubyObject width() {
        return RubyNumeric.int2fix(getRuntime(), width);
    }

    @JRubyMethod
    public IRubyObject height() {
        return RubyNumeric.int2fix(getRuntime(), width);
    }

    @JRubyMethod(name = "x=")
    public IRubyObject x_set(IRubyObject obj) {
        this.x = RubyNumeric.num2int(obj);
        return obj;
    }

    @JRubyMethod(name = "y=")
    public IRubyObject y_set(IRubyObject obj) {
        this.y = RubyNumeric.num2int(obj);
        return obj;
    }

    @JRubyMethod(name = "width=")
    public IRubyObject width_set(IRubyObject obj) {
        this.width = RubyNumeric.num2int(obj);
        return obj;
    }

    @JRubyMethod(name = "height=")
    public IRubyObject height_set(IRubyObject obj) {
        this.height = RubyNumeric.num2int(obj);
        return obj;
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
            throw obj.getRuntime().newTypeError(obj, RGSS.rectClass);
        }
        return this;
    }

    private IRubyObject set(IRubyObject x, IRubyObject y, IRubyObject width, IRubyObject height) {
        x_set(x);
        y_set(y);
        width_set(width);
        height_set(height);
        return this;
    }

    @JRubyMethod
    @Override
    public IRubyObject to_s() {
        return getRuntime().newString(String.format("(%d, %d, %d, %d)", x, y, width, height));
    }

    public Rectangle toJavaRectangle() {
        return new Rectangle(x, y, width, height);
    }

    @JRubyMethod(name = "==", required = 1)
    @Override
    public IRubyObject op_equal(ThreadContext context, IRubyObject obj) {
        if (this == obj) return context.tru;
        if (!(obj instanceof RubyRect rect)) return context.fals;
        return x == rect.x && y == rect.y && width == rect.width && height == rect.height ? context.tru : context.fals;
    }

    @JRubyMethod
    public IRubyObject _dump(IRubyObject depth) {
        ByteBuffer buf = ByteBuffer.allocate(Integer.BYTES*4).order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(x);
        buf.putInt(y);
        buf.putInt(width);
        buf.putInt(height);
        return getRuntime().newString(new ByteList(buf.array(), false));
    }

    @JRubyMethod(meta = true)
    public static IRubyObject _load(IRubyObject recv, IRubyObject obj) {
        RubyString data = obj.convertToString();
        ByteBuffer buf = ByteBuffer.wrap(data.getBytes()).order(ByteOrder.LITTLE_ENDIAN);
        RubyRect rect = (RubyRect) ((RubyClass) recv).allocate();
        try {
            rect.x = buf.getInt();
            rect.y = buf.getInt();
            rect.width = buf.getInt();
            rect.height = buf.getInt();
        } catch (BufferUnderflowException e) {
            throw recv.getRuntime().newArgumentError("not enough data");
        }
        return rect;
    }
}
