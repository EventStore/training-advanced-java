package com.eventstore.scheduling.infrastructure.commands;


import com.eventstore.scheduling.eventsourcing.CommandMetadata;
import io.vavr.Tuple2;
import lombok.val;

import java.util.function.Consumer;

public class Dispatcher {
    private final CommandHandlerMap map;

    public Dispatcher(CommandHandlerMap map) {
        this.map = map;
    }

    public void dispatch(Object command, CommandMetadata metadata) {
        val handler = map.get(command);

        if (handler.isDefined()) {
            Consumer<Tuple2<Object, CommandMetadata>> consumer = (Consumer<Tuple2<Object, CommandMetadata>>) handler.get();
            consumer.accept(new Tuple2(command, metadata));
        } else {
            throw new HandlerNotFound(command);
        }
    }
}
