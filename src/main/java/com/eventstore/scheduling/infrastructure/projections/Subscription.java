package com.eventstore.scheduling.infrastructure.projections;

import com.eventstore.scheduling.eventsourcing.CommandMetadata;
import com.eventstore.scheduling.eventsourcing.EventMetadata;
import io.vavr.Tuple2;

public interface Subscription {
    void project(Tuple2<Object, EventMetadata> tuple);
}
