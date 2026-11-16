package company.vk.edu.distrib.compute.moderation;

import java.time.Instant;

public record ModerationResultEvent(String id, Boolean passed, String reason, Instant moderatedAt) {
}
