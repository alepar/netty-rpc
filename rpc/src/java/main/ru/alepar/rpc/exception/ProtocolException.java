package ru.alepar.rpc.exception;

/**
 * This exception is thrown, when one of protocol constraints is violated (eg trying to pass non-serializable object through rpc). <br/>
 * If you see this exceptions thrown in your app - it means there's something wrong with your code
 */
public class ProtocolException extends RuntimeException {

    public ProtocolException() {
    }

    public ProtocolException(String message) {
        super(message);
    }

    public ProtocolException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProtocolException(Throwable cause) {
        super(cause);
    }
}
