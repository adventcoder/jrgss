package jrgss;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.function.IntFunction;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

//TODO: could probably use JavaObject for these...
//TODO: or do i need this base class at all...
//TODO: or should it be added as a RubyClass also
public abstract class RubyData extends RubyObject {
    public RubyData(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
    }

    public RubyData(Ruby runtime, RubyClass metaClass, boolean useObjectSpace) {
        super(runtime, metaClass, useObjectSpace);
    }

    // Overload to use java Object.toString
    @JRubyMethod
    @Override
    public RubyString to_s() {
        return getRuntime().newString(toString());
    }

    // Overload to use java Object.equals for convenience with lombok
    @JRubyMethod(name = "==")
    @Override
    public IRubyObject op_equal(ThreadContext context, IRubyObject obj) {
        return this.equals(obj) ? context.runtime.getTrue() : context.runtime.getFalse();
    }

    // Overload to use java Object.hashCode for convenience with lombok
    @JRubyMethod
    @Override
    public RubyFixnum hash() {
        return getRuntime().newFixnum(hashCode());
    }

    @JRubyMethod
    public IRubyObject _dump(IRubyObject depth) {
        ByteBuffer buf = ByteBuffer.allocate(dataSize()).order(ByteOrder.LITTLE_ENDIAN);
        dump(buf);
        ByteList bytes = new ByteList(buf.array(), 0, buf.position(), false);
        return getRuntime().newString(bytes);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject _load(IRubyObject recv, IRubyObject obj) {
        ByteList bytes = obj.convertToString().getByteList();

        ByteBuffer buf = ByteBuffer.wrap(bytes.getUnsafeBytes(), bytes.getBegin(), bytes.getRealSize()).order(ByteOrder.LITTLE_ENDIAN);

        RubyData dataObj = (RubyData) ((RubyClass) recv).allocate();
        try {
            dataObj.load(buf);
        } catch (BufferUnderflowException ex) {
            throw recv.getRuntime().newArgumentError("not enough data");
        }
        return dataObj;
    }

    public abstract int dataSize();
    public abstract void dump(ByteBuffer buf);
    public abstract void load(ByteBuffer buf);
}
