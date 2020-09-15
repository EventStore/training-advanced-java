package com.eventstore.scheduling;

import com.eventstore.dbclient.StreamsClient;
import com.eventstore.dbclient.Timeouts;
import com.eventstore.dbclient.UserCredentials;
import com.eventstore.scheduling.application.eventsourcing.*;
import com.eventstore.scheduling.application.messagehandlers.AvailableSlotsProjector;
import com.eventstore.scheduling.application.messagehandlers.DayArchiverProcessManager;
import com.eventstore.scheduling.application.messagehandlers.OverbookingProcessManager;
import com.eventstore.scheduling.domain.readmodel.availableslots.AvailableSlotsRepository;
import com.eventstore.scheduling.domain.service.IdGenerator;
import com.eventstore.scheduling.domain.service.RandomIdGenerator;
import com.eventstore.scheduling.domain.writemodel.doctorday.DoctorDayLogic;
import com.eventstore.scheduling.domain.writemodel.doctorday.command.Command;
import com.eventstore.scheduling.domain.writemodel.doctorday.error.Error;
import com.eventstore.scheduling.domain.writemodel.doctorday.event.Event;
import com.eventstore.scheduling.domain.writemodel.doctorday.state.State;
import com.eventstore.scheduling.infrastructure.eventstore.*;
import com.eventstore.scheduling.infrastructure.inmemory.InMemoryColdStorage;
import com.eventstore.scheduling.infrastructure.mongodb.MongoArchivableDaysRepository;
import com.eventstore.scheduling.infrastructure.mongodb.MongoAvailableSlotsRepository;
import com.eventstore.scheduling.infrastructure.mongodb.MongoBookedSlotsRepository;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.val;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import javax.net.ssl.SSLException;
import java.time.Period;

import static io.vavr.API.Some;

@Configuration
@EnableAsync
public class AppConfig {
  private final AvailableSlotsRepository availableSlotsRepository;

  private final CommandHandler<Command, Event, Error, State> commandHandler;
  private final EventStoreClient<EventMetadata> eventClient;

  public AppConfig() throws SSLException {
    String tenantPrefix = "prod";
    String categoryStream = "$ce-doctorday";

    StreamsClient client =
        new StreamsClient(
            "localhost",
            2113,
            new UserCredentials("admin", "changeit"),
            Timeouts.DEFAULT,
            GrpcSslContexts.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build());

    MongoDatabase projectionsDb =
        MongoClients.create("mongodb://localhost").getDatabase("prod_projections");

    IdGenerator idGenerator = new RandomIdGenerator();

    availableSlotsRepository = new MongoAvailableSlotsRepository(projectionsDb);
    val archivableDaysRepository = new MongoArchivableDaysRepository(projectionsDb);

    eventClient = new EsEventStoreClient<>(client, new EsEventSerde(), tenantPrefix);

    val aggregateStore = new EsAggregateStore(eventClient);
    val snapshotStore =
        new EsSnapshotStore(new EsEventStoreClient<>(client, new EsSnapshotSerde(), tenantPrefix));
    val checkpointStore =
        new EsCheckpointStore(
            new EsEventStoreClient<>(client, new EsCheckpointSerde(), tenantPrefix));
    val commandStore =
            new EsCommandStore(
                    "commands-doctorday",
                    new EsEventStoreClient<>(client, new EsCommandSerde(), tenantPrefix));

    val eventStreamSubscriptionFactory =
        new PersistentSubscriptionFactory<>(eventClient, checkpointStore);

    eventStreamSubscriptionFactory.subscribeToAStream(
        new SubscriptionId("available_slots"),
        categoryStream,
        new AvailableSlotsProjector(availableSlotsRepository));

    commandHandler =
        new CommandHandler<>(
            new DoctorDayLogic(idGenerator), aggregateStore, snapshotStore, Some(3L));

    val overbookingProcessManager =
        new OverbookingProcessManager(
            new MongoBookedSlotsRepository(projectionsDb), commandStore, 1, idGenerator);

    val dayArchiverProcessManager =
            new DayArchiverProcessManager(
                    new InMemoryColdStorage(),
                    eventClient,
                    archivableDaysRepository,
                    commandStore,
                    idGenerator,
                    Period.ofDays(1));

    eventStreamSubscriptionFactory.subscribeToAStream(
        new SubscriptionId("overbooking_process_manager"),
        categoryStream,
        overbookingProcessManager);

    eventStreamSubscriptionFactory.subscribeToAStream(
        new SubscriptionId("day_archiver_process_manager"),
        categoryStream,
        dayArchiverProcessManager);

    commandStore.subscribe(commandHandler, checkpointStore);
  }

  @Bean
  public AvailableSlotsRepository availableSlotsRepository() {
    return availableSlotsRepository;
  }

  @Bean
  public EventStoreClient<EventMetadata> eventStoreEventClient() {
    return eventClient;
  }

  @Bean
  public CommandHandler<Command, Event, Error, State> commandHandler() {
    return commandHandler;
  }
}
