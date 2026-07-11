package jrgss;

import java.io.FileNotFoundException;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.GlobalVariable.Scope;
import org.jruby.javasupport.JavaObject;
import org.jruby.runtime.builtin.IRubyObject;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RubySupport {
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

    public static void setGame(Ruby runtime, Game game) {
        runtime.defineReadonlyVariable("$__GAME__", JavaObject.wrap(runtime, game), Scope.GLOBAL);
    }

    public static Game getGame(Ruby runtime) {
        IRubyObject gameObj = runtime.getGlobalVariables().get("$__GAME__");
        if (gameObj.isNil()) return null;
        return (Game) ((JavaObject) gameObj).getValue();
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

    // Type Checking

    public static RubyRect asRect(IRubyObject obj) {
        if (obj instanceof RubyRect) return (RubyRect) obj;
        throw newTypeConversionError(obj, Rect);
    }

    public static RubyColor asColor(IRubyObject obj) {
        if (obj instanceof RubyColor) return (RubyColor) obj;
        throw newTypeConversionError(obj, Color);
    }

    public static RubyBitmap asBitmap(IRubyObject obj) {
        if (obj instanceof RubyBitmap) return (RubyBitmap) obj;
        throw newTypeConversionError(obj, Bitmap);
    }

    public static RubyFont asFont(IRubyObject obj) {
        if (obj instanceof RubyFont) return (RubyFont) obj;
        throw newTypeConversionError(obj, Font);
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

    // Properties

   
}
