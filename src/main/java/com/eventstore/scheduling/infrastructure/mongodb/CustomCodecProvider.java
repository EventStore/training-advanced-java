package com.eventstore.scheduling.infrastructure.mongodb;

import com.eventstore.scheduling.domain.readmodel.availableslots.AvailableSlot;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

public class CustomCodecProvider implements CodecProvider {
  @Override
  public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
    if (clazz == AvailableSlot.class) {
      return (Codec<T>) new AvailableSlotCodec();
    }
    return null;
  }
}
