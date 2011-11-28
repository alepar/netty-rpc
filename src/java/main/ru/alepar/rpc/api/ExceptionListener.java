package ru.alepar.rpc.api;

public interface ExceptionListener {

    /**
     * called when exception event happens
     * @param remote associated with client, which is responsible for exception
     * @param e exception thrown
     */
    void onExceptionCaught(Remote remote, Exception e);
}
