//package com.eventstore.scheduling.infrastructure.eventstore;
//
//import com.eventstore.scheduling.eventsourcing.SubscriptionId;
//import com.eventstore.scheduling.test.TestEventStoreConnection;
//import lombok.val;
//import org.junit.jupiter.api.Test;
//
//import java.util.Random;
//
//import static com.eventstore.scheduling.test.TestFixtures.randomString;
//import static org.junit.jupiter.api.Assertions.assertEquals;
//
//public class EsCheckpointStoreTest implements TestEventStoreConnection {
//  @Test
//  void shouldReadAndWriteCheckpoints() {
//    val checkpoint = new Checkpoint(new Random().nextLong());
//    val subscriptionId = new SubscriptionId(randomString());
//    val store =
//        new EsCheckpointStore(
//            new EsEventStoreClient<>(streamsClient, new EsCheckpointSerde(), "test"));
//    store.write(subscriptionId, checkpoint);
//    val result = store.getCheckpoint(subscriptionId).get();
//
//    assertEquals(checkpoint, result);
//  }
//}
