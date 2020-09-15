package com.eventstore.scheduling.domain.writemodel.doctorday.event;

import com.eventstore.scheduling.domain.writemodel.doctorday.SlotId;
import lombok.Data;
import lombok.NonNull;

@Data
public class SlotCancelled implements Event {
  private final @NonNull SlotId slotId;
}
