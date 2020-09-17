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

        when(DayScheduled.class, dayScheduled -> repository.add(dayScheduled.getDate(), dayScheduled.getDayId()));

        when(DayScheduleArchived.class, dayScheduleArchived -> {
            // Get events from store
            // save to cold storage
            // Get the latest version from the store
            // Truncate stream upto last version
        });

        when(CalendarDayStarted.class, calendarDayStarted -> {
            val date = calendarDayStarted.getDate();
            val archivableDays = repository.findAll(date.minus(archiveThreshold));
            archivableDays.forEach(this::sendArchiveCommand);
        });
    }

    private void sendArchiveCommand(DayId dayId) {
        commandStore.send(
                new ArchiveDaySchedule(dayId),
                new CommandMetadata(CorrelationId.create(idGenerator), CausationId.create(idGenerator)));
    }
}
