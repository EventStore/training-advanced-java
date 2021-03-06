package com.eventstore.scheduling.infrastructure.eventstore;

import com.eventstore.dbclient.*;
import com.eventstore.scheduling.eventsourcing.CommandEnvelope;
import com.eventstore.scheduling.eventsourcing.CommandMetadata;
import com.eventstore.scheduling.eventsourcing.EventStore;
import com.eventstore.scheduling.eventsourcing.SnapshotEnvelope;
import io.vavr.collection.List;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.SneakyThrows;
import lombok.val;

import java.util.UUID;

public class EsEventStore implements EventStore {
    private final Streams client;
    private final EventStoreSerde serde = new EventStoreSerde();
    private final String tenantPrefix;

    public EsEventStore(Streams client, String tenantPrefix) {
        this.client = client;
        this.tenantPrefix = tenantPrefix;
    }

    private String getPrefixedStreamId(String streamId) {
        return "[" + tenantPrefix + "]" + streamId;
    }

    @SneakyThrows
    @Override
    public void appendCommand(String streamId, Object command, CommandMetadata metadata) {
        val proposedCommand = serde.serializeCommand(command, metadata);

        client.appendStream(getPrefixedStreamId(streamId)).expectedRevision(ExpectedRevision.ANY).addEvent(proposedCommand).execute().get();
    }

    @Override
    public List<CommandEnvelope> loadCommands(String streamId) {
        val future =
                client.readStream(getPrefixedStreamId(streamId))
                        .forward()
                        .fromRevision(StreamRevision.START.getValueUnsigned())
                        .notResolveLinks()
                        .execute(4096);

        try {
            return List.ofAll(future.get().getEvents()).map(serde::deserializeCommand);
        } catch (Exception e) {
            return List.empty();
        }
    }

    @SneakyThrows
    @Override
    public void appendEvents(String streamId, Long version, CommandMetadata metadata, List<Object> events) {
        val serialized = events.map((event) -> serde.serializeEvent(event, metadata)).iterator();
        if (version == -1L) {
            client.appendStream(getPrefixedStreamId(streamId)).expectedRevision(ExpectedRevision.NO_STREAM).addEvents(serialized).execute().get();
        } else {
            client.appendStream(getPrefixedStreamId(streamId)).expectedRevision(ExpectedRevision.expectedRevision(version)).addEvents(serialized).execute().get();
        }
    }

    @SneakyThrows
    @Override
    public void appendEvents(String streamId, CommandMetadata metadata, List<Object> events) {
        val serialized = events.map((event) -> serde.serializeEvent(event, metadata)).iterator();
        client.appendStream(getPrefixedStreamId(streamId)).expectedRevision(ExpectedRevision.ANY).addEvents(serialized).execute().get();
    }

    @Override
    public List<Object> loadEvents(String streamId, Option<Long> version) {
        val result =
                Try.of(() -> client
                        .readStream(getPrefixedStreamId(streamId)).forward().fromRevision(version.map(StreamRevision::new).getOrElse(StreamRevision.START).getValueUnsigned()).notResolveLinks().execute(4096)
                        .get()).map(ReadResult::getEvents).map(List::ofAll).getOrElse(List.empty());

        return result.map(serde::deserializeEvent);
    }

    @Override
    public Option<Long> getLastVersion(String streamId) {
        return
                Try.of(() -> client
                        .readStream(getPrefixedStreamId(streamId)).backward().fromRevision(StreamRevision.END.getValueUnsigned()).notResolveLinks().execute(1)
                        .get()).map(ReadResult::getEvents).map(List::ofAll).getOrElse(List.empty()).headOption().map(event -> event.getEvent().getStreamRevision().getValueUnsigned());
    }

    @SneakyThrows
    @Override
    public void appendSnapshot(String streamId, Long version, Object snapshot, CommandMetadata metadata) {

        val proposed = serde.serializeSnapshot(snapshot, version, metadata);

        client.appendStream(getPrefixedStreamId("snapshot-" + streamId)).expectedRevision(ExpectedRevision.ANY).addEvent(proposed).execute().get();
    }

    @Override
    public SnapshotEnvelope loadSnapshot(String streamId) {
        List<ResolvedEvent> results = Try.of(() -> client
                .readStream(getPrefixedStreamId("snapshot-" + streamId)).backward().fromRevision(StreamRevision.END.getValueUnsigned()).notResolveLinks().execute(1)
                .get()).map(ReadResult::getEvents).map(List::ofAll).getOrElse(List.empty());

        if (results.isEmpty()) {
            return null;
        } else {
            return serde.deserializeSnapshot(results.head());
        }
    }

    @SneakyThrows
    @Override
    public void truncateStream(String streamId, Long version) {
        client
                .appendStream(
                        "$$" + getPrefixedStreamId(streamId)).expectedRevision(ExpectedRevision.ANY).addEvents(
                        List.of(
                                new EventData(
                                        UUID.randomUUID(),
                                        "$metadata",
                                        "application/json",
                                        ("{\"$tb\":" + version + "}").getBytes(),
                                        "{}".getBytes()))
                                .iterator())
                .execute()
                .get();
    }
}
