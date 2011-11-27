package ru.alepar.rpc.server;

import ru.alepar.rpc.api.Remote;

public interface ServerProvider<T> {
    
    T provideFor(Remote remote);
    
}
