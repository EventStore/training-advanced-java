package com.eventstore.scheduling.application;

import com.eventstore.scheduling.domain.readmodel.bookedslots.BookedSlotsRepository;
import com.eventstore.scheduling.domain.readmodel.bookedslots.Slot;
import com.eventstore.scheduling.domain.service.IdGenerator;
import com.eventstore.scheduling.domain.doctorday.DoctorDayId;
import com.eventstore.scheduling.domain.doctorday.command.CancelSlotBooking;
import com.eventstore.scheduling.domain.doctorday.event.SlotBooked;
import com.eventstore.scheduling.domain.doctorday.event.SlotBookingCancelled;
import com.eventstore.scheduling.domain.doctorday.event.SlotScheduled;
import com.eventstore.scheduling.eventsourcing.CausationId;
import com.eventstore.scheduling.eventsourcing.CommandMetadata;
import com.eventstore.scheduling.eventsourcing.CommandStore;
import com.eventstore.scheduling.eventsourcing.CorrelationId;
import com.eventstore.scheduling.infrastructure.projections.EventHandler;
import lombok.val;

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
