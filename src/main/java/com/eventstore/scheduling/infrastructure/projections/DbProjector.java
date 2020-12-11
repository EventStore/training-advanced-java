package com.eventstore.scheduling.infrastructure.projections;


import com.eventstore.scheduling.eventsourcing.CommandMetadata;
import io.vavr.Tuple2;

public class DbProjector implements Subscription {
    private final EventHandler projection;

    public DbProjector(EventHandler projection) {
        this.projection = projection;
    }

    @Override
    public void project(Tuple2<Object, CommandMetadata> tuple) {
        if (projection.canHandle(tuple)) {
            projection.handle(tuple);
        }
    }
}

