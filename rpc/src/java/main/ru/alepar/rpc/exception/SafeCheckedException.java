package ru.alepar.rpc.exception;

/**
 * subclasses of this exception will be passed to client as-is (i.e. without wrapping into RemoteException)
 * this implies that such exception classes must be available in client classpath
 */
public class SafeCheckedException extends Exception {

    public SafeCheckedException() {
    }

    public SafeCheckedException(String message) {
        super(message);
    }

    public SafeCheckedException(String message, Throwable cause) {
        super(message, cause);
    }

    public SafeCheckedException(Throwable cause) {
        super(cause);
    }
}
