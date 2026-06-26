package jrgss;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyNumeric;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.builtin.IRubyObject;

public class RGSS {
    public static RubyClass errorClass;
    public static RubyClass resetClass;

    public static RubyClass rectClass;
    public static RubyClass colorClass;
    public static RubyClass toneClass;

    public static RubyClass bitmapClass;
    public static RubyClass fontClass;

    public static void bootstrap(Ruby runtime) {
        resetClass = defineSubclass("RGSSReset", runtime.getException());
        errorClass = defineSubclass("RGSSError", runtime.getStandardError());

        RubyRect.createRectClass(runtime);
        RubyColor.createColorClass(runtime);

        RubyBitmap.createBitmapClass(runtime);
        RubyFont.createFontClass(runtime);
    }

    private static RubyClass defineSubclass(String name, RubyClass superClass) {
        return superClass.getRuntime().defineClass(name, superClass, superClass.getAllocator());
    }

    // Argument Helpers

    public static double checkRange(IRubyObject obj, String name, double min, double max) {
        double val = RubyNumeric.num2dbl(obj);
        if (val < min || val > max)
            throw obj.getRuntime().newArgumentError("bad value for " + name + " (" + val + " not between " + min + " and " + max + ")");
        return val;
    }

    //TODO: use this
    public static double clampRange(IRubyObject obj, double min, double max) {
        return Math.min(Math.max(RubyNumeric.num2dbl(obj), min), max);
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

    public static RaiseException newError(Ruby runtime, String message) {
        return runtime.newRaiseException(errorClass, message);
    }

    public static RaiseException newTypeConversionError(IRubyObject obj, RubyClass target) {
        return obj.getRuntime().newTypeError("can't convert " + obj.getMetaClass().getRealClass().getName() + " into " + target.getName());
    }
}
