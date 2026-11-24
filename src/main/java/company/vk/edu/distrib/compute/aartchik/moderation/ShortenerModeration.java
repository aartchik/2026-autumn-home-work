package company.vk.edu.distrib.compute.aartchik.moderation;

import company.vk.edu.distrib.compute.aartchik.urlshortener.LinkStore;
import company.vk.edu.distrib.compute.moderation.LinkCreatedEvent;
import company.vk.edu.distrib.compute.moderation.ModerationResultEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.UUID;

public final class ShortenerModeration implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(ShortenerModeration.class);
    private final LinkStore links;
    private final EventPublisher publisher;
    private final RecordWorker results;

    public ShortenerModeration(String bootstrapServers, LinkStore links) {
        this.links = links;
        publisher = new EventPublisher(bootstrapServers);
        // Each shortener has its own storage, so every instance must see moderation decisions.
        results = new RecordWorker(
                KafkaSettings.consumer(bootstrapServers, "aartchik-shortener-" + UUID.randomUUID()),
                KafkaSettings.MODERATED, this::apply);
    }

    public void start() {
        results.start();
        publisher.start();
    }

    public void created(String id, String url) {
        publisher.publish(new LinkCreatedEvent(id, url, Instant.now()));
    }

    private void apply(ConsumerRecord<String, String> record) {
        ModerationResultEvent result;
        try {
            result = EventCodec.decodeResult(record.value());

        } catch (IllegalArgumentException invalid) {
            LOG.warn("Ignoring invalid moderation result", invalid);
            return;
        }
        if (!result.id().equals(record.key())) {
            LOG.warn("Ignoring moderation result with mismatched key");
            return;
        }
        if (!result.passed()) {
            try {
                links.delete(result.id());
            } catch (IOException failure) {
                throw new UncheckedIOException("Unable to remove rejected link", failure);
            }
        }
    }

    @Override
    public void close() {
        publisher.close();
        results.close();
    }
}
