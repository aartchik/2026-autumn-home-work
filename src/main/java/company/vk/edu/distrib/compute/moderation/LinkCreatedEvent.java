package company.vk.edu.distrib.compute.moderation;

import java.time.Instant;

public record LinkCreatedEvent(String id, String url, Instant createdAt) {
}
