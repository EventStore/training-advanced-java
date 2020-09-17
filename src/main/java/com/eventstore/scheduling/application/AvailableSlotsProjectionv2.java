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
    when(SlotScheduled.class, scheduled -> {
      return;
    });

    when(SlotBooked.class, booked -> {
      return;
    });

    when(SlotBookingCancelled.class, cancelled -> {
      return;
    });

    when(SlotScheduleCancelled.class, cancelled -> {
      return;
    });
  }
}
