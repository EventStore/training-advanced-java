package com.eventstore.scheduling.infrastructure.eventstore;

import com.eventstore.dbclient.*;
import com.eventstore.scheduling.infrastructure.projections.CheckpointStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.collection.List;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.SneakyThrows;
import lombok.val;

import java.util.UUID;

import static com.eventstore.dbclient.Direction.Forward;

public class EsCheckpointStore implements CheckpointStore {
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final Streams client;
  private final String subscriptionName;

  public EsCheckpointStore(Streams client, String subscriptionName) {
    this.client = client;
    this.subscriptionName = "checkpoint-" + subscriptionName;
  }

  @Override
  public Option<Checkpoint> getCheckpoint() {
    return Try.of(() -> client
            .readStream(subscriptionName).fromRevision(StreamRevision.END.getValueUnsigned()).backward().execute(1)
            .get()).map(ReadResult::getEvents).map(List::ofAll).getOrElse(List.empty()).headOption().map(this::deserialize);
  }

  @SneakyThrows
  private Checkpoint deserialize(ResolvedEvent resolvedEvent) {
    RecordedEvent event = resolvedEvent.getEvent();
    val data = objectMapper.readTree(event.getEventData());
    return new Checkpoint(data.get("checkpoint").asLong());
  }

  @SneakyThrows
  @Override
  public void storeCheckpoint(Checkpoint checkpoint) {
    EventData proposed  = serialize(checkpoint);

    client.appendStream(subscriptionName).expectedRevision(ExpectedRevision.ANY).addEvent(proposed).execute().get();
  }

  @SneakyThrows
  private EventData serialize(Checkpoint checkpoint) {
    val node = objectMapper.createObjectNode();
    node.put("checkpoint", checkpoint.getValue());
    return new EventData(
            UUID.randomUUID(),
            "$checkpoint",
            "application/json",
            objectMapper.writeValueAsBytes(node),
            objectMapper.writeValueAsBytes(objectMapper.createObjectNode()));
  }
}
