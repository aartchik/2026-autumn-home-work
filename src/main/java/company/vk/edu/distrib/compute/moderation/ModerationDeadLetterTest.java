package company.vk.edu.distrib.compute.moderation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@link ModerationServiceFactory} implementation whose service supports the dead letter topic
 * (the bonus task): a message of the {@code links.created} topic that cannot be processed is forwarded
 * unchanged to the {@code links.created.dlt} topic with a non-empty {@code error} header,
 * and the service keeps moderating the following messages.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModerationDeadLetterTest {
}
