package company.vk.edu.distrib.compute.moderation;

import java.io.IOException;

@FunctionalInterface
public interface ModerationServiceFactory {

    /**
     * Construct a moderation service instance.
     *
     * <p>
     * Instances created by the same factory against the same Kafka cluster must cooperate:
     * every {@link LinkCreatedEvent} has to be moderated by one of them, not by all of them.
     *
     * @param bootstrapServers Kafka {@code bootstrap.servers}
     * @return a moderation service that is not started yet
     */
    ModerationService create(String bootstrapServers) throws IOException;
}
