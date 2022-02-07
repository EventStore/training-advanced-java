package com.eventstore.scheduling.infrastructure.projections;

import com.eventstore.scheduling.eventsourcing.EventMetadata;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import lombok.Getter;
import lombok.val;

import java.util.function.Consumer;

public class EventHandler {
    @Getter
    private List<EventHandlerEnvelope<?>> handlers = List.empty();

    protected <T> void when(Class<T> clazz, Consumer<Tuple2<T, EventMetadata>> when) {
        handlers = handlers.append(new EventHandlerEnvelope<T>(clazz, when));
    }

    public void handle(Tuple2<Object, EventMetadata> tuple) {
        handlers.forEach(handler -> {
            val event = tuple._1;
            if (handler.type() == event.getClass()) {
                handler.handler().accept(new Tuple2(handler.type().cast(event), tuple._2));
            }
        });
    }

    public boolean canHandle(Tuple2<Object, EventMetadata> tuple) {
        val event = tuple._1;
        return handlers.exists(handler -> handler.type() == event.getClass());
    }
}


