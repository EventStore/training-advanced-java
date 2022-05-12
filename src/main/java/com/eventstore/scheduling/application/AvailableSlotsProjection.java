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
                    scheduled._1.dayId(),
                    scheduled._1.slotId(),
                    scheduled._1.startDateTime().toLocalDate(),
                    scheduled._1.startDateTime().toLocalTime(),
                    scheduled._1.duration().toString())));
    when(SlotBooked.class, booked -> repository.hideSlot(booked._1.slotId()));
    when(SlotBookingCancelled.class, cancelled -> repository.showSlot(cancelled._1.slotId()));
    when(SlotScheduleCancelled.class, cancelled -> repository.deleteSlot(cancelled._1.slotId()));
  }
}
