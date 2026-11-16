package company.vk.edu.distrib.compute.test.moderation;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.BindException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import company.vk.edu.distrib.compute.AbstractHttpServiceFactory;
import company.vk.edu.distrib.compute.moderation.ModerationDeadLetterTest;
import company.vk.edu.distrib.compute.moderation.ModerationService;
import company.vk.edu.distrib.compute.moderation.ModerationServiceFactory;
import company.vk.edu.distrib.compute.moderation.ModerationServiceFactoryTest;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerService;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerTest;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junitpioneer.jupiter.RetryingTest;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

import static company.vk.edu.distrib.compute.test.AbstractArgumentsProvider.findAnnotatedFactories;
import static company.vk.edu.distrib.compute.test.TestUtils.TEST_CREDENTIALS;
import static company.vk.edu.distrib.compute.test.TestUtils.TEST_LONG_LINK;
import static company.vk.edu.distrib.compute.test.TestUtils.TIMEOUT;
import static company.vk.edu.distrib.compute.test.TestUtils.createUser;
import static company.vk.edu.distrib.compute.test.TestUtils.extractId;
import static company.vk.edu.distrib.compute.test.TestUtils.get;
import static company.vk.edu.distrib.compute.test.TestUtils.post;
import static company.vk.edu.distrib.compute.test.TestUtils.randomPort;
import static company.vk.edu.distrib.compute.test.TestUtils.runHttpCtx;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests of asynchronous link moderation: {@link UrlShortenerService} - Kafka - {@link ModerationService}.
 */
@ParameterizedClass(allowZeroInvocations = true)
@MethodSource("factoryPairs")
@Testcontainers(disabledWithoutDocker = true)
@EnabledIfEnvironmentVariable(named = "CURRENT_DATE", matches = "2026-(11-11|11-12|11-13|11-14|11-15|11-16|11-17|11-18|11-19|11-20|11-21|11-22|11-23)")
public class KafkaTest {
    private static final String LINKS_CREATED_TOPIC_NAME = "links.created";
    private static final String LINKS_MODERATED_TOPIC_NAME = "links.moderated";
    private static final String LINKS_CREATED_DLT_TOPIC_NAME = "links.created.dlt";
    private static final String ERROR_HEADER = "error";
    private static final Duration DUPLICATE_WATCH_TIME = Duration.ofSeconds(3);
    private static final int LINKS_PER_SCENARIO = 10;
    private static final int PARTITIONS = 3;
    private static final short REPLICATION_FACTOR = 1;
    private static final long ADMIN_TIMEOUT_SECONDS = 30;
    private static final Duration MODERATION_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration SCENARIO_TIMEOUT = MODERATION_TIMEOUT.plusSeconds(5);
    private static final String UNREACHABLE_KAFKA = "localhost:1";
    private static final String CLEAN_LINK = TEST_LONG_LINK;
    private static final String BANNED_LINK = "https://example.com/offense";
    private static final String ENCODED_BANNED_LINK = "https://example.com/%6Fbscene?q=1";
    private static final int PACKAGE_PREFIX_LEN = "company.vk.edu.distrib.compute.".length();

    private static HttpClient httpClient;

    @Container
    private static final ConfluentKafkaContainer KAFKA = new ConfluentKafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.7.8")
    );

    @Parameter(0)
    AbstractHttpServiceFactory<? extends UrlShortenerService> shortenerFactory;

    @Parameter(1)
    ModerationServiceFactory moderationFactory;

    @BeforeAll
    static void setUp() throws ExecutionException, InterruptedException, TimeoutException {
        httpClient = HttpClient.newHttpClient();
        try (Admin admin = Admin.create(Map.of(
            CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers()))) {
            List<NewTopic> topics = List.of(
                new NewTopic(LINKS_CREATED_TOPIC_NAME, PARTITIONS, REPLICATION_FACTOR),
                new NewTopic(LINKS_MODERATED_TOPIC_NAME, PARTITIONS, REPLICATION_FACTOR),
                new NewTopic(LINKS_CREATED_DLT_TOPIC_NAME, PARTITIONS, REPLICATION_FACTOR)
            );
            for (var entry : admin.createTopics(topics).values().entrySet()) {
                try {
                    entry.getValue().get(ADMIN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                } catch (ExecutionException e) {
                    if (!(e.getCause() instanceof TopicExistsException)) {
                        throw e;
                    }
                }
            }
        }
    }

    @AfterAll
    static void tearDown() {
        httpClient.close();
    }

    @Test
    void topicsAreCreated() throws ExecutionException, InterruptedException, TimeoutException {
        try (Admin admin = Admin.create(Map.of(
            CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers()))) {
            Set<String> names = admin.listTopics().names().get(ADMIN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertTrue(names.contains(LINKS_CREATED_TOPIC_NAME));
            assertTrue(names.contains(LINKS_MODERATED_TOPIC_NAME));
            assertTrue(names.contains(LINKS_CREATED_DLT_TOPIC_NAME));
        }
    }

    @RetryingTest(onExceptions = BindException.class, maxAttempts = 10, suspendForMs = 500)
    void createShortLinkSucceeds() {
        assertTimeoutPreemptively(TIMEOUT, () -> withShortener(null, port -> {
            String id = createLink(port, CLEAN_LINK);
            HttpResponse<String> stored = get(TEST_CREDENTIALS, "/v0/links/" + id, String.class);
            assertEquals(200, stored.statusCode());
            assertEquals(CLEAN_LINK, stored.body());
        }));
    }

    @RetryingTest(onExceptions = BindException.class, maxAttempts = 10, suspendForMs = 500)
    void bannedLinkIsRemoved() {
        ModerationService moderation = startModeration();
        try {
            assertTimeoutPreemptively(SCENARIO_TIMEOUT, () -> withShortener(KAFKA.getBootstrapServers(), port -> {
                String clean = createLink(port, CLEAN_LINK);
                String banned = createLink(port, BANNED_LINK);

                awaitTrue("banned link must be removed", () -> get("/" + banned).statusCode() == 404);
                assertEquals(301, get("/" + clean).statusCode());
            }));
        } finally {
            moderation.stop();
        }
    }

    @RetryingTest(onExceptions = BindException.class, maxAttempts = 10, suspendForMs = 500)
    void encodedBannedWordIsDetected() {
        ModerationService moderation = startModeration();
        try {
            assertTimeoutPreemptively(SCENARIO_TIMEOUT, () -> withShortener(KAFKA.getBootstrapServers(), port -> {
                String banned = createLink(port, ENCODED_BANNED_LINK);
                awaitTrue("encoded banned word must be detected", () -> get("/" + banned).statusCode() == 404);
            }));
        } finally {
            moderation.stop();
        }
    }

    @RetryingTest(onExceptions = BindException.class, maxAttempts = 10, suspendForMs = 500)
    void severalModerationInstancesShareTheLoad() {
        ModerationService first = startModeration();
        ModerationService second = startModeration();
        try {
            assertTimeoutPreemptively(SCENARIO_TIMEOUT, () -> withShortener(KAFKA.getBootstrapServers(), port -> {
                List<String> banned = new ArrayList<>();
                for (int i = 0; i < 10; i++) {
                    banned.add(createLink(port, BANNED_LINK + "/" + i));
                }
                awaitTrue("all banned links must be removed",
                    () -> banned.stream().allMatch(id -> get("/" + id).statusCode() == 404));
            }));
        } finally {
            first.stop();
            second.stop();
        }
    }

    @RetryingTest(onExceptions = BindException.class, maxAttempts = 10, suspendForMs = 500)
    void linkIsKeptWhileModerationIsDown() {
        assertTimeoutPreemptively(TIMEOUT, () -> withShortener(KAFKA.getBootstrapServers(), port -> {
            String banned = createLink(port, BANNED_LINK);
            assertEquals(301, get("/" + banned).statusCode());
        }));
    }

    @RetryingTest(onExceptions = BindException.class, maxAttempts = 10, suspendForMs = 500)
    void shortenerWorksWithUnreachableKafka() {
        assertTimeoutPreemptively(TIMEOUT, () -> withShortener(UNREACHABLE_KAFKA, port -> {
            for (int i = 0; i < 20; i++) {
                String id = createLink(port, CLEAN_LINK);
                assertEquals(301, get("/" + id).statusCode());
            }
        }));
    }

    @RetryingTest(onExceptions = BindException.class, maxAttempts = 10, suspendForMs = 500)
    void linkCreatedEventIsKeyedByLinkId() {
        assertTimeoutPreemptively(SCENARIO_TIMEOUT, () -> withShortener(KAFKA.getBootstrapServers(), port -> {
            String id = createLink(port, CLEAN_LINK);
            awaitRecordWithKey(LINKS_CREATED_TOPIC_NAME, id);
        }));
    }

    @RetryingTest(onExceptions = BindException.class, maxAttempts = 10, suspendForMs = 500)
    void everyLinkIsModeratedExactlyOnce() {
        ModerationService first = startModeration();
        ModerationService second = startModeration();
        try {
            assertTimeoutPreemptively(SCENARIO_TIMEOUT, () -> withShortener(KAFKA.getBootstrapServers(), port -> {
                Set<String> ids = new HashSet<>();
                for (int i = 0; i < LINKS_PER_SCENARIO; i++) {
                    ids.add(createLink(port, i % 2 == 0 ? CLEAN_LINK : BANNED_LINK));
                }
                Map<String, Integer> results = countModerationResults(ids);
                for (String id : ids) {
                    assertEquals(1, results.getOrDefault(id, 0), "moderation results of link " + id);
                }
            }));
        } finally {
            first.stop();
            second.stop();
        }
    }

    /**
     * Bonus task: a message that cannot be processed is forwarded to the dead letter topic.
     */
    @RetryingTest(onExceptions = BindException.class, maxAttempts = 10, suspendForMs = 500)
    void poisonMessageGoesToDeadLetterTopic() {
        assumeDeadLetterSupported();
        String poison = poisonMessage();
        ModerationService moderation = startModeration();
        try {
            assertTimeoutPreemptively(SCENARIO_TIMEOUT, () -> {
                sendToCreatedTopic(poison);
                ConsumerRecord<String, String> forwarded = awaitDeadLetter(poison);
                Header error = forwarded.headers().lastHeader(ERROR_HEADER);
                assertFalse(error == null || error.value() == null || error.value().length == 0,
                    "dead letter must carry a non-empty '" + ERROR_HEADER + "' header");
            });
        } finally {
            moderation.stop();
        }
    }

    /**
     * Bonus task: moderation keeps working after a message it cannot process.
     */
    @RetryingTest(onExceptions = BindException.class, maxAttempts = 10, suspendForMs = 500)
    void moderationSurvivesPoisonMessage() {
        assumeDeadLetterSupported();
        ModerationService moderation = startModeration();
        try {
            assertTimeoutPreemptively(SCENARIO_TIMEOUT, () -> withShortener(KAFKA.getBootstrapServers(), port -> {
                sendToCreatedTopic(poisonMessage());
                String banned = createLink(port, BANNED_LINK);
                awaitTrue("banned link must be removed after a poison message",
                    () -> get("/" + banned).statusCode() == 404);
            }));
        } finally {
            moderation.stop();
        }
    }

    private void assumeDeadLetterSupported() {
        assumeTrue(moderationFactory.getClass().isAnnotationPresent(ModerationDeadLetterTest.class),
            "dead letter topic (bonus task) is not implemented");
    }

    private static String poisonMessage() {
        return "poison-" + UUID.randomUUID() + " {{{ not an event";
    }

    private static void sendToCreatedTopic(String value) throws ExecutionException, InterruptedException,
        TimeoutException {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            producer.send(new ProducerRecord<>(LINKS_CREATED_TOPIC_NAME, "poison", value))
                .get(ADMIN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
    }

    private static KafkaConsumer<String, String> subscribe(String topic) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(List.of(topic));
        return consumer;
    }

    private static void awaitRecordWithKey(String topic, String key) {
        try (KafkaConsumer<String, String> consumer = subscribe(topic)) {
            long deadline = System.nanoTime() + MODERATION_TIMEOUT.toNanos();
            while (System.nanoTime() < deadline) {
                for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(200))) {
                    if (key.equals(record.key())) {
                        return;
                    }
                }
            }
        }
        fail("no record with key " + key + " in " + topic + ": the key must be the link id");
    }

    /**
     * Waits until every link has a moderation result, then keeps watching for a while to catch duplicates.
     */
    private static Map<String, Integer> countModerationResults(Set<String> ids) {
        Map<String, Integer> counts = new HashMap<>();
        try (KafkaConsumer<String, String> consumer = subscribe(LINKS_MODERATED_TOPIC_NAME)) {
            long deadline = System.nanoTime() + MODERATION_TIMEOUT.toNanos();
            while (System.nanoTime() < deadline && counts.size() < ids.size()) {
                count(consumer, ids, counts);
            }
            long watchUntil = System.nanoTime() + DUPLICATE_WATCH_TIME.toNanos();
            while (System.nanoTime() < watchUntil) {
                count(consumer, ids, counts);
            }
        }
        return counts;
    }

    private static void count(KafkaConsumer<String, String> consumer, Set<String> ids, Map<String, Integer> counts) {
        for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(200))) {
            if (ids.contains(record.key())) {
                counts.merge(record.key(), 1, Integer::sum);
            }
        }
    }

    private static ConsumerRecord<String, String> awaitDeadLetter(String value) {
        try (KafkaConsumer<String, String> consumer = subscribe(LINKS_CREATED_DLT_TOPIC_NAME)) {
            long deadline = System.nanoTime() + MODERATION_TIMEOUT.toNanos();
            while (System.nanoTime() < deadline) {
                for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(200))) {
                    if (value.equals(record.value())) {
                        return record;
                    }
                }
            }
        }
        return fail("the poison message did not reach " + LINKS_CREATED_DLT_TOPIC_NAME
            + " unchanged within " + MODERATION_TIMEOUT);
    }

    private ModerationService startModeration() {
        try {
            ModerationService service = moderationFactory.create(KAFKA.getBootstrapServers());
            service.start();
            return service;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void withShortener(@Nullable String kafkaBootstrapServers, ShortenerScenario scenario) throws Exception {
        int port = randomPort();
        UrlShortenerService service = shortenerFactory.create(port);
        if (kafkaBootstrapServers != null) {
            service.setKafkaBootstrapServers(kafkaBootstrapServers);
        }
        service.start();
        try {
            runHttpCtx(httpClient, port, () -> {
                assertEquals(200, createUser(TEST_CREDENTIALS).statusCode());
                scenario.run(port);
            });
        } finally {
            service.stop();
        }
    }

    private static String createLink(int port, String url) {
        HttpResponse<String> created = post(TEST_CREDENTIALS, "/v0/links", url);
        assertEquals(201, created.statusCode());
        return extractId(port, created.body());
    }

    private static void awaitTrue(String message, BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + MODERATION_TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(100);
        }
        fail(message);
    }

    /**
     * Pairs the URL shortener and the moderation service factories of the same student.
     */
    static Stream<Arguments> factoryPairs() {
        var shortenerFactories = groupByUsername(findAnnotatedFactories(UrlShortenerTest.class));
        var moderationFactories = groupByUsername(findAnnotatedFactories(ModerationServiceFactoryTest.class));
        return shortenerFactories.entrySet().stream()
            .filter(it -> moderationFactories.containsKey(it.getKey()))
            .map(it -> Arguments.of(
                ReflectionUtils.newInstance(it.getValue()),
                ReflectionUtils.newInstance(moderationFactories.get(it.getKey()))));
    }

    private static Map<String, Class<?>> groupByUsername(Collection<Class<?>> classes) {
        return classes.stream().collect(Collectors.toMap(
            clazz -> extractUsername(clazz.getPackageName()),
            Function.identity(),
            (x, _) -> x
        ));
    }

    private static String extractUsername(String packageName) {
        var withoutPrefix = packageName.substring(PACKAGE_PREFIX_LEN);
        int dot = withoutPrefix.indexOf('.');
        return dot < 0 ? withoutPrefix : withoutPrefix.substring(0, dot);
    }

    @FunctionalInterface
    private interface ShortenerScenario {
        void run(int port) throws Throwable;
    }
}
