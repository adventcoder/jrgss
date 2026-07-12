package jrgss;

import java.io.FileNotFoundException;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.GlobalVariable.Scope;
import org.jruby.internal.runtime.ValueAccessor;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.builtin.IRubyObject;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RubySupport {
    //TODO: All this state should be per runtime...

    public static Game game;

    public static RubyClass RGSSError;
    public static RubyClass RGSSReset;

    public static RubyClass Rect;
    public static RubyClass Color;
    public static RubyClass Tone;
    public static RubyClass Table;

    public static RubyClass Bitmap;
    public static RubyClass Font;

    public static RubyModule Graphics;
    public static RubyModule Input;
    public static RubyModule Audio;

    public static Game getGame(Ruby runtime) {
        return game;
    }

    public static void bootstrap(Ruby runtime) {
        RGSSReset = defineSubclass("RGSSReset", runtime.getException());
        RGSSError = defineSubclass("RGSSError", runtime.getStandardError());

        RubyRect.createRectClass(runtime);
        RubyColor.createColorClass(runtime);
        RubyTone.createToneClass(runtime);
        RubyTable.createTableClass(runtime);

        RubyBitmap.createBitmapClass(runtime);
        RubyFont.createFontClass(runtime);

        RubyGraphics.createGraphicsModule(runtime);
        RubyInput.createInputModule(runtime);
        RubyAudio.createAudioModule(runtime);

        runtime.getObject().defineAnnotatedMethods(RubyFunctions.class);
    }

    private static RubyClass defineSubclass(String name, RubyClass superClass) {
        return superClass.getRuntime().defineClass(name, superClass, superClass.getAllocator());
    }

    public static void setGlobalVariable(Ruby runtime, String name, Object obj) {
        IRubyObject rubyObj = JavaEmbedUtils.javaToRuby(runtime, obj);
        runtime.getGlobalVariables().define(name, new ValueAccessor(rubyObj), Scope.GLOBAL);
    }

    // Argument Checking

    public static int numToIntInRangeClamped(IRubyObject obj, int min, int max) {
        int val = RubyNumeric.num2int(obj);
        if (val < min) return min;
        if (val > max) return max;
        return val;
    }

    public static double numToDoubleInRangeClamped(IRubyObject obj, double min, double max) {
        double val = RubyNumeric.num2dbl(obj);
        if (val < min) return min;
        if (val > max) return max;
        return val;
    }

    public static double numToDoubleInRange(IRubyObject obj, String name, double min, double max) {
        double val = RubyNumeric.num2dbl(obj);
        if (val < min || val > max)
            throw obj.getRuntime().newArgumentError("bad value for " + name + " (" + val + " not between " + min + " and " + max + ")");
        return val;
    }

    // Type Checking/Conversion

    public static IRubyObject convertType(IRubyObject obj, RubyClass target) {
        // we don't actually do any conversion but the error messages shown is 'can't convert X into Y'
        if (target.isInstance(obj)) return obj;
        throw newTypeConversionError(obj, target);
    }

    public static RubyRect asRect(IRubyObject obj) {
        return (RubyRect) convertType(obj, Rect);
    }

    public static RubyColor asColor(IRubyObject obj) {
        return (RubyColor) convertType(obj, Color);
    }

    public static RubyBitmap asBitmap(IRubyObject obj) {
        return (RubyBitmap) convertType(obj, Bitmap);
    }

    public static RubyFont asFont(IRubyObject obj) {
        return (RubyFont) convertType(obj, Font);
    }

    public static <T extends RubyObject> T checkType(IRubyObject obj, Class<T> javaClass) {
        if (javaClass.isAssignableFrom(obj.getClass()))
            return javaClass.cast(obj);
        throw obj.getRuntime().newTypeError(obj, "Data");
    }

    // Errors

    public static RaiseException newRGSSReset(Ruby runtime) {
        return runtime.newRaiseException(RGSSReset, "");
    }

    public static RaiseException newRGSSError(Ruby runtime, String message) {
        return runtime.newRaiseException(RGSSError, message);
    }

    public static RaiseException newTypeConversionError(IRubyObject obj, RubyClass target) {
        return obj.getRuntime().newTypeError("can't convert " + obj.getMetaClass().getRealClass().getName() + " into " + target.getName());
    }

    public static RaiseException newFileNotFoundError(Ruby runtime, FileNotFoundException ex) {
        return runtime.newErrnoENOENTError(ex.getMessage().replaceFirst(" \\([^)]*\\)$", ""));
    }
}
