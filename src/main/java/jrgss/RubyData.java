package jrgss;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.javasupport.JavaObject;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.JRubyObjectInputStream;

//TODO: could probably use JavaObject for these...
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
    @JRubyMethod(name = { "==" })
    @Override
    public IRubyObject op_equal(ThreadContext context, IRubyObject obj) {
        return this.equals(obj) ? context.runtime.getTrue() : context.runtime.getFalse();
    }

    // Overload to use java Object.hashCode for convenience with lombok
    @JRubyMethod(name = "hash")
    @Override
    public RubyFixnum hash() {
        return getRuntime().newFixnum(hashCode());
    }

    @JRubyMethod
    public IRubyObject _dump(IRubyObject depth) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream dos = new DataOutputStream(baos)) {
            writeData(dos);
        } catch (IOException ex) {
            throw getRuntime().newIOErrorFromException(ex);
        }
        ByteList bytes = new ByteList(baos.toByteArray(), false);
        return getRuntime().newString(bytes);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject _load(IRubyObject recv, IRubyObject obj) {
        Ruby runtime = recv.getRuntime();
        ByteList bytes = obj.convertToString().getByteList();
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes.getUnsafeBytes(), bytes.getBegin(), bytes.getRealSize());
        RubyData dataObj = (RubyData) ((RubyClass) recv).allocate();
        try (DataInputStream dis = new DataInputStream(bais)) {
            dataObj.readData(dis);
        } catch (IOException ex) {
            throw runtime.newIOErrorFromException(ex);
        }
        return dataObj;
    }

    protected abstract void writeData(DataOutputStream out) throws IOException;
    protected abstract void readData(DataInputStream in) throws IOException;
}
