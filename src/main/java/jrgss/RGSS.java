package jrgss;

import java.awt.Window;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.GlobalVariable.Scope;
import org.jruby.internal.runtime.ValueAccessor;
import org.jruby.runtime.builtin.IRubyObject;

public class RGSS {
    public static Ruby runtime;

    public static RubyClass errorClass;
    public static RubyClass resetClass;

    public static RubyClass rectClass;
    public static RubyClass colorClass;
    public static RubyClass tableClass;

    public static RubyClass bitmapClass;
    public static RubyClass fontClass;

    public static RubyModule graphicsModule;
    public static RubyModule inputModule;

    public static void init(Window wnd, String[] args) {
        runtime = Ruby.newInstance(getRubyConfig());
        bootstrap();
        setGlobalVariables(args);
        RubyInput.init(wnd);
        RubyGraphics.init(wnd);
    }

    public static void reset() {
        RubyInput.reset();
        RubyGraphics.reset();
    }

    private static RubyInstanceConfig getRubyConfig() {
        RubyInstanceConfig config = new RubyInstanceConfig();

        return config;
    }

    public static void bootstrap() {
        resetClass = defineSubclass("RGSSReset", runtime.getException());
        errorClass = defineSubclass("RGSSError", runtime.getStandardError());

        RubyRect.createRectClass(runtime);
        RubyColor.createColorClass(runtime);
        RubyTable.createTableClass(runtime);

        RubyBitmap.createBitmapClass(runtime);
        RubyFont.createFontClass(runtime);

        RubyGraphics.createGraphicsModule(runtime);
        RubyInput.createInputModule(runtime);
    }

    private static RubyClass defineSubclass(String name, RubyClass superClass) {
        return superClass.getRuntime().defineClass(name, superClass, superClass.getAllocator());
    }

    private static void setGlobalVariables(String[] args) {
        boolean test = false;
        boolean btest = false;
        for (String arg : args) {
            if (arg.equalsIgnoreCase("test")) {
                test = true;
            } else if (arg.equalsIgnoreCase("best")) {
                test = true;
                btest = true;
            }
        }
        runtime.getGlobalVariables().define("$TEST", new ValueAccessor(runtime.newBoolean(test)), Scope.GLOBAL);
        runtime.getGlobalVariables().define("$BTEST", new ValueAccessor(runtime.newBoolean(btest)), Scope.GLOBAL);
    }

    // Argument Checking

    public static double checkRange(IRubyObject obj, String name, double min, double max) {
        double val = RubyNumeric.num2dbl(obj);
        if (val < min || val > max)
            throw obj.getRuntime().newArgumentError("bad value for " + name + " (" + val + " not between " + min + " and " + max + ")");
        return val;
    }

    // Type Checking

    public static RubyRect asRect(IRubyObject obj) {
        if (obj instanceof RubyRect) return (RubyRect) obj;
        throw newTypeConversionError(obj, rectClass);
    }

    public static RubyColor asColor(IRubyObject obj) {
        if (obj instanceof RubyColor) return (RubyColor) obj;
        throw newTypeConversionError(obj, colorClass);
    }

    public static RubyBitmap asBitmap(IRubyObject obj) {
        if (obj instanceof RubyBitmap) return (RubyBitmap) obj;
        throw newTypeConversionError(obj, bitmapClass);
    }

    public static RubyFont asFont(IRubyObject obj) {
        if (obj instanceof RubyFont) return (RubyFont) obj;
        throw newTypeConversionError(obj, fontClass);
    }

    // Errors

    public static RaiseException newReset(Ruby runtime) {
        return runtime.newRaiseException(resetClass, "");
    }

    public static RaiseException newError(Ruby runtime, String message) {
        return runtime.newRaiseException(errorClass, message);
    }

    public static RaiseException newTypeConversionError(IRubyObject obj, RubyClass target) {
        return obj.getRuntime().newTypeError("can't convert " + obj.getMetaClass().getRealClass().getName() + " into " + target.getName());
    }
}
