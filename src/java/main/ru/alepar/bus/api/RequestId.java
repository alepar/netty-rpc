package ru.alepar.bus.api;

import java.util.UUID;

public class RequestId {

    private final long id;

    public RequestId() {
        final UUID random = UUID.randomUUID();
        this.id = random.getLeastSignificantBits() ^ random.getMostSignificantBits();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RequestId requestId = (RequestId) o;

        if (id != requestId.id) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }

    @Override
    public String toString() {
        return "RequestId{" + Long.toString(id, 16) + '}';
    }
}
