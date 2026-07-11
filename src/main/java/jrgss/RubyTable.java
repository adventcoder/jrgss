package jrgss;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

public class RubyTable extends RubyObject {
    public static RubyClass createTableClass(Ruby runtime) {
        RubyClass cls = runtime.defineClass("Table", runtime.getObject(), RubyTable::new);
        RubySupport.Table = cls;
        cls.defineAnnotatedMethods(RubyTable.class);
        return cls;
    }

    private int arity;
    private int xsize;
    private int ysize;
    private int zsize;
    private short[] data = new short[0];

    public RubyTable(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
    }

    public @JRubyMethod IRubyObject xsize() { return getRuntime().newFixnum(xsize); }
    public @JRubyMethod IRubyObject ysize() { return getRuntime().newFixnum(xsize); }
    public @JRubyMethod IRubyObject zsize() { return getRuntime().newFixnum(xsize); }

    @JRubyMethod(visibility = Visibility.PRIVATE, required = 1, optional = 2)
    public void initialize(IRubyObject... args) {
        this.arity = args.length;
        this.xsize = args.length >= 1 ? Math.max(RubyNumeric.num2int(args[0]), 0) : 1;
        this.ysize = args.length >= 2 ? Math.max(RubyNumeric.num2int(args[1]), 0) : 1;
        this.zsize = args.length >= 3 ? Math.max(RubyNumeric.num2int(args[2]), 0) : 1;
        this.data = new short[xsize * ysize * zsize];
    }

    @JRubyMethod(visibility = Visibility.PRIVATE)
    @Override
    public IRubyObject initialize_copy(IRubyObject obj) {
        if (obj == this) return this;
        if (obj instanceof RubyTable table) {
            this.arity = table.arity;
            this.xsize = table.xsize;
            this.ysize = table.ysize;
            this.zsize = table.zsize;
            this.data = table.data.clone();
        } else {
            throw getRuntime().newTypeError(obj, RubySupport.Table);
        }
        return this;
    }

    @JRubyMethod(required = 1, optional = 2)
    public void resize(IRubyObject... args) {
        int oldxsize = xsize, oldysize = ysize, oldzsize = zsize;
        short[] oldData = data;
        initialize(args);
        int xlimit = Math.min(xsize, oldxsize);
        int ylimit = Math.min(ysize, oldysize);
        int zlimit = Math.min(zsize, oldzsize);
        for (int z = 0; z < zlimit; z++) {
            int yoffset = z*ysize;
            int oldyoffset = z*oldysize;
            for (int y = 0; y < ylimit; y++) {
                int xoffset = (yoffset + y)*xsize;
                int oldxoffset = (oldyoffset + y)*oldxsize;
                System.arraycopy(oldData, oldxoffset, data, xoffset, xlimit);
            }
        }
    }

    @JRubyMethod(name = "[]", rest = true)
    public IRubyObject op_aref(IRubyObject... args) {
        Arity.checkArgumentCount(getRuntime(), args, arity, arity);
        int i = 0;
        if (arity >= 3) {
            int z = RubyNumeric.num2int(args[2]);
            if (z < 0 || z >= zsize) return getRuntime().getNil();
            i = i*zsize + z;
        }
        if (arity >= 2) {
            int y = RubyNumeric.num2int(args[1]);
            if (y < 0 || y >= ysize) return getRuntime().getNil();
            i = i*ysize + y;
        }
        if (arity >= 1) {
            int x = RubyNumeric.num2int(args[0]);
            if (x < 0 || x >= xsize) return getRuntime().getNil();
            i = i*xsize + x;
        }
        return getRuntime().newFixnum(data[i]);
    }

    @JRubyMethod(name = "[]=", rest = true)
    public IRubyObject op_aset(IRubyObject... args) {
        Arity.checkArgumentCount(getRuntime(), args, arity + 1, arity + 1);
        IRubyObject obj = args[arity];
        int i = 0;
        if (arity >= 3) {
            int z = RubyNumeric.num2int(args[2]);
            if (z < 0 || z >= zsize) return obj;
            i = i*zsize + z;
        }
        if (arity >= 2) {
            int y = RubyNumeric.num2int(args[1]);
            if (y < 0 || y >= ysize) return obj;
            i = i*ysize + y;
        }
        if (arity >= 1) {
            int x = RubyNumeric.num2int(args[0]);
            if (x < 0 || x >= xsize) return obj;
            i = i*xsize + x;
        }
        data[i] = (short) RubyNumeric.num2long(obj);
        return obj;
    }

    @JRubyMethod(name = "==")
    @Override
    public IRubyObject op_equal(ThreadContext context, IRubyObject other) {
        Ruby runtime = context.runtime;
        if (this == other) return runtime.getTrue();
        if (other instanceof RubyTable table)
            return runtime.newBoolean(arity == table.arity && xsize == table.xsize && ysize == table.ysize && zsize == table.zsize && Arrays.equals(data, table.data));
        return runtime.getFalse();
    }

    @JRubyMethod(name = "eql?")
    @Override
    public IRubyObject eql_p(IRubyObject other) {
        return op_equal(getRuntime().getCurrentContext(), other);
    }

    @JRubyMethod
    @Override
    public RubyFixnum hash() {
        long h = Helpers.hashStart(getRuntime(), arity);
        h = Helpers.murmurCombine(h, xsize);
        h = Helpers.murmurCombine(h, ysize);
        h = Helpers.murmurCombine(h, zsize);
        h = Helpers.murmurCombine(h, Arrays.hashCode(data));
        return getRuntime().newFixnum(Helpers.hashEnd(h));
    }

    @JRubyMethod
    @Override
    public IRubyObject to_s() {
        StringBuilder sb = new StringBuilder();
        if (arity == 1)
            appendX(sb, 0, 0, "");
        else if (arity == 2)
            appendY(sb, 0, "");
        else
            appendZ(sb, "");
        return RubyString.newString(getRuntime(), sb);
    }

    private void appendZ(StringBuilder sb, String margin) {
        sb.append("(");
        for (int z = 0; z < zsize; z++) {
            if (z > 0)
                sb.append(",\n\n").append(margin).append(" ");
            appendY(sb, z, margin + " ");
        }
        sb.append(")");
    }

    private void appendY(StringBuilder sb, int z, String margin) {
        sb.append("(");
        for (int y = 0; y < ysize; y++) {
            if (y > 0)
                sb.append(",\n").append(margin).append(" ");
            appendX(sb, z, y, margin + " ");
        }
        sb.append(")");
    }

    private void appendX(StringBuilder sb, int z, int y, String margin) {
        sb.append("(");
        for (int x = 0; x < xsize; x++) {
            if (x > 0)
                sb.append(", ");
            sb.append(data[x + xsize*(y + ysize*z)]);
        }
        sb.append(")");
    }

    @JRubyMethod
    public IRubyObject _dump(IRubyObject arg) {
        ByteBuffer buf = ByteBuffer.allocate(Integer.BYTES*5 + Short.BYTES*data.length).order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(arity);
        buf.putInt(xsize);
        buf.putInt(ysize);
        buf.putInt(zsize);
        buf.putInt(data.length);
        for (short val : data)
            buf.putShort(val);
        return RubySupport.newString(getRuntime(), buf, false);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject _load(IRubyObject recv, IRubyObject obj) {
        ByteBuffer buf = RubySupport.getByteBuffer(obj.convertToString());
        RubyTable table = (RubyTable) ((RubyClass) recv).allocate();
        //TODO: sanity checking
        table.arity = buf.getInt();
        table.xsize = buf.getInt();
        table.ysize = buf.getInt();
        table.zsize = buf.getInt();
        table.data = new short[buf.getInt()];
        for (int i = 0; i < table.data.length; i++)
            table.data[i] = buf.getShort();
        return table;
    }
}
