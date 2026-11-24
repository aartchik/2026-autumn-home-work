package company.vk.edu.distrib.compute.aartchik.moderation;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerGroupMetadata;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.Nullable;
import java.util.function.Consumer;
import java.util.function.BiConsumer;

final class RecordWorker implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(RecordWorker.class);
    private static final Duration POLL_TIMEOUT = Duration.ofMillis(200);
    private static final Duration CLOSE_TIMEOUT = Duration.ofSeconds(1);

    private final Properties settings;
    private final String topic;
    private final BiConsumer<ConsumerRecord<String, String>, ConsumerGroupMetadata> handler;
    private final boolean commitOffsets;
    private final Thread thread;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicReference<@Nullable KafkaConsumer<String, String>> consumer = new AtomicReference<>();

    RecordWorker(Properties settings, String topic, Consumer<ConsumerRecord<String, String>> handler) {
        this(settings, topic, (record, metadata) -> handler.accept(record), true);
    }

    RecordWorker(
            Properties settings,
            String topic,
            BiConsumer<ConsumerRecord<String, String>, ConsumerGroupMetadata> transactionalHandler) {
        this(settings, topic, transactionalHandler, false);
    }

    private RecordWorker(
            Properties settings,
            String topic,
            BiConsumer<ConsumerRecord<String, String>, ConsumerGroupMetadata> handler,
            boolean commitOffsets) {
        this.settings = settings;
        this.topic = topic;
        this.handler = handler;
        this.commitOffsets = commitOffsets;
        thread = Thread.ofPlatform().name("aartchik-" + topic).unstarted(this::run);
    }

    void start() {
        thread.start();
    }

    @SuppressWarnings("PMD.UseTryWithResources") // Kafka default close can block for 30 seconds.
    private void run() {
        KafkaConsumer<String, String> client = new KafkaConsumer<>(settings);
        try {
            consumer.set(client);
            client.subscribe(List.of(topic));
            while (running.get()) {
                try {
                    consume(client);
                } catch (WakeupException wakeup) {
                    if (running.get()) {
                        LOG.warn("Kafka consumer was woken unexpectedly", wakeup);
                    }
                } catch (KafkaException failure) {
                    LOG.warn("Kafka consumer will retry", failure);
                }
            }
        } catch (RuntimeException failure) {
            LOG.error("Kafka consumer stopped", failure);
        } finally {
            consumer.set(null);
            client.close(CLOSE_TIMEOUT);
        }
    }

    private void consume(KafkaConsumer<String, String> client) {
        var records = client.poll(POLL_TIMEOUT);
        // Poll advances all positions. Keep unprocessed records available if a handler fails mid-batch.
        for (var partition : records.partitions()) {
            client.seek(partition, records.records(partition).getFirst().offset());
        }
        for (var record : records) {
            if (!running.get()) {
                return;
            }
            if (!process(client, record)) {
                return;
            }
        }
    }

    private boolean process(KafkaConsumer<String, String> client, ConsumerRecord<String, String> record) {
        TopicPartition partition = new TopicPartition(record.topic(), record.partition());
        try {
            handler.accept(record, client.groupMetadata());
            if (commitOffsets) {
                client.commitSync(Map.of(partition, new OffsetAndMetadata(record.offset() + 1)), CLOSE_TIMEOUT);
            }
            client.seek(partition, record.offset() + 1);
            return true;
        } catch (RuntimeException failure) {
            // A failed operation must be retried before advancing this partition's offset.
            client.seek(partition, record.offset());
            if (LOG.isWarnEnabled()) {
                LOG.warn("Kafka record will be retried at {}:{}", partition, record.offset(), failure);
            }
            return false;
        }
    }

    @Override
    public void close() {
        running.set(false);
        KafkaConsumer<String, String> client = consumer.get();
        if (client != null) {
            client.wakeup();
        }
        try {
            thread.join();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
