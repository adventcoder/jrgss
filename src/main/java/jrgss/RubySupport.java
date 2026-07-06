package jrgss;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.GlobalVariable.Scope;
import org.jruby.internal.runtime.ValueAccessor;
import org.jruby.runtime.builtin.IRubyObject;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RubySupport {
    public static RubyClass rgssErrorClass;
    public static RubyClass rgssResetClass;

    public static RubyClass rectClass;
    public static RubyClass colorClass;
    public static RubyClass tableClass;

    public static RubyClass bitmapClass;
    public static RubyClass fontClass;

    public static RubyModule graphicsModule;
    public static RubyModule inputModule;

    public static void bootstrap(Ruby runtime) {
        rgssResetClass = defineSubclass("RGSSReset", runtime.getException());
        rgssErrorClass = defineSubclass("RGSSError", runtime.getStandardError());

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

    public static void setGlobalVariable(Ruby runtime, String name, boolean value) {
        runtime.getGlobalVariables().define(name, new ValueAccessor(runtime.newBoolean(value)), Scope.GLOBAL);
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

    public static RaiseException newRGSSReset(Ruby runtime) {
        return runtime.newRaiseException(rgssResetClass, "");
    }

    public static RaiseException newRGSSError(Ruby runtime, String message) {
        return runtime.newRaiseException(rgssErrorClass, message);
    }

    public static RaiseException newTypeConversionError(IRubyObject obj, RubyClass target) {
        return obj.getRuntime().newTypeError("can't convert " + obj.getMetaClass().getRealClass().getName() + " into " + target.getName());
    }
}
