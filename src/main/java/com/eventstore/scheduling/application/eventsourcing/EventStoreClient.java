package com.eventstore.scheduling.application.eventsourcing;

import com.eventstore.dbclient.Subscription;
import io.vavr.collection.List;
import io.vavr.control.Option;

import java.util.concurrent.CompletableFuture;

public interface EventStoreClient<M> {
  Version createNewStream(String streamId, List<?> eventsToWrite, M metadata);

  Version appendToStream(String streamId, List<?> eventsToWrite, M metadata, Version version);

  Version appendToStream(String streamId, List<?> events, M metadata);

  List<MessageEnvelope<M>> readFromStream(String streamId, Option<Version> fromVersion);

  Option<MessageEnvelope<M>> readLastFromStream(String streamId);

  void truncateStream(String streamId, Version beforeVersion);

  void deleteStream(String streamId, Version version);

  CompletableFuture<Subscription> subscribeToAStream(
      String streamId, Option<Checkpoint> checkpoint, MessageHandler<M> handler);
}
