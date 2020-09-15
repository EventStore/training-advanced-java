package com.eventstore.scheduling.domain.writemodel.doctorday;

import com.eventstore.scheduling.domain.writemodel.AggregateId;

public class DoctorDayId extends AggregateId {
  public DoctorDayId(DayId dayId) {
    super(dayId.getValue(), "doctorday");
  }
}
