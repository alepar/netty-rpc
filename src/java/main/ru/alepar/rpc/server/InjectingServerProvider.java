package ru.alepar.rpc.server;

import ru.alepar.rpc.api.Inject;
import ru.alepar.rpc.api.Remote;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;

public class InjectingServerProvider<T> implements ServerProvider {

    private final Class<? extends T> implClass;

    public InjectingServerProvider(Class<? extends T> implClass) {
        this.implClass = implClass;
    }

    @Override
    public Object provideFor(Remote remote) {
        try {
            Constructor<?> constructor = findConstructor();
            return constructor.newInstance(makeArgumentsForConstructor(constructor, remote));
        } catch (Exception e) {
            throw new RuntimeException("failed to provide implementation for " + implClass.getCanonicalName(), e);
        }
    }

    private Object[] makeArgumentsForConstructor(Constructor<?> constructor, Remote remote) {
        Class<?>[] parameterTypes = constructor.getParameterTypes();
        Object[] arguments = new Object[parameterTypes.length];

        for (int i = 0; i < parameterTypes.length; i++) {
            arguments[i] = remote;
        }

        return arguments;
    }

    private Constructor<?> findConstructor() {
        for (Constructor<?> constructor : implClass.getConstructors()) {
            if(parametersAreFine(constructor)) {
                return constructor;
            }
        }
        throw new RuntimeException("no suitable constructor found - there should be either default constructor, or constructor with all params annotated as @Inject - " + implClass.getCanonicalName());
    }

    private static boolean parametersAreFine(Constructor<?> constructor) {
        for (int i = 0; i < constructor.getParameterTypes().length; i++) {
            Class<?> type = constructor.getParameterTypes()[i];

            if(!hasInjectAnnotation(constructor.getParameterAnnotations()[i]) || !Remote.class.isAssignableFrom(type)) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasInjectAnnotation(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if(annotation.annotationType().equals(Inject.class)) {
                return true;
            }
        }

        return false;
    }
}
