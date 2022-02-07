package com.eventstore.scheduling.infrastructure.eventstore;

import com.eventstore.dbclient.EventData;
import com.eventstore.dbclient.ResolvedEvent;
import com.eventstore.scheduling.eventsourcing.CommandEnvelope;
import com.eventstore.scheduling.eventsourcing.CommandMetadata;
import com.eventstore.scheduling.eventsourcing.SnapshotEnvelope;
import io.vavr.Tuple2;

public class EventStoreSerde {
    private final EsEventSerde eventSerde = new EsEventSerde();
    private final EsCommandSerde commandSerde = new EsCommandSerde();
    private final EsSnapshotSerde snapshotSerde = new EsSnapshotSerde();
    public EventData serializeCommand(Object command, CommandMetadata metadata) {
        return commandSerde.serialize(command, metadata);
    }

    public CommandEnvelope deserializeCommand(ResolvedEvent command) {
        Tuple2<Object, CommandMetadata> result = commandSerde.deserialize(command);
        return new CommandEnvelope(result._1, result._2);
    }

    public EventData serializeEvent(Object event, CommandMetadata metadata) {
        return eventSerde.serialize(event, metadata).get();
    }

    public Object deserializeEvent(ResolvedEvent resolvedEvent) {
        return eventSerde.deserialize(resolvedEvent)._1;
    }

    public EventData serializeSnapshot(Object snapshot, Long version, CommandMetadata metadata) {
        return snapshotSerde.serialize(snapshot, version, metadata);
    }

    public SnapshotEnvelope deserializeSnapshot(ResolvedEvent result) {
        return snapshotSerde.deserialize(result);
    }
}
