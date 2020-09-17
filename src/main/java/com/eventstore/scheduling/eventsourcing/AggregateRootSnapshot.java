package com.eventstore.scheduling.eventsourcing;

import io.vavr.Function0;
import lombok.Getter;
import lombok.Setter;

import java.util.function.Consumer;

public class AggregateRootSnapshot extends AggregateRoot {
    private Consumer<Object> snapshotLoad;
    private Function0<Object> snapshotGet;

    @Getter
    @Setter
    public Long snapshotVersion = -1L;

    protected <T> void registerSnapshot(Class<T> clazz, Consumer<T> load, Function0<T> get) {
        snapshotLoad = (Consumer<Object>) load;
        snapshotGet = (Function0<Object>) get;
    }

    public void loadSnapshot(Object snapshot, Long version) {
        snapshotLoad.accept(snapshot);
        setVersion(version);
        snapshotVersion = version;
    }

    public Object getSnapshot() {
        return snapshotGet.apply();
    }
}
