package ru.alepar.bus.api;

public class Key<T extends Message> {

    private final Class<T> clazz;
    private final RequestId id;

    public Key(Class<T> clazz) {
        this(clazz, null);
    }

    private Key(Class<T> clazz, RequestId id) {
        this.clazz = clazz;
        this.id = id;
    }

    public Class<T> getClazz() {
        return clazz;
    }

    public Key<T> deriveRequest() {
        return new Key<T>(clazz, new RequestId());
    }

    public <M extends Message> Key<M> deriveResponse(Class<M> responseClass) {
        return new Key<M>(responseClass, id);
    }
}
