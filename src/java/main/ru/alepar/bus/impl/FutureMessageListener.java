package ru.alepar.bus.impl;

import ru.alepar.bus.api.*;

import java.util.concurrent.*;

class FutureMessageListener<T extends Message> implements MessageListener<T>, Future<T> {
    
    private final CountDownLatch latch = new CountDownLatch(1);
    private T msg;
    
    @Override
    public void onMessage(ru.alepar.bus.api.Context<T> ctx) {
        msg = ctx.message();
        latch.countDown();
    }

    @Override
    public boolean cancel(boolean b) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return latch.getCount() == 0;
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        latch.await();
        return msg;
    }

    @Override
    public T get(long timeout, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
        latch.await(timeout, timeUnit);
        return msg;
    }
    
}
