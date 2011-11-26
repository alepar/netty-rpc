package ru.alepar.rpc.api;

public interface ExceptionListener {
    void onExceptionCaught(Remote remote, Exception e);
}
