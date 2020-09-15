package com.eventstore.scheduling.application.messagehandlers;

import com.eventstore.scheduling.application.eventsourcing.*;
import com.eventstore.scheduling.domain.readmodel.archivabledays.ArchivableDaysRepository;
import com.eventstore.scheduling.domain.service.IdGenerator;
import com.eventstore.scheduling.domain.writemodel.doctorday.DayId;
import com.eventstore.scheduling.domain.writemodel.doctorday.DoctorDayId;
import com.eventstore.scheduling.domain.writemodel.doctorday.command.Archive;
import com.eventstore.scheduling.domain.writemodel.doctorday.event.CalendarDayStarted;
import com.eventstore.scheduling.domain.writemodel.doctorday.event.DayScheduleArchived;
import com.eventstore.scheduling.domain.writemodel.doctorday.event.DayScheduled;
import io.vavr.collection.List;
import io.vavr.control.Option;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;

import java.time.Instant;
import java.time.Period;
import java.util.UUID;

import static io.vavr.API.None;

public class DayArchiverProcessManager implements MessageHandler<EventMetadata> {
  private final ColdStorage coldStorage;
  private final EventStoreClient<EventMetadata> eventStoreClient;
  private final ArchivableDaysRepository repository;
  private final CommandStore commandStore;
  private final Period archiveThreshold;
  private final IdGenerator idGenerator;

  public DayArchiverProcessManager(
      ColdStorage coldStorage,
      EventStoreClient<EventMetadata> eventStoreClient,
      ArchivableDaysRepository repository,
      CommandStore commandStore,
      IdGenerator idGenerator,
      Period archiveThreshold) {
    this.coldStorage = coldStorage;
    this.eventStoreClient = eventStoreClient;
    this.repository = repository;
    this.commandStore = commandStore;
    this.idGenerator = idGenerator;
    this.archiveThreshold = archiveThreshold;
  }

  @Override
  public void handle(
      Object message,
      EventMetadata metadata,
      UUID messageId,
      Instant occurredAt,
      Version position,
      Option<Version> streamPosition) {
    if (message instanceof DayScheduleArchived) {
      @NonNull DayId dayId = ((DayScheduleArchived) message).getDayId();
      archiveAndPurge(dayId);
      repository.remove(dayId);
    }
    if (message instanceof DayScheduled) {
      val dayScheduled = ((DayScheduled) message);
      repository.add(dayScheduled.getDate(), dayScheduled.getDayId());
    }
    if (message instanceof CalendarDayStarted) {
      val date = ((CalendarDayStarted) message).getDate();
      val archivableDays = repository.findAll(date.minus(archiveThreshold));
      archivableDays.forEach(dayid -> sendArchiveCommand(dayid, metadata));
    }
  }

  private void sendArchiveCommand(DayId dayId, EventMetadata metadata) {
    commandStore.send(
        new Archive(),
        new CommandMetadata(
            metadata.getCorrelationId(), CausationId.create(idGenerator), new DoctorDayId(dayId)));
  }

  @SneakyThrows
  private void archiveAndPurge(DayId dayId) {
    DoctorDayId aggregateId = new DoctorDayId(dayId);
    List<MessageEnvelope<EventMetadata>> events =
        eventStoreClient.readFromStream(aggregateId.toString(), None());
    events
        .reverse()
        .headOption()
        .forEach(
            (lastEvent) -> {
              coldStorage.saveAll(aggregateId, events);
              eventStoreClient.truncateStream(aggregateId.toString(), lastEvent.getVersion());
            });
  }
}
