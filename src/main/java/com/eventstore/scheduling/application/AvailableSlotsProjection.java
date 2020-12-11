package com.eventstore.scheduling.application;


import com.eventstore.scheduling.domain.readmodel.availableslots.AvailableSlot;
import com.eventstore.scheduling.domain.doctorday.event.SlotBooked;
import com.eventstore.scheduling.domain.doctorday.event.SlotBookingCancelled;
import com.eventstore.scheduling.domain.doctorday.event.SlotScheduleCancelled;
import com.eventstore.scheduling.domain.doctorday.event.SlotScheduled;
import com.eventstore.scheduling.infrastructure.mongodb.MongoAvailableSlotsRepository;
import com.eventstore.scheduling.infrastructure.projections.EventHandler;

public class AvailableSlotsProjection extends EventHandler {
  public AvailableSlotsProjection(MongoAvailableSlotsRepository repository) {
    when(SlotScheduled.class, scheduled -> repository.addSlot(
            new AvailableSlot(
                    scheduled._1.getDayId(),
                    scheduled._1.getSlotId(),
                    scheduled._1.getStartDateTime().toLocalDate(),
                    scheduled._1.getStartDateTime().toLocalTime(),
                    scheduled._1.getDuration().toString())));
    when(SlotBooked.class, booked -> repository.hideSlot(booked._1.getSlotId()));
    when(SlotBookingCancelled.class, cancelled -> repository.showSlot(cancelled._1.getSlotId()));
    when(SlotScheduleCancelled.class, cancelled -> repository.deleteSlot(cancelled._1.getSlotId()));
  }
}
