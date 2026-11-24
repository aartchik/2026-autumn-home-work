package company.vk.edu.distrib.compute.aartchik.moderation;

import company.vk.edu.distrib.compute.moderation.LinkCreatedEvent;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

final class EventPublisher implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(EventPublisher.class);
    private final BlockingQueue<LinkCreatedEvent> pending = new ArrayBlockingQueue<>(1024);
    private final String bootstrapServers;
    private final Thread thread;
    private final AtomicBoolean running = new AtomicBoolean(true);

    EventPublisher(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
        thread = Thread.ofPlatform().name("aartchik-link-publisher").unstarted(this::run);
    }

    void start() {
        thread.start();
    }

    void publish(LinkCreatedEvent event) {
        if (running.get() && !pending.offer(event) && LOG.isWarnEnabled()) {
            LOG.warn("Moderation queue is full; link {} remains available without moderation", event.id());
        }
    }

    @SuppressWarnings("PMD.UseTryWithResources") // Bound Kafka shutdown even when the broker is unreachable.
    private void run() {
        KafkaProducer<String, String> producer = new KafkaProducer<>(KafkaSettings.producer(bootstrapServers));
        try {
            while (running.get()) {
                LinkCreatedEvent event = pending.poll(200, TimeUnit.MILLISECONDS);
                if (event != null) {
                    send(producer, event);
                }
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } finally {
            producer.close(Duration.ofSeconds(1));
        }
    }

    private void send(KafkaProducer<String, String> producer, LinkCreatedEvent event) throws InterruptedException {
        ProducerRecord<String, String> record =
                new ProducerRecord<>(KafkaSettings.CREATED, event.id(), EventCodec.encode(event));
        while (running.get()) {
            try {
                producer.send(record).get();
                return;
            } catch (ExecutionException | KafkaException failure) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Unable to publish link {}; will retry", event.id(), failure);
                }
                Thread.sleep(200);
            }
        }
    }

    @Override
    public void close() {
        running.set(false);
        thread.interrupt();
        try {
            thread.join();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
