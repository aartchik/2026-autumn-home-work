package company.vk.edu.distrib.compute.aartchik.moderation;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

final class KafkaSettings {
    static final String CREATED = "links.created";
    static final String MODERATED = "links.moderated";
    static final String DEAD_LETTER = "links.created.dlt";

    private KafkaSettings() {
    }

    static Properties producer(String bootstrapServers) {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        properties.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 1000);
        properties.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 3000);
        properties.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 10000);
        properties.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 4 * 1024 * 1024);
        return properties;
    }

    static Properties consumer(String bootstrapServers, String group) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, group);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        properties.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, false);
        properties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10);
        properties.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        return properties;
    }
}
