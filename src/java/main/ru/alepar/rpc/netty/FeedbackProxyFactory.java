package ru.alepar.rpc.netty;

import org.jboss.netty.channel.Channel;
import ru.alepar.rpc.Remote;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static ru.alepar.rpc.netty.Util.*;

class FeedbackProxyFactory implements Remote.ProxyFactory {

    private final Channel clientChannel;

    FeedbackProxyFactory(Channel clientChannel) {
        this.clientChannel = clientChannel;
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public <T> T getProxy(Class<T> clazz) {
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, new FeedbackProxyHandler(clientChannel));
    }

    private class FeedbackProxyHandler implements InvocationHandler {

        private final Channel channel;

        private FeedbackProxyHandler(Channel channel) {
            this.channel = channel;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            validateMethod(method);
            channel.write(new InvocationRequest(method.getDeclaringClass().getName(), method.getName(), toSerializable(args), method.getParameterTypes()));
            return null;
        }
    }

}
