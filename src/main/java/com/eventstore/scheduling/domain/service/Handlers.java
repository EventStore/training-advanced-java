package com.eventstore.scheduling.domain.service;

import com.eventstore.scheduling.domain.doctorday.SlotId;
import com.eventstore.scheduling.domain.doctorday.command.*;
import com.eventstore.scheduling.infrastructure.commands.CommandHandler;
import lombok.val;
import lombok.var;

public class Handlers extends CommandHandler {
    public Handlers(DayRepository dayRepository, IdGenerator idGenerator) {
        register(ScheduleDay.class, tuple -> {
            val command = tuple._1;
            val metadata = tuple._2;
            val aggregate = dayRepository.get(command.getDayId());
            aggregate.schedule(command.getDoctorId(), command.getDate(), command.getSlots(), idGenerator);
            dayRepository.save(aggregate, metadata);
        });
        register(ScheduleSlot.class, tuple -> {
            val command = tuple._1;
            val metadata = tuple._2;
            val aggregate = dayRepository.get(command.getDayId());
            aggregate.scheduleSlot(SlotId.create(idGenerator), command.getStartTime(), command.getDuration());
            dayRepository.save(aggregate, metadata);
        });
        register(BookSlot.class, tuple -> {
            val command = tuple._1;
            val metadata = tuple._2;
            val aggregate = dayRepository.get(command.getDayId());
            aggregate.bookSlot(command.getSlotId(), command.getPatientId());
            dayRepository.save(aggregate, metadata);
        });
        register(CancelSlotBooking.class, tuple -> {
            val command = tuple._1;
            val metadata = tuple._2;
            val aggregate = dayRepository.get(command.getDayId());
            aggregate.cancelBookedSlot(command.getSlotId(), command.getReason());
            dayRepository.save(aggregate, metadata);
        });
        register(ArchiveDaySchedule.class, tuple -> {
            val command = tuple._1;
            val metadata = tuple._2;
            val aggregate = dayRepository.get(command.getDayId());
            aggregate.archive();
            dayRepository.save(aggregate, metadata);
        });
    }
}