package com.eventstore.scheduling.infrastructure.eventstore;

import com.eventstore.dbclient.ResolvedEvent;
import com.eventstore.dbclient.StreamRevision;
import com.eventstore.dbclient.StreamsClient;
import com.eventstore.dbclient.SubscriptionListener;
import com.eventstore.scheduling.eventsourcing.CommandMetadata;
import com.eventstore.scheduling.eventsourcing.CommandStore;
import com.eventstore.scheduling.eventsourcing.EventStore;
import com.eventstore.scheduling.infrastructure.commands.Dispatcher;
import lombok.SneakyThrows;
import lombok.val;

public class EsCommandStore implements CommandStore {

    private final EventStore eventStore;
    private final StreamsClient streamsClient;
    private final Dispatcher dispatcher;
    private final String tenantPrefix;
    private final EsCommandSerde commandSerde;

    private final String streamName = "async-command-handler";

    public EsCommandStore(EventStore eventStore, StreamsClient streamsClient, Dispatcher dispatcher, String tenantPrefix, EsCommandSerde commandSerde) {
        this.eventStore = eventStore;
        this.streamsClient = streamsClient;
        this.dispatcher = dispatcher;
        this.tenantPrefix = tenantPrefix;
        this.commandSerde = commandSerde;
    }

    @Override
    public void send(Object command, CommandMetadata metadata) {
        eventStore.appendCommand(streamName, command, metadata);
    }

    @SneakyThrows
    @Override
    public void start() {
        streamsClient.subscribeToStream("[" + tenantPrefix + "]" + streamName, StreamRevision.END, false, new Listener()).get();
    }

    private void eventAppeared(ResolvedEvent event) {
        if (event.getEvent().getEventType().startsWith("$")) {
            return;
        }

        val deserialized = commandSerde.deserialize(event);

        if (dispatcher != null) {
            dispatcher.dispatch(deserialized._1, deserialized._2);
        }
    }

    private class Listener extends SubscriptionListener {
        @Override
        public void onEvent(com.eventstore.dbclient.Subscription subscription, ResolvedEvent event) {
            eventAppeared(event);
        }
    }
}
