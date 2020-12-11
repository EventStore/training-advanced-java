package com.eventstore.scheduling.application;

import com.eventstore.scheduling.domain.doctorday.DayId;
import com.eventstore.scheduling.domain.doctorday.DoctorDayId;
import com.eventstore.scheduling.domain.doctorday.command.ArchiveDaySchedule;
import com.eventstore.scheduling.domain.doctorday.event.CalendarDayStarted;
import com.eventstore.scheduling.domain.doctorday.event.DayScheduleArchived;
import com.eventstore.scheduling.domain.doctorday.event.DayScheduled;
import com.eventstore.scheduling.domain.readmodel.archivabledays.ArchivableDaysRepository;
import com.eventstore.scheduling.domain.service.IdGenerator;
import com.eventstore.scheduling.eventsourcing.*;
import com.eventstore.scheduling.infrastructure.projections.EventHandler;
import io.vavr.collection.List;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;

import java.time.Period;

import static io.vavr.API.None;

public class DayArchiverProcessManager extends EventHandler {
    private final ColdStorage coldStorage;
    private final EventStore eventStoreClient;
    private final CommandStore commandStore;
    private final IdGenerator idGenerator;

    public DayArchiverProcessManager(
            ColdStorage coldStorage,
            EventStore eventStoreClient,
            ArchivableDaysRepository repository,
            CommandStore commandStore,
            IdGenerator idGenerator,
            Period archiveThreshold) {
        this.coldStorage = coldStorage;
        this.eventStoreClient = eventStoreClient;
        this.commandStore = commandStore;
        this.idGenerator = idGenerator;

        when(DayScheduled.class, dayScheduled -> repository.add(dayScheduled._1.getDate(), dayScheduled._1.getDayId()));

        when(DayScheduleArchived.class, dayScheduleArchived -> {
            @NonNull DayId dayId = dayScheduleArchived._1.getDayId();
            archiveAndPurge(dayId);
            repository.remove(dayId);
        });

        when(CalendarDayStarted.class, calendarDayStarted -> {
            val date = calendarDayStarted._1.getDate();
            val archivableDays = repository.findAll(date.minus(archiveThreshold));
            archivableDays.forEach(dayId -> sendArchiveCommand(dayId, calendarDayStarted._2.getCorrelationId()));
        });
    }

    private void sendArchiveCommand(DayId dayId, CorrelationId correlationId) {
        commandStore.send(
                new ArchiveDaySchedule(dayId),
                new CommandMetadata(correlationId, CausationId.create(idGenerator)));
    }

    @SneakyThrows
    private void archiveAndPurge(DayId dayId) {
        DoctorDayId aggregateId = new DoctorDayId(dayId);

        eventStoreClient.getLastVersion(aggregateId.toString()).map(lastVersion -> {
            List<Object> events =
                    eventStoreClient.loadEvents(aggregateId.toString(), None());
            events
                    .reverse()
                    .headOption()
                    .forEach(
                            (lastEvent) -> {
                                coldStorage.saveAll(aggregateId.toString(), events);
                                eventStoreClient.truncateStream(aggregateId.toString(), lastVersion);
                            });

            return null;
        });
    }
}
