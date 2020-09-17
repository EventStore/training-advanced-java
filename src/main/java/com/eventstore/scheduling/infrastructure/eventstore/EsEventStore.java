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

import static com.eventstore.dbclient.Direction.Backward;
import static com.eventstore.dbclient.Direction.Forward;

public class EsEventStore implements EventStore {
    private final StreamsClient client;
    private final EventStoreSerde serde = new EventStoreSerde();
    private final String tenantPrefix;

    public EsEventStore(StreamsClient client, String tenantPrefix) {
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

        client.appendToStream(getPrefixedStreamId(streamId), SpecialStreamRevision.ANY, List.of(proposedCommand).asJava()).get();
    }

    @Override
    public List<CommandEnvelope> loadCommands(String streamId) {
        val future =
                client.readStream(
                        Forward,
                        getPrefixedStreamId(streamId),
                        StreamRevision.START, 4096, false);

        try {
            return List.ofAll(future.get().getEvents()).map(serde::deserializeCommand);
        } catch (Exception e) {
            return List.empty();
        }
    }

    @SneakyThrows
    @Override
    public void appendEvents(String streamId, Long version, CommandMetadata metadata, List<Object> events) {
        val serialized = events.map((event) -> serde.serializeEvent(event, metadata)).asJava();
        if (version == -1L) {
            client.appendToStream(getPrefixedStreamId(streamId), SpecialStreamRevision.NO_STREAM, serialized).get();
        } else {
            client.appendToStream(getPrefixedStreamId(streamId), ExpectedRevision.expectedRevision(version), serialized).get();
        }
    }

    @SneakyThrows
    @Override
    public void appendEvents(String streamId, CommandMetadata metadata, List<Object> events) {
        List<ProposedEvent> serialized = events.map((event) -> serde.serializeEvent(event, metadata));
        client.appendToStream(
                getPrefixedStreamId(streamId), SpecialStreamRevision.NO_STREAM, serialized.asJava()).get();
    }

    @Override
    public List<Object> loadEvents(String streamId, Option<Long> version) {
        val result =
                Try.of(() -> client
                        .readStream(Direction.Forward, getPrefixedStreamId(streamId), version.map(StreamRevision::new).getOrElse(StreamRevision.START), 4096, false)
                        .get()).map(ReadResult::getEvents).map(List::ofAll).getOrElse(List.empty());

        return result.map(serde::deserializeEvent);
    }

    @Override
    public Option<Long> getLastVersion(String streamId) {
        return
                Try.of(() -> client
                        .readStream(Direction.Backward, getPrefixedStreamId(streamId), StreamRevision.END, 1, false)
                        .get()).map(ReadResult::getEvents).map(List::ofAll).getOrElse(List.empty()).headOption().map(event -> event.getEvent().getStreamRevision().getValueUnsigned());
    }

    @SneakyThrows
    @Override
    public void appendSnapshot(String streamId, Long version, Object snapshot) {
        // Serialize the snapshot
        // Get snapshot stream name
        // Append snapshot to stream
    }

    @Override
    public SnapshotEnvelope loadSnapshot(String streamId) {
        List<ResolvedEvent> results = Try.of(() -> client
                .readStream(Backward, getPrefixedStreamId("snapshot-" + streamId), StreamRevision.END, 1, false)
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
                .appendToStream(
                        "$$" + getPrefixedStreamId(streamId),
                        SpecialStreamRevision.ANY,
                        List.of(
                                new ProposedEvent(
                                        UUID.randomUUID(),
                                        "$metadata",
                                        "application/json",
                                        ("{\"$tb\":" + version + "}").getBytes(),
                                        "{}".getBytes()))
                                .asJava())
                .get();
    }
}
