package com.eventstore.scheduling.infrastructure.mongodb;

import com.eventstore.scheduling.domain.readmodel.availableslots.AvailableSlot;
import com.eventstore.scheduling.domain.doctorday.DayId;
import com.eventstore.scheduling.domain.doctorday.SlotId;
import lombok.val;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.time.LocalDate;
import java.time.LocalTime;

public class AvailableSlotCodec implements Codec<AvailableSlot> {
  @Override
  public AvailableSlot decode(BsonReader reader, DecoderContext decoderContext) {
    reader.readStartDocument();
    val availableSlot =
        new AvailableSlot(
            new DayId(reader.readString("dayId")),
            new SlotId(reader.readString("slotId")),
            LocalDate.parse(reader.readString("date")),
            LocalTime.parse(reader.readString("time")),
            reader.readString("duration"));
    reader.readEndDocument();
    return availableSlot;
  }

  @Override
  public void encode(BsonWriter writer, AvailableSlot value, EncoderContext encoderContext) {
    writer.writeStartDocument();
    writer.writeString("dayId", value.getDayId().getValue());
    writer.writeString("slotId", value.getSlotId().getValue());
    writer.writeString("date", value.getDate().toString());
    writer.writeString("time", value.getTime().toString());
    writer.writeString("duration", value.getDuration());
    writer.writeEndDocument();
  }

  @Override
  public Class<AvailableSlot> getEncoderClass() {
    return AvailableSlot.class;
  }
}
