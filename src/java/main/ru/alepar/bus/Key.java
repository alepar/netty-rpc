package ru.alepar.bus;

public class Key<T extends Message> {

    private final Class<T> clazz;

    public Key(Class<T> clazz) {
        this.clazz = clazz;
    }

    public Class<T> getClazz() {
        return clazz;
    }
}
