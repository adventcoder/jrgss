package jrgss;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

public class RubyFont extends RubyObject {
    public static void createFontClass(Ruby runtime) {
        RubyClass cls = runtime.defineClass("Font", runtime.getObject(), RubyFont::new);
        RubySupport.Font = cls;
        cls.defineAnnotatedMethods(RubyFont.class);
        cls.setInternalModuleVariable("defaultFont", createDefaultFont(runtime));
    }

    private static RubyFont createDefaultFont(Ruby runtime) {
        RubyFont font = new RubyFont(runtime, RubySupport.Font, false);
        font.name = runtime.newString(Font.SANS_SERIF);
        font.size = 24;
        font.bold = false;
        font.italic = false;
        font.outline = true;
        font.shadow = false;
        font.color = RubyColor.newColor(runtime, 255, 255, 255);
        font.outColor = RubyColor.newColor(runtime, 0, 0, 0, 128);
        return font;
    }

    private static RubyFont getDefaultFont(IRubyObject recv) {
        return (RubyFont) ((RubyClass) recv).getInternalVariable("defaultFont");
    }

    public static @JRubyMethod(meta = true) IRubyObject default_name(IRubyObject recv) { return getDefaultFont(recv).name(); }
    public static @JRubyMethod(meta = true) IRubyObject default_size(IRubyObject recv) { return getDefaultFont(recv).size(); }
    public static @JRubyMethod(meta = true) IRubyObject default_bold(IRubyObject recv) { return getDefaultFont(recv).bold(); }
    public static @JRubyMethod(meta = true) IRubyObject default_italic(IRubyObject recv) { return getDefaultFont(recv).italic(); }
    public static @JRubyMethod(meta = true) IRubyObject default_outline(IRubyObject recv) { return getDefaultFont(recv).outline(); }
    public static @JRubyMethod(meta = true) IRubyObject default_shadow(IRubyObject recv) { return getDefaultFont(recv).shadow(); }
    public static @JRubyMethod(meta = true) IRubyObject default_color(IRubyObject recv) { return getDefaultFont(recv).color(); }
    public static @JRubyMethod(meta = true) IRubyObject default_out_color(IRubyObject recv) { return getDefaultFont(recv).out_color(); }

    public static @JRubyMethod(meta = true, name = "default_name=") IRubyObject set_default_name(IRubyObject recv, IRubyObject obj) { return getDefaultFont(recv).set_name(obj); }
    public static @JRubyMethod(meta = true, name = "default_size=") IRubyObject set_default_size(IRubyObject recv, IRubyObject obj) { return getDefaultFont(recv).set_size(obj); }
    public static @JRubyMethod(meta = true, name = "default_bold=") IRubyObject set_default_bold(IRubyObject recv, IRubyObject obj) { return getDefaultFont(recv).set_bold(obj); }
    public static @JRubyMethod(meta = true, name = "default_italic=") IRubyObject set_default_italic(IRubyObject recv, IRubyObject obj) { return getDefaultFont(recv).set_italic(obj); }
    public static @JRubyMethod(meta = true, name = "default_outline=") IRubyObject set_default_outline(IRubyObject recv, IRubyObject obj) { return getDefaultFont(recv).set_outline(obj); }
    public static @JRubyMethod(meta = true, name = "default_shadow=") IRubyObject set_default_shadow(IRubyObject recv, IRubyObject obj) { return getDefaultFont(recv).set_shadow(obj); }
    public static @JRubyMethod(meta = true, name = "default_color=") IRubyObject set_default_color(IRubyObject recv, IRubyObject obj) { return getDefaultFont(recv).set_color(obj); }
    public static @JRubyMethod(meta = true, name = "default_out_color=") IRubyObject set_default_out_color(IRubyObject recv, IRubyObject obj) { return getDefaultFont(recv).set_out_color(obj); }

    public IRubyObject name;
    public double size;
    public boolean bold, italic, outline, shadow;
    public RubyColor color, outColor;
    private transient Font baseFont;

    public RubyFont(Ruby runtime, RubyClass metaClass, boolean useObjectSpace) {
        super(runtime, metaClass, useObjectSpace);
        // we shouldn't expose nulls after Font.allocate
        name = runtime.getNil();
        color = new RubyColor(runtime);
        outColor = new RubyColor(runtime);
    }

    public RubyFont(Ruby runtime, RubyClass metaClass) {
        this(runtime, metaClass, true);
    }

    public @JRubyMethod IRubyObject name() { return name; }
    public @JRubyMethod IRubyObject size() { return getRuntime().newFloat(size); }
    public @JRubyMethod IRubyObject bold() { return getRuntime().newBoolean(bold); }
    public @JRubyMethod IRubyObject italic() { return getRuntime().newBoolean(italic); }
    public @JRubyMethod IRubyObject outline() { return getRuntime().newBoolean(outline); }
    public @JRubyMethod IRubyObject shadow() { return getRuntime().newBoolean(shadow); }
    public @JRubyMethod IRubyObject color() { return color; }
    public @JRubyMethod IRubyObject out_color() { return outColor; }

    @JRubyMethod(name = "name=")
    public IRubyObject set_name(IRubyObject obj) {
        name = obj;
        baseFont = null;
        return obj;
    }

    @JRubyMethod(name = "size=")
    public IRubyObject set_size(IRubyObject obj) {
        size = RubySupport.checkRange(obj, "size", 6, 96);
        return obj;
    }

    @JRubyMethod(name = "bold=")
    public IRubyObject set_bold(IRubyObject obj) {
        this.bold = obj.isTrue();
        updateBaseStyle();
        return obj;
    }

    @JRubyMethod(name = "italic=")
    public IRubyObject set_italic(IRubyObject obj) {
        this.italic = obj.isTrue();
        updateBaseStyle();
        return obj;
    }

    @JRubyMethod(name = "outline=")
    public IRubyObject set_outline(IRubyObject obj) {
        this.outline = obj.isTrue();
        return obj;
    }

    @JRubyMethod(name = "shadow=")
    public IRubyObject set_shadow(IRubyObject obj) {
        this.shadow = obj.isTrue();
        return obj;
    }

    @JRubyMethod(name = "color=")
    public IRubyObject set_color(IRubyObject obj) {
        this.color.set(obj);
        return obj;
    }

    @JRubyMethod(name = "out_color=")
    public IRubyObject set_out_color(IRubyObject obj) {
        this.outColor.set(obj);
        return obj;
    }

    @JRubyMethod(visibility = Visibility.PRIVATE, optional = 2)
    public void initialize(IRubyObject... args) {
        set(getDefaultFont(getMetaClass().getRealClass()));
        if (args.length >= 1) set_name(args[0]);
        if (args.length >= 2) set_size(args[1]);
    }

    @JRubyMethod(visibility = Visibility.PRIVATE)
    @Override
    public IRubyObject initialize_copy(IRubyObject obj) {
        if (obj == this) return this;
        if (obj instanceof RubyFont font) {
            set(font);
        } else {
            throw getRuntime().newTypeError(obj, RubySupport.Font);
        }
        return this;
    }

    public void set(RubyFont font) {
        this.name = font.name;
        this.size = font.size;
        this.bold = font.bold;
        this.italic = font.italic;
        this.outline = font.outline;
        this.shadow = font.shadow;
        this.color.set(font.color);
        this.outColor.set(font.outColor);
        this.baseFont = font.getBaseFont();
    }

    @JRubyMethod(meta = true)
    public static IRubyObject list(IRubyObject recv) {
        String[] names = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        RubyString[] nameObjs = new RubyString[names.length];
        for (int i = 0; i < names.length; i++)
            nameObjs[i] = RubyString.newString(recv.getRuntime(), names[i]);
        return RubyArray.newArrayMayCopy(recv.getRuntime(), nameObjs);
    }

    @JRubyMethod(name = "exist?", meta = true)
    public static IRubyObject exist_p(IRubyObject recv, IRubyObject obj) {
        String name = obj.asString().asJavaString();
        Font font = new Font(name, Font.PLAIN, 1);
        return recv.getRuntime().newBoolean(font.getFamily().equalsIgnoreCase(name));
    }

    public Color getColor() {
        return color.getColor();
    }

    public Color getOutColor() {
        int r = (int) (outColor.red + 0.5);
        int g = (int) (outColor.green + 0.5);
        int b = (int) (outColor.blue + 0.5);
        int a = (int) (outColor.alpha * color.alpha / 255.0 + 0.5);
        return new Color(r, g, b, a);
    }

    public Color getShadowColor() {
        int a = (int) (color.alpha + 0.5);
        return new Color(0, 0, 0, a);
    }

    public Font getFont(FontRenderContext fontRenderContext) {
        Font base = getBaseFont();
        LineMetrics lineMetrics = base.getLineMetrics("", fontRenderContext);
        return base.deriveFont(getFontStyle(), (float) (size / lineMetrics.getHeight()));
    }

    // the logical font at size 1pt.
    // we can't determine the pixel size without a FontRenderContext as that is device dependent.
    private Font getBaseFont() {
        if (baseFont == null)
            baseFont = lookupFont(name, getFontStyle(), 1);
        return baseFont;
    }

    private static Font lookupFont(IRubyObject name, int style, int sizePts) {
        if (name instanceof RubyArray) {
            RubyArray<?> nameAsArray = (RubyArray<?>) name;
            for (int i = 0; i < nameAsArray.getLength(); i++) {
                IRubyObject elt = nameAsArray.eltOk(i);
                Font font = lookupFont(elt.asString().asJavaString(), style, sizePts);
                if (font != null) return font;
            }
        } else {
            Font font = lookupFont(name.asString().asJavaString(), style, sizePts);
            if (font != null) return font;
        }
        return new Font(Font.SANS_SERIF, style, sizePts);
    }

    private static Font lookupFont(String name, int style, int sizePts) {
        Font font = new Font(name, style, sizePts);
        if (font.getFamily().equalsIgnoreCase(name) || font.getFontName().equalsIgnoreCase(name))
            return font;
        return null;
    }

    private void updateBaseStyle() {
        int style = getFontStyle();
        if (baseFont != null && baseFont.getStyle() != style)
            baseFont = baseFont.deriveFont(style);
    }

    private int getFontStyle() {
        int style = 0;
        if (bold) style |= Font.BOLD;
        if (italic) style |= Font.ITALIC;
        return style;
    }
}
