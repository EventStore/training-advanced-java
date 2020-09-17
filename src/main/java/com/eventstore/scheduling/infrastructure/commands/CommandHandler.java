package com.eventstore.scheduling.infrastructure.commands;

import com.eventstore.scheduling.eventsourcing.CommandMetadata;
import io.vavr.Tuple1;
import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import lombok.Getter;

import java.util.function.Consumer;

public class CommandHandler {
    @Getter
    private Map<Class<?>, Consumer<?>> handlers = HashMap.empty();

    protected <T> void register(Class<T> clazz, Consumer<Tuple2<T, CommandMetadata>> handler) {
        handlers = handlers.put(clazz, handler);
    }
}
