package com.eventstore.scheduling.application;

import com.eventstore.scheduling.domain.doctorday.command.CancelSlotBooking;
import com.eventstore.scheduling.domain.doctorday.event.SlotBooked;
import com.eventstore.scheduling.domain.doctorday.event.SlotBookingCancelled;
import com.eventstore.scheduling.domain.doctorday.event.SlotScheduled;
import com.eventstore.scheduling.domain.readmodel.bookedslots.BookedSlotsRepository;
import com.eventstore.scheduling.domain.readmodel.bookedslots.Slot;
import com.eventstore.scheduling.domain.service.IdGenerator;
import com.eventstore.scheduling.eventsourcing.CausationId;
import com.eventstore.scheduling.eventsourcing.CommandMetadata;
import com.eventstore.scheduling.eventsourcing.CommandStore;
import com.eventstore.scheduling.infrastructure.projections.EventHandler;
import lombok.val;

public class OverbookingProcessManager extends EventHandler {

  public OverbookingProcessManager(
      BookedSlotsRepository repository,
      CommandStore commandStore,
      int bookingLimitPerPatient,
      IdGenerator idGenerator) {

    when(SlotScheduled.class, slotScheduled -> repository.addSlot(
            new Slot(
                    slotScheduled._1.slotId(),
                    slotScheduled._1.dayId(),
                    slotScheduled._1.startDateTime().getMonth())));

    when(SlotBooked.class, slotBooked -> {
      repository.markSlotAsBooked(slotBooked._1.slotId(), slotBooked._1.patientId());

      val slot = repository.getSlot(slotBooked._1.slotId());
      val count = repository.countByPatientAndMonth(slotBooked._1.patientId(), slot.month());

      if (count > bookingLimitPerPatient) {
        commandStore.send(
                new CancelSlotBooking(slot.dayId(), slot.slotId(), "Overbooking"),
                new CommandMetadata(
                        slotBooked._2.correlationId(),
                        CausationId.create(idGenerator)));
      }
    });

    when(SlotBookingCancelled.class, slotBookedCancelled -> repository.markSlotAsAvailable(slotBookedCancelled._1.slotId()));
  }
}
