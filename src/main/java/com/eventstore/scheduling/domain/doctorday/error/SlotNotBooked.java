package com.eventstore.scheduling.domain.doctorday.error;

import com.eventstore.scheduling.eventsourcing.Error;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
public class SlotNotBooked extends Error {}
