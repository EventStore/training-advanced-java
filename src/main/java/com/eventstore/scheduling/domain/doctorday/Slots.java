package com.eventstore.scheduling.domain.doctorday;

import com.eventstore.scheduling.domain.doctorday.event.SlotScheduled;
import io.vavr.collection.List;
import lombok.Data;
import lombok.NonNull;

import java.time.Duration;
import java.time.LocalTime;

@Data
public class Slots {
    private @NonNull List<Slot> slots;

    public void add(SlotScheduled slotScheduled) {
        slots = slots.append(
                new Slot(
                        slotScheduled.slotId(),
                        slotScheduled.startDateTime().toLocalTime(),
                        slotScheduled.duration(),
                        false));
    }

    public void remove(SlotId slotId) {
        slots = slots.filter((slot) -> (!slot.slotId().equals(slotId)));
    }

    public void markAsBooked(SlotId slotId) {
        slots = slots.map(
                (slot) -> {
                    if (slot.slotId().equals(slotId)) {
                        return new Slot(slot.slotId(), slot.startTime(), slot.duration(), true);
                    }
                    return slot;
                });
    }

    public void markAsAvailable(SlotId slotId) {
        slots = slots.map(
                (slot) -> {
                    if (slot.slotId().equals(slotId)) {
                        return new Slot(slot.slotId(), slot.startTime(), slot.duration(), false);
                    }
                    return slot;
                });
    }

    public boolean overlapsWith(LocalTime startTime, Duration duration) {
        return slots.exists((slot) -> (slot.overlapsWith(startTime, duration)));
    }

    public SlotStatus getState(SlotId slotId) {
        return slots.find(slot -> slot.slotId().equals(slotId)).map(slot -> {
            if (slot.booked()) {
                return SlotStatus.Booked;
            } else {
                return SlotStatus.Available;
            }
        }).getOrElse(SlotStatus.NotScheduled);
    }

    public boolean hasBookedSlot(SlotId slotId) {
        return slots.exists(slot -> slot.slotId().equals(slotId) && slot.booked());
    }

    public List<Slot> getBookedSlots() {
        return slots.filter(Slot::booked);
    }
}
