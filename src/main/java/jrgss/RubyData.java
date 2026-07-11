package jrgss;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyObject;
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

    @JRubyMethod(name = "==")
    @Override
    public IRubyObject op_equal(ThreadContext context, IRubyObject other) {
        return context.runtime.newBoolean(equals(other));
    }

    @JRubyMethod(name = "eql?")
    public IRubyObject eql_p(IRubyObject other) {
        return op_equal(other.getRuntime().getCurrentContext(), other);
    }

    @Override
    public abstract boolean equals(Object other);

    @JRubyMethod
    @Override
    public RubyFixnum hash() {
        return getRuntime().newFixnum(hashCode());
    }

    @Override
    public abstract int hashCode();

    @JRubyMethod
    public IRubyObject to_s() {
        return  getRuntime().newString(toString());
    }

    @Override
    public abstract String toString();

    @JRubyMethod
    public IRubyObject _dump(IRubyObject arg) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(baos)) {
            dump(out);
        } catch (IOException ioe) {
            throw getRuntime().newIOErrorFromException(ioe);
        }
        ByteList byteList = new ByteList(baos.toByteArray(), false);
        return getRuntime().newString(byteList);
    }

    public abstract void dump(DataOutputStream out) throws IOException;

    @JRubyMethod(meta = true)
    public static IRubyObject _load(IRubyObject recv, IRubyObject obj) {
        // is this thread safe, could the string be modified?
        ByteList byteList = obj.convertToString().getByteList();

        RubyData data = (RubyData) ((RubyClass) recv).allocate();

        ByteArrayInputStream bais = new ByteArrayInputStream(byteList.getUnsafeBytes(), byteList.getBegin(), byteList.getRealSize());
        try (DataInputStream in = new DataInputStream(bais)) {
            data.load(in);
        } catch (IOException ioe) {
            throw recv.getRuntime().newIOErrorFromException(ioe);
        }

        return data;
    }

    public abstract void load(DataInputStream in) throws IOException;
}
