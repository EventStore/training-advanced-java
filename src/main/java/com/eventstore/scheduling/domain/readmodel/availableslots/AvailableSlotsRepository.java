package com.eventstore.scheduling.domain.readmodel.availableslots;

import com.eventstore.scheduling.domain.doctorday.SlotId;
import io.vavr.collection.List;

import java.time.LocalDate;

public interface AvailableSlotsRepository {
  List<AvailableSlot> getAvailableSlotsOn(LocalDate today);

  void addSlot(AvailableSlot availableSlot);

  void hideSlot(SlotId slotId);

  void showSlot(SlotId slotId);

  void deleteSlot(SlotId slotId);
}
