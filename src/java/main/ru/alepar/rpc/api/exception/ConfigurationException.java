package ru.alepar.rpc.api.exception;

/**
 * This exception is thrown during configuration, when one of rules is broken. <br/>
 * If you see this exceptions thrown in your app - it means there's something wrong with your code <br/>
 */
public class ConfigurationException extends RuntimeException {

    public ConfigurationException() {
    }

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConfigurationException(Throwable cause) {
        super(cause);
    }
}
