package com.eventstore.scheduling.infrastructure.eventstore;

import com.eventstore.dbclient.*;
import com.eventstore.scheduling.application.eventsourcing.*;
import io.vavr.collection.List;
import io.vavr.control.Option;
import lombok.SneakyThrows;
import lombok.val;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.eventstore.dbclient.Direction.Forward;
import static io.vavr.API.None;

public class EsEventStoreClient<M> implements EventStoreClient<M> {
  private final StreamsClient client;
  private final EsSerde<M> esSerde;
  private final String tenantPrefix;

  public EsEventStoreClient(StreamsClient client, EsSerde<M> esSerde, String tenantPrefix) {
    super();
    this.client = client;
    this.esSerde = esSerde;
    this.tenantPrefix = tenantPrefix;
  }

  private String getPrefixedStreamId(String streamId) {
    return "[" + tenantPrefix + "]" + streamId;
  }

  @SneakyThrows
  @Override
  public Version createNewStream(String streamId, List<?> events, M metadata) {
    List<ProposedEvent> serialized = events.map((event) -> esSerde.serialize(event, metadata).get());
    val result =
        client.appendToStream(
            getPrefixedStreamId(streamId), SpecialStreamRevision.NO_STREAM, serialized.asJava());

    return new Version(result.get().getNextExpectedRevision().getValueUnsigned());
  }

  @SneakyThrows()
  @Override
  public List<MessageEnvelope<M>> readFromStream(String streamId, Option<Version> fromVersion) {
    val future =
        client.readStream(
            Forward,
            getPrefixedStreamId(streamId),
            fromVersion
                .map(version -> new StreamRevision(version.getValue()))
                .getOrElse(StreamRevision.START),
            4096,
            false);
    List<MessageEnvelope<M>> events;
    try {
      events = List.ofAll(future.get().getEvents()).map((event) -> esSerde.deserialize(event).get());
    } catch (ExecutionException e) {
      if (e.getCause() instanceof StreamNotFoundException) {
        throw e.getCause();
      }
      throw e;
    }

    return events;
  }

  @SneakyThrows
  @Override
  public Option<MessageEnvelope<M>> readLastFromStream(String streamId) {
    try {
      val result =
          client
              .readStream(
                  Direction.Backward, getPrefixedStreamId(streamId), StreamRevision.END, 1, false)
              .get()
              .getEvents();

      return List.ofAll(result).headOption().map((event) -> esSerde.deserialize(event).get());
    } catch (ExecutionException e) {
      if (!(e.getCause() instanceof StreamNotFoundException)) {
        throw e;
      }
    }
    return None();
  }

  @SneakyThrows
  @Override
  public Version appendToStream(
      String streamId, List<?> events, M metadata, Version expectedVersion) {
    val serialized = events.map((e) -> esSerde.serialize(e, metadata).get()).asJava();
    StreamRevision expectedRevision = new StreamRevision(expectedVersion.getValue());
    val aresult = client.appendToStream(getPrefixedStreamId(streamId), expectedRevision, serialized);

    return new Version(result.get().getNextExpectedRevision().getValueUnsigned());
  }

  @SneakyThrows
  @Override
  public Version appendToStream(String streamId, List<?> events, M metadata) {
    val serialized = events.map((e) -> esSerde.serialize(e, metadata).get()).asJava();
    val result =
        client.appendToStream(getPrefixedStreamId(streamId), SpecialStreamRevision.ANY, serialized);

    return new Version(result.get().getNextExpectedRevision().getValueUnsigned());
  }

  @SneakyThrows
  @Override
  public void truncateStream(String streamId, Version beforeVersion) {
    client
        .appendToStream(
            "$$" + getPrefixedStreamId(streamId),
            SpecialStreamRevision.ANY,
            List.of(
                    new ProposedEvent(
                        UUID.randomUUID(),
                        "$metadata",
                        "application/json",
                        ("{\"$tb\":" + beforeVersion.getValue() + "}").getBytes(),
                        "{}".getBytes()))
                .asJava())
        .get();
  }

  @SneakyThrows
  @Override
  public void deleteStream(String streamId, Version expectedVersion) {
    client
        .softDelete(getPrefixedStreamId(streamId), new StreamRevision(expectedVersion.getValue()))
        .get();
  }

  @Override
  public CompletableFuture<Subscription> subscribeToAStream(
      String streamId, Option<Checkpoint> checkpoint, MessageHandler<M> handler) {
    String prefixedStreamId = null;
    if (streamId.contains("$ce-")) {
      prefixedStreamId = streamId.replace("$ce-", "$ce-" + "[" + tenantPrefix + "]");
    } else {
      prefixedStreamId = getPrefixedStreamId(streamId);
    }

    return client.subscribeToStream(
        prefixedStreamId,
        checkpoint
            .map((version) -> new StreamRevision(version.getValue()))
            .getOrElse(StreamRevision.START),
        true,
        new SubscriptionListener() {
          @Override
          public void onEvent(Subscription subscription, ResolvedEvent event) {
            handler.handle(esSerde.deserialize(event).get());
          }
        });
  }
}
