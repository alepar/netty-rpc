package ru.alepar.rpc.exception;

/**
 * subclasses of this exception will be passed to client as-is (i.e. without wrapping into RemoteException)
 * this implies that such exception classes must be available in client classpath
 */
public class SafeRuntimeException extends RuntimeException {

    public SafeRuntimeException() {
    }

    public SafeRuntimeException(String message) {
        super(message);
    }

    public SafeRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public SafeRuntimeException(Throwable cause) {
        super(cause);
    }
}
