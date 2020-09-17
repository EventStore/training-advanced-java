package com.eventstore.scheduling.infrastructure.projections;

import com.eventstore.dbclient.*;
import com.eventstore.scheduling.infrastructure.eventstore.Checkpoint;
import com.eventstore.scheduling.infrastructure.eventstore.EsEventSerde;
import io.vavr.collection.List;
import lombok.SneakyThrows;
import lombok.val;

public class SubscriptionManager {
    private final CheckpointStore checkpointStore;
    private final String name;
    private final String streamName;
    private final StreamsClient client;
    private final List<Subscription> subscriptions;
    private final Boolean isAllStream;
    private final EsEventSerde serde = new EsEventSerde();

    public SubscriptionManager(CheckpointStore checkpointStore, String name, String streamName, StreamsClient client, List<Subscription> subscriptions) {
        this.checkpointStore = checkpointStore;
        this.name = name;
        this.streamName = streamName;
        this.client = client;
        this.subscriptions = subscriptions;
        this.isAllStream = streamName.equals("$all");
    }

    @SneakyThrows
    public void start() {
        val checkpoint = checkpointStore.getCheckpoint().map(c -> new StreamRevision(c.getValue())).getOrElse(StreamRevision.START);

        if (isAllStream) {
            client.subscribeToAll(new Position(checkpoint.getValueUnsigned(), checkpoint.getValueUnsigned()), false, new Listener()).get();
        } else {
            client.subscribeToStream(streamName, checkpoint, true, new Listener()).get();
        }
    }

    private void eventAppeared(ResolvedEvent event) {
        if (event.getEvent().getEventType().startsWith("doctorday")) {
            val deserialized = serde.deserialize(event);
            subscriptions.forEach(s -> s.project(deserialized._1));

            Checkpoint checkpoint;

            if (isAllStream) {
                checkpoint = new Checkpoint(event.getEvent().getPosition().getCommitUnsigned());
            } else {
                checkpoint = new Checkpoint(event.getEvent().getStreamRevision().getValueUnsigned());
            }

            checkpointStore.storeCheckpoint(checkpoint);
        }

    }

    private class Listener extends SubscriptionListener {
        @Override
        public void onEvent(com.eventstore.dbclient.Subscription subscription, ResolvedEvent event) {
            eventAppeared(event);
        }
    }
}
