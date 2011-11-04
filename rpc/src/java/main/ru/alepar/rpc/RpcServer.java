package ru.alepar.rpc;

public interface RpcServer {

    <T> void addImplementation(Class<T> interfaceClass, T implObject);

    <T> void addClass(Class<T> interfaceClass, Class<? extends T> implClass);

    <T> void addFactory(Class<T> interfaceClass, ImplementationFactory<? extends T> factory);

    void addExceptionListener(ExceptionListener listener);

    void shutdown();

    public interface ExceptionListener {
        void onExceptionCaught(Exception e);
    }
}
