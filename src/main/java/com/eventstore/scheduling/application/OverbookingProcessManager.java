package com.eventstore.scheduling.application;

import com.eventstore.scheduling.domain.doctorday.event.SlotBooked;
import com.eventstore.scheduling.domain.doctorday.event.SlotBookingCancelled;
import com.eventstore.scheduling.domain.doctorday.event.SlotScheduled;
import com.eventstore.scheduling.domain.readmodel.bookedslots.BookedSlotsRepository;
import com.eventstore.scheduling.domain.service.IdGenerator;
import com.eventstore.scheduling.eventsourcing.CommandStore;
import com.eventstore.scheduling.infrastructure.projections.EventHandler;

public class OverbookingProcessManager extends EventHandler {

  public OverbookingProcessManager(
      BookedSlotsRepository repository,
      CommandStore commandStore,
      int bookingLimitPerPatient,
      IdGenerator idGenerator) {

    when(SlotScheduled.class, slotScheduled -> {});

    when(SlotBooked.class, slotBooked -> {});

    when(SlotBookingCancelled.class, slotBookedCancelled -> {});
  }
}
