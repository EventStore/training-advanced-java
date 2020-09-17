package com.eventstore.scheduling.domain.readmodel.archivabledays;

import com.eventstore.scheduling.domain.doctorday.DayId;
import io.vavr.collection.List;

import java.time.LocalDate;

public interface ArchivableDaysRepository {
  void add(LocalDate date, DayId dayId);

  void remove(DayId dayId);

  List<DayId> findAll(LocalDate minus);
}
