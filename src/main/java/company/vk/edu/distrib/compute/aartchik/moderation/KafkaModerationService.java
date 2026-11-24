package company.vk.edu.distrib.compute.aartchik.moderation;

import company.vk.edu.distrib.compute.moderation.LinkCreatedEvent;
import company.vk.edu.distrib.compute.moderation.ModerationResultEvent;
import company.vk.edu.distrib.compute.moderation.ModerationService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerGroupMetadata;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

final class KafkaModerationService implements ModerationService {
    private final ReentrantLock lifecycleLock = new ReentrantLock();
    private final String bootstrapServers;
    private final BannedWords dictionary;
    @Nullable private KafkaProducer<String, String> producer;
    @Nullable private RecordWorker worker;
    private boolean started;
    private boolean stopped;

    KafkaModerationService(String bootstrapServers) throws IOException {
        this.bootstrapServers = bootstrapServers;
        dictionary = new BannedWords();
    }

    @Override
    public void start() {
        lifecycleLock.lock();
        try {
            startResources();
        } finally {
            lifecycleLock.unlock();
        }
    }

    private void startResources() {
        if (started || stopped) {
            throw new IllegalStateException("Moderation service can only be started once");
        }
        started = true;
        var settings = KafkaSettings.producer(bootstrapServers);
        settings.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "aartchik-moderation-" + UUID.randomUUID());
        settings.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 10000);
        KafkaProducer<String, String> client = new KafkaProducer<>(settings);
        producer = client;
        try {
            client.initTransactions();
        } catch (RuntimeException failure) {
            client.close(Duration.ofSeconds(1));
            throw failure;
        }
        RecordWorker consumer = new RecordWorker(
                KafkaSettings.consumer(bootstrapServers, "aartchik-moderation-v1"),
                KafkaSettings.CREATED, (record, metadata) -> moderate(client, record, metadata));
        worker = consumer;
        consumer.start();
    }

    private void moderate(
            KafkaProducer<String, String> client,
            ConsumerRecord<String, String> record,
            ConsumerGroupMetadata metadata) {
        ProducerRecord<String, String> output;
        try {
            LinkCreatedEvent event = EventCodec.decodeCreated(record.value());
            requireMatchingKey(event.id(), record.key());
            boolean passed = !dictionary.rejects(event.url());
            ModerationResultEvent result = new ModerationResultEvent(
                    event.id(), passed, passed ? "" : "URL contains a banned word", Instant.now());
            output = new ProducerRecord<>(KafkaSettings.MODERATED, event.id(), EventCodec.encode(result));
        } catch (IllegalArgumentException invalid) {
            output = new ProducerRecord<>(KafkaSettings.DEAD_LETTER, record.key(), record.value());
            output.headers().add("error", invalid.toString().getBytes(StandardCharsets.UTF_8));
        }
        client.beginTransaction();
        try {
            client.send(output);
            // Output and input offset become visible together, including after a rebalance or restart.
            client.sendOffsetsToTransaction(Map.of(
                    new TopicPartition(record.topic(), record.partition()),
                    new OffsetAndMetadata(record.offset() + 1)), metadata);
            client.commitTransaction();
        } catch (RuntimeException failure) {
            client.abortTransaction();
            throw failure;
        }
    }

    private static void requireMatchingKey(String id, @Nullable String key) {
        if (!id.equals(key)) {
            throw new IllegalArgumentException("Link event key does not match its ID");
        }
    }

    @Override
    public void stop() {
        lifecycleLock.lock();
        try {
            stopResources();
        } finally {
            lifecycleLock.unlock();
        }
    }

    private void stopResources() {
        if (stopped) {
            return;
        }
        stopped = true;
        RecordWorker consumer = worker;
        KafkaProducer<String, String> client = producer;
        if (consumer != null) {
            consumer.close();
        }
        if (client != null) {
            client.close(Duration.ofSeconds(1));
        }
    }
}
