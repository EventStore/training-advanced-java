package com.eventstore.scheduling.infrastructure.commands;

public class HandlerNotFound extends RuntimeException {
    private final Object command;

    public HandlerNotFound(Object command) {
        this.command = command;
    }

    @Override
    public String getMessage() {
        return "No handler found for command: %s".formatted(command.getClass().getName());
    }
}
