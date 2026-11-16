package company.vk.edu.distrib.compute.moderation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@link ModerationServiceFactory} implementation to be picked up
 * by the moderation integration test suite.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModerationServiceFactoryTest {
}
