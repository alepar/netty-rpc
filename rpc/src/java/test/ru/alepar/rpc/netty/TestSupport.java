package ru.alepar.rpc.netty;

import java.util.concurrent.TimeoutException;

public class TestSupport {

    public static void executeWithTimeout(Runnable r) throws Exception {
        executeWithTimeout(r, 200L);
    }

    public static void executeWithTimeout(Runnable r, long timeout) throws Exception {
        ErrorCatchingRunnable catcher = new ErrorCatchingRunnable(r);
        Thread thread = new Thread(catcher);
        thread.start();
        thread.join(timeout);
        if(thread.isAlive()) {
            throw new TimeoutException();
        }
        if(catcher.error != null) {
            if (catcher.error instanceof Error) {
                throw (Error) catcher.error;
            } else if(catcher.error instanceof Exception) {
                throw (Exception) catcher.error;
            } else {
                throw new RuntimeException("caught unknown throwable", catcher.error);
            }
        }
    }

    public static class ErrorCatchingRunnable implements Runnable {

        private final Runnable delegate;
        private Throwable error;

        public ErrorCatchingRunnable(Runnable delegate) {
            this.delegate = delegate;
        }

        @Override
        public void run() {
            try {
                delegate.run();
            } catch(Throwable e) {
                error = e;
            }
        }

        public Throwable getError() {
            return error;
        }
    }
}
