package ru.alepar.rpc.exception;

/**
 *  This exception is thrown when struck by a problem in underlying transport level  <br/>
 *  i.e. all occasional network problems
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
