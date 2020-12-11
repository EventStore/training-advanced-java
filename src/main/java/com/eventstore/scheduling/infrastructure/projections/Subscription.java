package com.eventstore.scheduling.infrastructure.projections;

import com.eventstore.scheduling.eventsourcing.CommandMetadata;
import io.vavr.Tuple2;

public interface Subscription {
    void project(Tuple2<Object, CommandMetadata> tuple);
}
