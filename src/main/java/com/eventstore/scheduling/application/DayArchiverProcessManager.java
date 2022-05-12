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

        when(DayScheduled.class, dayScheduled -> repository.add(dayScheduled._1.date(), dayScheduled._1.dayId()));

        when(DayScheduleArchived.class, dayScheduleArchived -> {
            // Get events from store
            // save to cold storage
            // Get the latest version from the store
            // Truncate stream upto last version
        });

        when(CalendarDayStarted.class, calendarDayStarted -> {
            val date = calendarDayStarted._1.date();
            val archivableDays = repository.findAll(date.minus(archiveThreshold));
            archivableDays.forEach(dayId -> sendArchiveCommand(dayId, calendarDayStarted._2.correlationId()));
        });
    }

    private void sendArchiveCommand(DayId dayId, CorrelationId correlationId) {
        commandStore.send(
                new ArchiveDaySchedule(dayId),
                new CommandMetadata(correlationId, CausationId.create(idGenerator)));
    }
}
