package com.eventstore.scheduling.domain.readmodel.bookedslots;

import com.eventstore.scheduling.domain.writemodel.doctorday.SlotId;
import com.eventstore.scheduling.domain.writemodel.doctorday.state.PatientId;

import java.time.Month;

public interface BookedSlotsRepository {
  int countByPatientAndMonth(PatientId patientId, Month month);

  void addSlot(Slot slot);

  void markSlotAsBooked(SlotId slotId, PatientId patientId);

  void markSlotAsAvailable(SlotId slotId);

  Slot getSlot(SlotId slotId);
}
