package ru.alepar.rpc.api.exception;

/**
 * This exception is thrown when struck by a problem in underlying transport level. <br/>
 * i.e. all occasional network problems <br/>
 * <br/>
 * When you get this exception you will probably want to retry rpc call for the sake of reliability
 */
public class TransportException extends RuntimeException {
    public TransportException() {
    }

    public TransportException(String message) {
        super(message);
    }

    public TransportException(String message, Throwable cause) {
        super(message, cause);
    }

    public TransportException(Throwable cause) {
        super(cause);
    }
}
