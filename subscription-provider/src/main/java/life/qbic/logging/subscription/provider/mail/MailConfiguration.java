package life.qbic.logging.subscription.provider.mail;

import life.qbic.logging.subscription.provider.mail.property.MailProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * <b>Mail Configuration</b>
 * <p>
 * Registers the {@link MailProperties} configuration properties bean so that the e-mail settings
 * are bound from the application properties.
 *
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties(MailProperties.class)
public class MailConfiguration {

}