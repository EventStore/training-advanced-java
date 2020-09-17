package com.eventstore.scheduling.infrastructure.projections;

import com.eventstore.scheduling.domain.service.IdGenerator;
import com.eventstore.scheduling.domain.service.RandomIdGenerator;
import io.vavr.collection.List;
import lombok.Getter;
import lombok.val;

import java.util.function.Consumer;

public class EventHandler {
    @Getter
    private List<EventHandlerEnvelope> handlers = List.empty();

    protected <T> void when(Class<T> clazz, Consumer<T> when) {
        handlers = handlers.append(new EventHandlerEnvelope(clazz, (Consumer<Object>) when));
    }

    public void handle(Object event) {
        handlers.forEach(handler -> {
            if (handler.getType() == event.getClass()) {
                handler.getHandler().accept(event);
            }
        });
    }

    public boolean canHandle(Object event) {
        return handlers.exists(handler -> handler.getType() == event.getClass());
    }
}


