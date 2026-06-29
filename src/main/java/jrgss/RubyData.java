package jrgss;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

public abstract class RubyData extends RubyObject {
    public RubyData(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
    }

    public RubyData(Ruby runtime, RubyClass metaClass, boolean useObjectSpace) {
        super(runtime, metaClass, useObjectSpace);
    }

    // Overload to use java Object.toString
    @JRubyMethod(name = "to_s")
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
    @JRubyMethod(name = "hash", required = 1)
    @Override
    public RubyFixnum hash() {
        return getRuntime().newFixnum(hashCode());
    }

    @JRubyMethod
    public IRubyObject _dump(IRubyObject depth) {
        ByteBuffer buf = ByteBuffer.allocate(dataSize()).order(ByteOrder.LITTLE_ENDIAN);
        dump(buf);
        ByteList bytes = new ByteList(buf.array(), 0, buf.limit(), false);
        return getRuntime().newString(bytes);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject _load(IRubyObject recv, IRubyObject obj) {
        Ruby runtime = recv.getRuntime();

        ByteList bytes = obj.convertToString().getByteList();
        ByteBuffer buf = ByteBuffer.wrap(bytes.getUnsafeBytes(), bytes.begin(), bytes.length()).order(ByteOrder.LITTLE_ENDIAN);

        RubyData data = (RubyData) ((RubyClass) recv).allocate();
        if (buf.remaining() < data.dataSize())
            throw runtime.newArgumentError("not enough data");
        data.load(buf);
        return data;
    }

    public abstract int dataSize();
    public abstract void dump(ByteBuffer buf);
    public abstract void load(ByteBuffer buf);
}
