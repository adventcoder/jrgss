package jrgss;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyNumeric;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

public class RubyTable extends RubyData {
    public static RubyClass createTableClass(Ruby runtime) {
        RubyClass cls = runtime.defineClass("Table", runtime.getObject(), RubyTable::new);
        RGSS.tableClass = cls;
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
            throw getRuntime().newTypeError(obj, RGSS.tableClass);
        }
        return this;
    }

    @JRubyMethod(required = 1, optional = 2)
    public void resize(IRubyObject... args) {
        int oldxsize = xsize, oldysize = ysize, oldzsize = zsize;
        short[] oldData = data;
        initialize(args);
        int minxsize = Math.min(xsize, oldxsize);
        int minysize = Math.min(ysize, oldysize);
        int minzsize = Math.min(zsize, oldzsize);
        for (int z = 0; z < minzsize; z++) {
            int yoffset = z*ysize;
            int oldyoffset = z*oldysize;
            for (int y = 0; y < minysize; y++) {
                int xoffset = (yoffset + y)*xsize;
                int oldxoffset = (oldyoffset + y)*oldxsize;
                System.arraycopy(oldData, oldxoffset, data, xoffset, minxsize);
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (arity == 1)
            appendX(sb, 0, 0, "");
        else if (arity == 2)
            appendY(sb, 0, "");
        else
            appendZ(sb, "");
        return sb.toString();
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

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof RubyTable table)) return false;
        return xsize == table.xsize && ysize == table.ysize && zsize == table.zsize && Arrays.equals(data, table.data);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(xsize, ysize, zsize);
        result = result * 31 + Arrays.hashCode(data);
        return result;
    }

    @Override
    public int dataSize() {
        return Integer.BYTES*5 + Short.BYTES*data.length;
    }

    @Override
    public void dump(ByteBuffer buf) {
        buf.putInt(arity);
        buf.putInt(xsize);
        buf.putInt(ysize);
        buf.putInt(zsize);
        buf.putInt(data.length);
        for (short val : data)
            buf.putShort(val);
    }

    @Override
    public void load(ByteBuffer buf) {
        //TODO: sanity checking
        arity = buf.getInt();
        xsize = buf.getInt();
        ysize = buf.getInt();
        zsize = buf.getInt();
        data = new short[buf.getInt()];
        for (int i = 0; i < data.length; i++)
            data[i] = buf.getShort();
    }
}
