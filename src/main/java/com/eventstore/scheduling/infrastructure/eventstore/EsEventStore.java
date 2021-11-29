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
    private final EventStoreDBClient client;
    private final EventStoreSerde serde = new EventStoreSerde();
    private final String tenantPrefix;

    public EsEventStore(EventStoreDBClient client, String tenantPrefix) {
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

        client.appendToStream(getPrefixedStreamId(streamId), proposedCommand).get();
    }

    @Override
    public List<CommandEnvelope> loadCommands(String streamId) {
        val future =
                client.readStream(getPrefixedStreamId(streamId));

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

        AppendToStreamOptions options = AppendToStreamOptions.get()
                .expectedRevision(version == -1L ? ExpectedRevision.NO_STREAM : ExpectedRevision.expectedRevision(version));

        client.appendToStream(getPrefixedStreamId(streamId), options, serialized).get();
        }

    @SneakyThrows
    @Override
    public void appendEvents(String streamId, CommandMetadata metadata, List<Object> events) {
        val serialized = events.map((event) -> serde.serializeEvent(event, metadata)).iterator();

        client.appendToStream(getPrefixedStreamId(streamId), serialized).get();
    }

    @Override
    public List<Object> loadEvents(String streamId, Option<Long> version) {
        ReadStreamOptions options = ReadStreamOptions.get()
                .fromRevision(version.map(StreamRevision::new).getOrElse(StreamRevision.START));

        val result =
                Try.of(() -> client
                        .readStream(getPrefixedStreamId(streamId), options)
                        .get()).map(ReadResult::getEvents).map(List::ofAll).getOrElse(List.empty());

        return result.map(serde::deserializeEvent);
    }

    @Override
    public Option<Long> getLastVersion(String streamId) {
        ReadStreamOptions options = ReadStreamOptions.get()
                .fromEnd()
                .backwards();

        return
                Try.of(() -> client
                        .readStream(getPrefixedStreamId(streamId), 1, options).get()
                ).map(ReadResult::getEvents).map(List::ofAll).getOrElse(List.empty()).headOption().map(event -> event.getEvent().getStreamRevision().getValueUnsigned());
    }

    @SneakyThrows
    @Override
    public void appendSnapshot(String streamId, Long version, Object snapshot, CommandMetadata metadata) {
        // Serialize the snapshot
        // Get snapshot stream name
        // Append snapshot to stream
    }

    @Override
    public SnapshotEnvelope loadSnapshot(String streamId) {
        ReadStreamOptions options = ReadStreamOptions.get()
                .fromEnd()
                .backwards();

        List<ResolvedEvent> results =
                Try.of(() -> client
                    .readStream(getPrefixedStreamId("snapshot-" + streamId), 1, options).get()
                ).map(ReadResult::getEvents).map(List::ofAll).getOrElse(List.empty());

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
                .appendToStream(
                        "$$" + getPrefixedStreamId(streamId),
                        List.of(
                                new EventData(
                                        UUID.randomUUID(),
                                        "$metadata",
                                        "application/json",
                                        ("{\"$tb\":" + version + "}").getBytes(),
                                        "{}".getBytes()))
                                .iterator())
                .get();
    }
}
