package com.eventstore.scheduling.domain.service;

import com.eventstore.scheduling.domain.doctorday.SlotId;
import com.eventstore.scheduling.domain.doctorday.command.*;
import com.eventstore.scheduling.infrastructure.commands.CommandHandler;
import lombok.val;

public class Handlers extends CommandHandler {
    public Handlers(DayRepository dayRepository, IdGenerator idGenerator) {
        register(ScheduleDay.class, tuple -> {
            val command = tuple._1;
            val metadata = tuple._2;
            val aggregate = dayRepository.get(command.dayId());
            aggregate.schedule(command.doctorId(), command.date(), command.slots(), idGenerator);
            dayRepository.save(aggregate, metadata);
        });
        register(ScheduleSlot.class, tuple -> {
            val command = tuple._1;
            val metadata = tuple._2;
            val aggregate = dayRepository.get(command.dayId());
            aggregate.scheduleSlot(SlotId.create(idGenerator), command.startTime(), command.duration());
            dayRepository.save(aggregate, metadata);
        });
        register(BookSlot.class, tuple -> {
            val command = tuple._1;
            val metadata = tuple._2;
            val aggregate = dayRepository.get(command.dayId());
            aggregate.bookSlot(command.slotId(), command.patientId());
            dayRepository.save(aggregate, metadata);
        });
        register(CancelSlotBooking.class, tuple -> {
            val command = tuple._1;
            val metadata = tuple._2;
            val aggregate = dayRepository.get(command.dayId());
            aggregate.cancelBookedSlot(command.slotId(), command.reason());
            dayRepository.save(aggregate, metadata);
        });
        register(ArchiveDaySchedule.class, tuple -> {
            val command = tuple._1;
            val metadata = tuple._2;
            val aggregate = dayRepository.get(command.dayId());
            aggregate.archive();
            dayRepository.save(aggregate, metadata);
        });
    }
}