package com.eventstore.scheduling.infrastructure.eventstore;

import com.eventstore.dbclient.ProposedEvent;
import com.eventstore.dbclient.ResolvedEvent;
import com.eventstore.scheduling.eventsourcing.CommandEnvelope;
import com.eventstore.scheduling.eventsourcing.CommandMetadata;
import com.eventstore.scheduling.eventsourcing.SnapshotEnvelope;
import io.vavr.Tuple2;

public class EventStoreSerde {
    private EsEventSerde eventSerde = new EsEventSerde();
    private EsCommandSerde commandSerde = new EsCommandSerde();
    private EsSnapshotSerde snapshotSerde = new EsSnapshotSerde();
    public ProposedEvent serializeCommand(Object command, CommandMetadata metadata) {
        return commandSerde.serialize(command, metadata);
    }

    public CommandEnvelope deserializeCommand(ResolvedEvent command) {
        Tuple2<Object, CommandMetadata> result = commandSerde.deserialize(command);
        return new CommandEnvelope(result._1, result._2);
    }

    public ProposedEvent serializeEvent(Object event, CommandMetadata metadata) {
        return eventSerde.serialize(event, metadata).get();
    }

    public Object deserializeEvent(ResolvedEvent resolvedEvent) {
        return eventSerde.deserialize(resolvedEvent)._1;
    }

    public ProposedEvent serializeSnapshot(Object snapshot, Long version) {
        return snapshotSerde.serialize(snapshot, version);
    }

    public SnapshotEnvelope deserializeSnapshot(ResolvedEvent result) {
        return snapshotSerde.deserialize(result);
    }
}
