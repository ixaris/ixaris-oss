package com.ixaris.commons.kafka.multitenancy;

import static com.ixaris.commons.async.lib.CompletionStageUtil.all;
import static com.ixaris.commons.async.lib.CompletionStageUtil.join;
import static com.ixaris.commons.kafka.test.TestKafkaServer.TEST_KAFKA_PORT;
import static com.ixaris.commons.misc.lib.object.Tuple.tuple;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Duration.TEN_SECONDS;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.awaitility.Duration;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.AsyncExecutor;
import com.ixaris.commons.async.lib.CompletionStageUtil;
import com.ixaris.commons.kafka.test.TestKafkaCluster;
import com.ixaris.commons.misc.lib.object.Tuple2;

public class KafkaConnectionIT {
    
    private static final Logger LOG = LoggerFactory.getLogger(KafkaConnectionIT.class);
    private static final Duration PUBLISH_TIMEOUT = new Duration(20, TimeUnit.SECONDS);
    private static final Duration EVENT_RECEIVE_TIMEOUT = TEN_SECONDS;
    private static final ScheduledExecutorService EXECUTOR = AsyncExecutor.DEFAULT;
    
    @Test
    public void testSimplePublish() {
        withKafka(() -> withConnection(connection -> {
            final String topicName = "testTopicSimple";
            
            final List<Tuple2<Instant, String>> subscriber1Events = subscribe(connection, topicName, null);
            
            publishEventsToTopic(connection, topicName, 1);
            await().atMost(EVENT_RECEIVE_TIMEOUT).until(() -> (subscriber1Events.size() == 1));
            
            unsubscribeFromTopic(connection, topicName, null);
            
            // Publish event that should not be received by subscribers
            join(blockingPublish(connection, topicName, "shouldNotReceiveThisEvent"));
            
            // Ensure that we don't receive the event (at least until the timeout)
            runAfterDuration(EVENT_RECEIVE_TIMEOUT, () -> assertThat(subscriber1Events.size()).isEqualTo(1));
        }));
    }
    
    @Test
    public void whenKafkaIsDown_thenPublishingEventsShouldFail() {
        withConnection(connection -> assertThat(blockingPublish(connection, "testTopic", "shouldFailToArrive"))
            .hasFailedWithThrowableThat()
            .isInstanceOf(TimeoutException.class));
    }
    
    @Test
    public void givenKafkaIsDown_whenKafkaIsStarted_thenPublishingEventsShouldSucceed() {
        withConnection(connection -> {
            assertThat(blockingPublish(connection, "testTopic", "value"))
                .hasFailedWithThrowableThat()
                .isInstanceOf(TimeoutException.class);
            
            withKafka(() -> assertThat(blockingPublish(connection, "testTopic", "value")).isCompleted());
        });
    }
    
    @Test
    public void givenMultipleSubscribers_afterUnsubscribing_subscribersShouldNotReceiveAdditionalEvents() {
        withKafka(() -> withConnection(connection -> {
            final String topicName = "testTopic";
            
            final List<Tuple2<Instant, String>> subscriber1Events = subscribe(connection, topicName, null);
            final List<Tuple2<Instant, String>> subscriber1EventsAlt = subscribe(connection, topicName, "1");
            
            final int numberOfReceivableEvents = 6;
            publishEventsToTopic(connection, topicName, numberOfReceivableEvents);
            await()
                .atMost(EVENT_RECEIVE_TIMEOUT)
                .until(() -> (subscriber1Events.size() == numberOfReceivableEvents) && (subscriber1EventsAlt.size() == numberOfReceivableEvents));
            
            final List<String> subscriberWithDefaultIdEvents = sortAndExtractEvents(subscriber1Events.stream());
            final List<String> subscriberWithIdEvents = sortAndExtractEvents(subscriber1EventsAlt.stream());
            assertThat(subscriberWithIdEvents).containsExactlyElementsOf(subscriberWithDefaultIdEvents);
            
            unsubscribeFromTopic(connection, topicName, null);
            unsubscribeFromTopic(connection, topicName, "1");
            
            // Publish event that should not be received by subscribers
            assertThat(blockingPublish(connection, topicName, "shouldNotReceiveThisEvent")).isCompleted();
            
            // Ensure that we don't receive the event (at least until the timeout)
            runAfterDuration(EVENT_RECEIVE_TIMEOUT, () -> {
                assertThat(subscriber1Events.size()).isEqualTo(numberOfReceivableEvents);
                assertThat(subscriber1EventsAlt.size()).isEqualTo(numberOfReceivableEvents);
            });
        }));
    }
    
    @Test
    public void givenSubscribersWithIdAndSubscriberWithDefaultId_bothShouldReceiveTheSameEvents() {
        withKafka(() -> withConnection(connection -> {
            final String topicName = "testTopic";
            final int numberOfEvents = 6;
            
            final List<Tuple2<Instant, String>> subscriberWithId = subscribe(connection, topicName, "s10");
            final List<Tuple2<Instant, String>> subscriberWithDefaultId = subscribe(connection, topicName, null);
            
            publishEventsToTopic(connection, topicName, numberOfEvents);
            await()
                .atMost(EVENT_RECEIVE_TIMEOUT)
                .until(() -> (subscriberWithId.size() == numberOfEvents) && (subscriberWithDefaultId.size() == numberOfEvents));
            
            final List<String> subscriberWithIdEvents = sortAndExtractEvents(subscriberWithId.stream());
            final List<String> subscriberWithDefaultIdEvents = sortAndExtractEvents(subscriberWithDefaultId.stream());
            assertThat(subscriberWithIdEvents).containsExactlyElementsOf(subscriberWithDefaultIdEvents);
        }));
    }
    
    @Test
    public void givenSubscriberWithIdAndASubscriberWithDifferentId_bothShouldReceiveTheSameEvents() {
        withKafka(() -> withConnection(connection -> {
            final String topicName = "testTopic";
            
            // Events should be split between the subscribers with the same subscriberGroup1Id.
            // Here we assume that one of the subscribers was subscribed before the events started being published.
            final String subscriberGroup1Id = "s1";
            final List<Tuple2<Instant, String>> group1SubscriberEvents = subscribe(connection, topicName, subscriberGroup1Id);
            
            final int numberOfEvents = 6;
            publishEventsToTopic(connection, topicName, numberOfEvents);
            
            // Subscriber 2 should receive all the events as it is the only subscriber with that id
            // (even though it subscribed later, since we're reading all events by default)
            final String subscriberGroup2Id = "s2";
            final List<Tuple2<Instant, String>> subscriber2Events = subscribe(connection, topicName, subscriberGroup2Id);
            
            await()
                .atMost(EVENT_RECEIVE_TIMEOUT)
                .until(() -> (group1SubscriberEvents.size() == numberOfEvents) && (subscriber2Events.size() == numberOfEvents));
            
            final List<String> combinedS1EventsSortedByTimestamp = sortAndExtractEvents(group1SubscriberEvents.stream());
            final List<String> s2Events = sortAndExtractEvents(subscriber2Events.stream());
            assertThat(combinedS1EventsSortedByTimestamp)
                .hasSize(numberOfEvents)
                .doesNotHaveDuplicates()
                .containsExactlyElementsOf(s2Events);
            
            unsubscribeFromTopic(connection, topicName, subscriberGroup1Id);
            unsubscribeFromTopic(connection, topicName, subscriberGroup2Id);
        }));
    }
    
    private void withConnection(final Consumer<KafkaConnection> consumer) {
        final KafkaConnection connection = connection();
        connection.start();
        try {
            consumer.accept(connection);
        } finally {
            connection.stop();
        }
    }
    
    private static void withKafka(final Runnable callable) {
        final TestKafkaCluster kafka = new TestKafkaCluster();
        try {
            kafka.start();
            callable.run();
        } finally {
            kafka.stop();
        }
    }
    
    private static List<Tuple2<Instant, String>> subscribe(final KafkaConnection connection, final String topicName, final String subscriberName) {
        final List<Tuple2<Instant, String>> receivedEventsStore = new ArrayList<>();
        connection.subscribe(subscriberName, topicName, (__, message) -> {
            final String receivedEvent = new String(message, UTF_8);
            LOG.warn("Received event [{}] for subscriber [{}] in topic [{}]", receivedEvent, subscriberName, topicName);
            receivedEventsStore.add(tuple(Instant.now(), receivedEvent));
            return Async.result();
        });
        return receivedEventsStore;
    }
    
    private void publishEventsToTopic(final KafkaConnection c, final String topicName, final int numberOfMessages) {
        final CompletionStage<?>[] publishedMessages = IntStream
            .range(0, numberOfMessages)
            .mapToObj(i -> publish(c, topicName, Integer.toString(i)))
            .toArray(CompletionStage[]::new);
        final CompletionStage<Object[]> publishedAllMessages = all(publishedMessages);
        await().atMost(PUBLISH_TIMEOUT).until(() -> CompletionStageUtil.isDone(publishedAllMessages));
        assertThat(publishedAllMessages).isCompleted();
    }
    
    private static void unsubscribeFromTopic(final KafkaConnection connection, final String topicName, final String subscriberName) {
        connection.unsubscribe(subscriberName, topicName);
    }
    
    private static List<String> sortAndExtractEvents(final Stream<Tuple2<Instant, String>> events) {
        return events.sorted(Comparator.comparing(Tuple2::get1)).map(Tuple2::get2).collect(Collectors.toList());
    }
    
    private static KafkaConnection connection() {
        return new KafkaConnection(KafkaConnectionIT.EXECUTOR,
            KafkaSettings.newBuilder()
                .setUrl("localhost:" + TEST_KAFKA_PORT)
                .setTopicPrefix("test-connection")
                .setPartitions((short) 1)
                .setReplicationFactor((short) 1)
                .setGroupId("testing")
                .setMaxBlockMs(5000L)
                .setAckTimeoutMs(5000L)
                .setMinBackoffMs(5000L)
                .setMaxBackoffMs(5000L)
                .build());
    }
    
    private static CompletableFuture<Void> blockingPublish(final KafkaConnection connection, final String topic, final String message) {
        final CompletableFuture<Void> future = publish(connection, topic, message).toCompletableFuture();
        await().atMost(PUBLISH_TIMEOUT).until(future::isDone);
        return future;
    }
    
    private static Async<Void> publish(final KafkaConnection c, final String topic, final String message) {
        return c.publish(topic, 1L, message.getBytes(UTF_8));
    }
    
    private static void runAfterDuration(final Duration duration, final Runnable toRun) {
        try {
            Thread.sleep(duration.getValueInMS());
        } catch (final InterruptedException e) {
            throw new IllegalStateException(e);
        }
        toRun.run();
    }
    
}
