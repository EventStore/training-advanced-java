package com.eventstore.scheduling.application;


import com.eventstore.scheduling.domain.readmodel.availableslots.AvailableSlot;
import com.eventstore.scheduling.domain.doctorday.event.SlotBooked;
import com.eventstore.scheduling.domain.doctorday.event.SlotBookingCancelled;
import com.eventstore.scheduling.domain.doctorday.event.SlotScheduleCancelled;
import com.eventstore.scheduling.domain.doctorday.event.SlotScheduled;
import com.eventstore.scheduling.infrastructure.mongodb.MongoAvailableSlotsRepositoryv2;
import com.eventstore.scheduling.infrastructure.projections.EventHandler;

public class AvailableSlotsProjectionv2 extends EventHandler {
  public AvailableSlotsProjectionv2(MongoAvailableSlotsRepositoryv2 repository) {
    when(SlotScheduled.class, scheduled -> repository.addSlot(
            new AvailableSlot(
                    scheduled.getDayId(),
                    scheduled.getSlotId(),
                    scheduled.getStartDateTime().toLocalDate(),
                    scheduled.getStartDateTime().toLocalTime(),
                    scheduled.getDuration().toString())));

    when(SlotBooked.class, booked -> repository.hideSlot(booked.getSlotId()));

    when(SlotBookingCancelled.class, cancelled -> repository.showSlot(cancelled.getSlotId()));
    when(SlotScheduleCancelled.class, cancelled -> repository.deleteSlot(cancelled.getSlotId()));
  }
}
