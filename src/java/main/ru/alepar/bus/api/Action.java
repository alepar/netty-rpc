package ru.alepar.bus.api;

import java.util.concurrent.ExecutionException;

public interface Action {

    <M extends Message> M waitForResponse(Class<M> responseClass) throws InterruptedException, ExecutionException;

}
