package com.eventstore.scheduling.eventsourcing;

import io.vavr.collection.List;
import io.vavr.control.Option;

public interface EventStore {
    void appendCommand(String streamId, Object command, CommandMetadata metadata);
    List<CommandEnvelope> loadCommands(String streamId);

    void appendEvents(String streamId, Long version, CommandMetadata metadata, List<Object> events);
    void appendEvents(String streamId, CommandMetadata metadata, List<Object> events);
    List<Object> loadEvents(String streamId, Option<Long> version);

    Option<Long> getLastVersion(String streamId);

    void appendSnapshot(String streamId, Long version, Object snapshot, CommandMetadata metadata);
    SnapshotEnvelope loadSnapshot(String streamId);

    void truncateStream(String streamId, Long version);
}