package com.eventstore.scheduling.domain.doctorday;

import com.eventstore.scheduling.domain.doctorday.command.ScheduleSlot;
import com.eventstore.scheduling.domain.doctorday.error.*;
import com.eventstore.scheduling.domain.doctorday.event.*;
import com.eventstore.scheduling.domain.service.IdGenerator;
import com.eventstore.scheduling.eventsourcing.AggregateRootSnapshot;
import io.vavr.collection.List;
import lombok.SneakyThrows;
import lombok.val;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class Day extends AggregateRootSnapshot {
    public Slots slots = new Slots(List.empty());
    public boolean isArchived = false;
    public boolean isCancelled = false;
    public boolean isScheduled = false;

    private DayId dayId = null;
    private LocalDate date = null;

    public Day() {
        register(DayScheduled.class, this::when);
        register(SlotScheduled.class, this::when);
        register(SlotBooked.class, this::when);
        register(SlotBookingCancelled.class, this::when);
        register(SlotScheduleCancelled.class, this::when);
        register(DayScheduleCancelled.class, this::when);
        register(DayScheduleArchived.class, this::when);
        registerSnapshot(DaySnapshot.class, this::loadDaySnapshot, this::getDaySnapshot);
    }

    private DaySnapshot getDaySnapshot() {
        return new DaySnapshot(
                slots.getSlots(),
                isArchived,
                isCancelled,
                isScheduled,
                dayId,
                date
        );
    }

    private void loadDaySnapshot(DaySnapshot snapshot) {
        setId(snapshot.dayId().value());
        slots = new Slots(snapshot.slots());
        isArchived = snapshot.isArchived();
        isCancelled = snapshot.isCancelled();
        isScheduled = snapshot.isScheduled();
        dayId = snapshot.dayId();
        date = snapshot.date();
    }

    @SneakyThrows
    private void isCancelledOrArchived() {
        if (isArchived) {
            throw new DayScheduleAlreadyArchived();
        }
        if (isCancelled) {
            throw new DayScheduleAlreadyCancelled();
        }
    }

    @SneakyThrows
    public void schedule(DoctorId doctorId, LocalDate date, List<ScheduleSlot> slots, IdGenerator idGenerator) {
        isCancelledOrArchived();
        if (isScheduled) {
            throw new DayAlreadyScheduled();
        }

        val dayId = new DayId(doctorId, date);
        raise(new DayScheduled(dayId, doctorId, date));

        slots.forEach(slot -> {
            raise(new SlotScheduled(SlotId.create(idGenerator), dayId, LocalDateTime.of(date, slot.startTime()), slot.duration()));
        });
    }

    @SneakyThrows
    public void scheduleSlot(SlotId slotId, LocalTime startTime, Duration duration) {
        isCancelledOrArchived();
        isNotScheduled();

        if (slots.overlapsWith(startTime, duration)) {
            throw new SlotOverlapped();
        }

        raise(new SlotScheduled(slotId, dayId, LocalDateTime.of(date, startTime), duration));
    }

    @SneakyThrows
    private void isNotScheduled() {
        if (!isScheduled) {
            throw new DayNotScheduled();
        }
    }

    @SneakyThrows
    public void bookSlot(SlotId slotId, PatientId patientId) {
        isCancelledOrArchived();
        isNotScheduled();

        switch (slots.getState(slotId)) {
            case Booked:
                throw new SlotAlreadyBooked();
            case NotScheduled:
                throw new SlotNotScheduled();
            case Available:
                raise(new SlotBooked(dayId, slotId, patientId));
        }
    }

    @SneakyThrows
    public void cancelBookedSlot(SlotId slotId, String reason) {
        isCancelledOrArchived();
        isNotScheduled();

        if (!slots.hasBookedSlot(slotId)) {
            throw new SlotNotBooked();
        }

        raise(new SlotBookingCancelled(dayId, slotId, reason));
    }

    public void cancel() {
        isCancelledOrArchived();
        isNotScheduled();

        slots.getBookedSlots().forEach(slot -> {
            raise(new SlotBookingCancelled(dayId, slot.slotId(), "doctor cancelled the day"));
        });

        slots.getSlots().forEach(slot -> {
            raise(new SlotScheduleCancelled(dayId, slot.slotId()));
        });

        raise(new DayScheduleCancelled(dayId, "doctor cancelled the day"));
    }

    private void when(DayScheduled event) {
        dayId = event.dayId();
        setId(dayId.value());
        isScheduled = true;
        date = event.date();
    }

    private void when(SlotScheduled event) {
        slots.add(event);
    }

    private void when(SlotBooked event) {
        slots.markAsBooked(event.slotId());
    }

    private void when(SlotBookingCancelled event) {
        slots.markAsAvailable(event.slotId());
    }

    private void when(SlotScheduleCancelled event) {
        slots.remove(event.slotId());
    }

    private void when(DayScheduleCancelled event) {
        isCancelled = true;
    }

    private void when(DayScheduleArchived event) {
        isArchived = true;
    }
}
