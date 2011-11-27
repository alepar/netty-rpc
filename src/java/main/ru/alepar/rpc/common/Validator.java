package ru.alepar.rpc.common;

import ru.alepar.rpc.api.exception.ConfigurationException;

import java.io.Serializable;
import java.lang.reflect.Method;

public class Validator {
    
    public void validateInterface(Class clazz) {
        if(!clazz.isInterface()) {
            throw new ConfigurationException("you must supply interface class instead of " + clazz.getCanonicalName());
        }

        for (Method method : clazz.getMethods()) {
            validateMethod(method);
        }
    }

    public void validateMethod(Method method) {
        for (int i = 0; i < method.getParameterTypes().length; i++) {
            Class<?> clazz = method.getParameterTypes()[i];
            if (!clazz.isPrimitive() && !Serializable.class.isAssignableFrom(clazz)) {
                throw new ConfigurationException("param #" + (i + 1) + "(" + clazz.getName() + ") is not serializable");
            }
        }
        Class<?> clazz = method.getReturnType();
        if(clazz != Void.TYPE) {
            throw new ConfigurationException("method must have void as return type");
        }
    }


}
