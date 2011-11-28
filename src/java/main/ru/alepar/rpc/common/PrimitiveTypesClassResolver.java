package ru.alepar.rpc.common;

import java.util.HashMap;
import java.util.Map;

import org.jboss.netty.handler.codec.serialization.ClassResolver;

public class PrimitiveTypesClassResolver implements ClassResolver {

    private final ClassResolver delegate;

    private static final Map<String, Class<?>> primClasses = new HashMap<String, Class<?>> (8, 1.0F) {{
        put("boolean", boolean.class);
        put("byte", byte.class);
        put("char", char.class);
        put("short", short.class);
        put("int", int.class);
        put("long", long.class);
        put("float", float.class);
        put("double", double.class);
        put("void", void.class);
    }};

    public PrimitiveTypesClassResolver(ClassResolver delegate) {
        this.delegate = delegate;
    }

    @Override
    public Class<?> resolve(String className) throws ClassNotFoundException {
        final Class<?> primClass = primClasses.get(className);
        if(primClass != null) {
            return primClass;
        }

        return delegate.resolve(className);
    }
}
