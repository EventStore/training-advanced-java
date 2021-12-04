package com.eventstore.scheduling.test;

import com.eventstore.scheduling.domain.service.IdGenerator;
import com.eventstore.scheduling.domain.service.RandomIdGenerator;
import com.eventstore.scheduling.eventsourcing.CausationId;
import com.eventstore.scheduling.eventsourcing.CommandMetadata;
import com.eventstore.scheduling.eventsourcing.CorrelationId;
import com.eventstore.scheduling.eventsourcing.EventMetadata;
import com.eventstore.scheduling.infrastructure.projections.EventHandler;
import io.vavr.Tuple2;
import io.vavr.collection.List;

import static io.vavr.API.None;
import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class HandlerTest {
    protected IdGenerator idGenerator = new RandomIdGenerator();

    protected abstract EventHandler handler();

    protected boolean enableAtLeastOnceMonkey() {
        return false;
    }

    protected boolean enableAtLeastOnceGorilla() {
        return false;
    }

    protected void given(List<Object> events) {
        events.zipWithIndex().forEach(event -> {
            EventMetadata eventMetadata = new EventMetadata(CorrelationId.create(idGenerator), CausationId.create(idGenerator), event._2.longValue(), None());
            handler().handle(new Tuple2(event._1, eventMetadata));

            if (enableAtLeastOnceMonkey()) {
                handler().handle(new Tuple2(event._1, eventMetadata));
            }
        });

        if (enableAtLeastOnceGorilla()) {
            events.zipWithIndex().dropRight(1).forEach(event -> {
                EventMetadata eventMetadata = new EventMetadata(CorrelationId.create(idGenerator), CausationId.create(idGenerator), event._2.longValue(), None());
                handler().handle(new Tuple2(event, eventMetadata));
            });
        }
    }

    protected void then(Object actual, Object expected) {
        assertEquals(expected, actual);
    }
}
