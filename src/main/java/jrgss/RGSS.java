package jrgss;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.builtin.IRubyObject;

public class RGSS {
    public static RubyClass errorClass;
    public static RubyClass resetClass;

    public static RubyClass rectClass;
    public static RubyClass colorClass;
    public static RubyClass toneClass;
    public static RubyClass bitmapClass;

    public static void bootstrap(Ruby runtime) {
        resetClass = defineSubclass("RGSSReset", runtime.getException());
        errorClass = defineSubclass("RGSSError", runtime.getStandardError());

        RubyRect.createRectClass(runtime);
        RubyColor.createColorClass(runtime);
        RubyBitmap.createBitmapClass(runtime);
    }

    private static RubyClass defineSubclass(String name, RubyClass superClass) {
        return superClass.getRuntime().defineClass(name, superClass, superClass.getAllocator());
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
        throw newTypeConversionError(obj, colorClass);
    }

    // Errors

    public static RaiseException newError(Ruby runtime, String message) {
        return runtime.newRaiseException(errorClass, message);
    }

    public static RaiseException newTypeConversionError(IRubyObject obj, RubyClass target) {
        return obj.getRuntime().newTypeError("can't convert " + obj.getMetaClass().getRealClass().getName() + " into " + target.getName());
    }
}
