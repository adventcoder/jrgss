package jrgss.property;

import java.lang.reflect.Field;
import java.util.function.ToDoubleFunction;

import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.internal.runtime.methods.JavaMethod;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

import jrgss.RubySupport;

public class DoubleProperty<T extends IRubyObject> {
    private final Field field;
    private final ToDoubleFunction<IRubyObject> rubyToJava;
    private final String attrName;

    public DoubleProperty(Class<T> javaClass, String fieldName, ToDoubleFunction<IRubyObject> rubyToJava) {
        try {
            field = javaClass.getDeclaredField(fieldName);
        } catch (NoSuchFieldException ex) {
            throw new IllegalArgumentException(ex.getMessage());
        }
        this.rubyToJava = rubyToJava;
        this.attrName = JavaUtil.getRubyCasedName(field.getName());
    }

    public DoubleProperty(Class<T> javaClass, String fieldName) {
        this(javaClass, fieldName, RubyNumeric::num2dbl);
    }

    public DoubleProperty(Class<T> javaClass, String fieldName, double min, double max) {
        this(javaClass, fieldName, obj -> RubySupport.numToDoubleInRangeClamped(obj, min, max));
    }

    public IRubyObject get(IRubyObject self) {
        try {
            return RubyNumeric.dbl2num(self.getRuntime(), field.getDouble(self));
        } catch (IllegalAccessException ex) {
            throw self.getRuntime().newSecurityError(ex.getMessage());
        }
    }

    public IRubyObject set(IRubyObject self, IRubyObject arg0) {
        try {
            field.setDouble(self, rubyToJava.applyAsDouble(arg0));
            return arg0;
        } catch (IllegalAccessException ex) {
            throw self.getRuntime().newSecurityError(ex.getMessage());
        }
    }

    public void attrAccessor(RubyClass rubyClass) {
        attrReader(rubyClass);
        attrWriter(rubyClass);
    }

    public void attrReader(RubyClass rubyClass) {
        rubyClass.addMethod(attrName, new JavaMethod.JavaMethodZero(rubyClass, Visibility.PUBLIC, attrName) {
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
                return get(self);
            }
        });
    }

    public void attrWriter(RubyClass rubyClass) {
        rubyClass.addMethod(attrName + "=", new JavaMethod.JavaMethodOne(rubyClass, Visibility.PUBLIC, attrName + "=") {
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0) {
                return set(self, arg0);
            }
        });
    }
}