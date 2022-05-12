package com.eventstore.scheduling;

import com.eventstore.dbclient.*;
import com.eventstore.scheduling.application.AvailableSlotsProjection;
import com.eventstore.scheduling.application.DayArchiverProcessManager;
import com.eventstore.scheduling.application.OverbookingProcessManager;
import com.eventstore.scheduling.domain.readmodel.availableslots.AvailableSlotsRepository;
import com.eventstore.scheduling.domain.service.Handlers;
import com.eventstore.scheduling.domain.service.RandomIdGenerator;
import com.eventstore.scheduling.eventsourcing.AggregateStore;
import com.eventstore.scheduling.eventsourcing.EventStore;
import com.eventstore.scheduling.infrastructure.commands.CommandHandlerMap;
import com.eventstore.scheduling.infrastructure.commands.Dispatcher;
import com.eventstore.scheduling.infrastructure.eventstore.*;
import com.eventstore.scheduling.infrastructure.inmemory.InMemoryColdStorage;
import com.eventstore.scheduling.infrastructure.mongodb.MongoArchivableDaysRepository;
import com.eventstore.scheduling.infrastructure.mongodb.MongoAvailableSlotsRepository;
import com.eventstore.scheduling.infrastructure.projections.DbProjector;
import com.eventstore.scheduling.infrastructure.projections.SubscriptionManager;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import io.vavr.collection.List;
import lombok.val;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;

import java.time.Period;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync
public class AppConfig {

    private final Dispatcher dispatcher;
    private final EsCommandStore commandStore;
    private final SubscriptionManager subscriptionManager;

    @Bean
    public Dispatcher dispatcher() {
        return dispatcher;
    }

    private final MongoAvailableSlotsRepository availableSlotsRepository;

    @Bean
    public AvailableSlotsRepository availableSlotsRepository() {
        return availableSlotsRepository;
    }

    private final EventStore eventStore;

    @Bean
    public EventStore eventStore() {
        return eventStore;
    }


    public AppConfig() {

        EventStoreDBClientSettings setts = EventStoreDBConnectionString.parseOrThrow("esdb://localhost:2113?tls=false");
        EventStoreDBClient client = EventStoreDBClient.create(setts);

        String tenantPrefix = "prod";

        eventStore = new EsEventStore(client, tenantPrefix);

        AggregateStore aggregateStore = new EsAggregateStore(eventStore, 1);
        RandomIdGenerator idGenerator = new RandomIdGenerator();
        dispatcher = new Dispatcher(new CommandHandlerMap(new Handlers(new EsDayRepository(aggregateStore), idGenerator)));

        MongoDatabase projectionsDb = MongoClients.create("mongodb://localhost").getDatabase("prod_projections");
        availableSlotsRepository = new MongoAvailableSlotsRepository(projectionsDb);
        val archivableDaysRepository = new MongoArchivableDaysRepository(projectionsDb);

        commandStore = new EsCommandStore(eventStore, client, dispatcher, tenantPrefix, new EsCommandSerde());

        val dayArchiverProcessManager = new DayArchiverProcessManager(
                new InMemoryColdStorage(),
                eventStore,
                archivableDaysRepository,
                commandStore,
                idGenerator,
                Period.ofDays(1)
        );

        subscriptionManager = new SubscriptionManager(
                new EsCheckpointStore(client, "day-subscription"),
                "day-subscription",
                "$all",
                client,
                List.of(
                        new DbProjector(new AvailableSlotsProjection(availableSlotsRepository)),
                        new DbProjector(dayArchiverProcessManager)
                )
        );
        startSubscription();
    }


    @Bean(name = "fixedThreadPool")
    public Executor fixedThreadPool() {
        return Executors.newFixedThreadPool(2);
    }

    @Async("fixedThreadPool")
    public void startSubscription() {
        commandStore.start();
        subscriptionManager.start();
    }
}
