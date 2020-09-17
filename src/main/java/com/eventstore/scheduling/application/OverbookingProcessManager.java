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

    when(SlotScheduled.class, slotScheduled -> repository.addSlot(
            new Slot(
                    slotScheduled.getSlotId(),
                    slotScheduled.getDayId(),
                    slotScheduled.getStartDateTime().getMonth())));

    when(SlotBooked.class, slotBooked -> {
      repository.markSlotAsBooked(slotBooked.getSlotId(), slotBooked.getPatientId());

      val slot = repository.getSlot(slotBooked.getSlotId());
      val count = repository.countByPatientAndMonth(slotBooked.getPatientId(), slot.getMonth());

      if (count > bookingLimitPerPatient) {
        commandStore.send(
                new CancelSlotBooking(slot.getDayId(), slot.getSlotId(), "Overbooking"),
                new CommandMetadata(
                        CorrelationId.create(idGenerator),
                        CausationId.create(idGenerator)));
      }
    });

    when(SlotBookingCancelled.class, slotBookedCancelled -> repository.markSlotAsAvailable(slotBookedCancelled.getSlotId()));
  }
}
