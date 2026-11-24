package company.vk.edu.distrib.compute.aartchik.moderation;

import company.vk.edu.distrib.compute.moderation.LinkCreatedEvent;
import company.vk.edu.distrib.compute.moderation.ModerationResultEvent;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.DateTimeException;
import java.util.Base64;

public final class EventCodec {
    private EventCodec() {
    }

    public static String encode(LinkCreatedEvent event) {
        return "1\t" + event.id() + '\t' + encodeText(event.url()) + '\t' + event.createdAt();
    }

    public static LinkCreatedEvent decodeCreated(@Nullable String value) {
        String[] fields = fields(value, 4);
        String url = decodeText(fields[2]);
        URI parsed = URI.create(url);
        if (parsed.getHost() == null
                || !("http".equalsIgnoreCase(parsed.getScheme()) || "https".equalsIgnoreCase(parsed.getScheme()))) {
            throw new IllegalArgumentException("Invalid link URL");
        }
        return new LinkCreatedEvent(fields[1], url, timestamp(fields[3]));
    }

    public static String encode(ModerationResultEvent event) {
        return "1\t" + event.id() + '\t' + event.passed() + '\t'
                + encodeText(event.reason()) + '\t' + event.moderatedAt();
    }

    public static ModerationResultEvent decodeResult(@Nullable String value) {
        String[] fields = fields(value, 5);
        if (!"true".equals(fields[2]) && !"false".equals(fields[2])) {
            throw new IllegalArgumentException("Invalid moderation decision");
        }
        return new ModerationResultEvent(
                fields[1], Boolean.valueOf(fields[2]), decodeText(fields[3]), timestamp(fields[4]));
    }

    private static String[] fields(@Nullable String value, int count) {
        if (value == null) {
            throw new IllegalArgumentException("Event value is missing");
        }
        String[] fields = value.split("\t", -1);
        if (fields.length != count || !"1".equals(fields[0]) || !fields[1].matches("[a-zA-Z0-9]{10}")) {
            throw new IllegalArgumentException("Invalid event envelope");
        }
        return fields;
    }

    private static Instant timestamp(String value) {
        try {
            return Instant.parse(value);
        } catch (DateTimeException invalid) {
            throw new IllegalArgumentException("Invalid event timestamp", invalid);
        }
    }

    private static String encodeText(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeText(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }
}
