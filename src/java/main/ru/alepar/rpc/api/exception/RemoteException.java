package ru.alepar.rpc.api.exception;

import java.util.Arrays;

public class RemoteException extends RuntimeException {

    public RemoteException(Throwable cause) {
        super(impose(cause));
    }

    private static ImpostorException impose(Throwable t) {
        if (t == null) {
            return null;
        }
        return new ImpostorException(t, t.getCause());
    }

    private static class ImpostorException extends Exception {

        private final String srcClassName;

        private ImpostorException(Throwable src, Throwable cause) {
            super(src.getMessage(), impose(cause));
            this.setStackTrace(Arrays.copyOf(src.getStackTrace(), src.getStackTrace().length));
            this.srcClassName = src.getClass().getName();
        }

        @Override
        public String toString() {
            String s = srcClassName;
            String message = getLocalizedMessage();
            return (message != null) ? (s + ": " + message) : s;
        }
    }

}
