package life.qbic.datamanager.views.notifications;

import static java.util.Objects.isNull;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.RouteParameters;
import java.io.Serializable;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Service;

/**
 * A factory setting up {@link Toast} notifications
 * <p>
 * This Toast factory sets up {@link Toast} notifications by using a message source.
 *
 * @since 1.4.0
 */
@Service
public class MessageSourceToastFactory implements Serializable {

  private static final Logger LOG = LoggerFactory.logger(MessageSourceToastFactory.class);
  private final MessageSource messageSource;
  private static final Object[] EMPTY_PARAMETERS = new Object[]{};

  public MessageSourceToastFactory(@Qualifier("messageSource") MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  /**
   * Creates a toast notification with the contents found for the message key.
   * <p>
   * The following message keys have to be present:
   * <ul>
   *   <li>{@code <key>.message.type}
   *   <li>{@code <key>.message.text}
   * </ul>
   * Optionally, using messages, you can overwrite the closeable property for manual closeing the toast.
   * {@code <key>.cloaseable}
   * <p>
   * For more information please toast-notifications.properties
   *
   * @param key        the key for the messages
   * @param parameters the parameters shown in the message
   * @param locale     the locale for which to load the message
   * @return a Toast with loaded content
   */
  private Toast create(String key, Object[] parameters, Locale locale) {
    MessageType type = parseMessageType(key, locale);
    String messageText = parseMessage(key, parameters, locale);

    Component content = switch (type) {
      case HTML -> new Html("<div style=\"display:contents\">%s</div>".formatted(messageText));
      case TEXT -> new Span(messageText);
    };

    NotificationLevel level = parseLevel(key, locale);
    Duration duration = parseDuration(key, locale).orElse(Toast.DEFAULT_OPEN_DURATION);

    Toast toast = new Toast(level);
    toast.withContent(content);
    toast.setDuration(duration);

    return toast;
  }

  private String parseMessage(String key, Object[] parameters, Locale locale) {
    try {
      return messageSource.getMessage("%s.message.text".formatted(key),
          parameters, locale).strip();
    } catch (NoSuchMessageException e) {
      throw new RuntimeException("No message specified for " + key, e);
    }
  }

  private MessageType parseMessageType(String key, Locale locale) {
    try {
      String messageType = messageSource.getMessage("%s.message.type".formatted(key),
          EMPTY_PARAMETERS, locale).strip().toUpperCase();
      return MessageType.valueOf(messageType);
    } catch (NoSuchMessageException e) {
      throw new RuntimeException("No message type specified for " + key, e);
    }
  }

  private NotificationLevel parseLevel(String key, Locale locale) {
    String levelProperty;
    try {
      levelProperty = messageSource.getMessage("%s.level".formatted(key),
          EMPTY_PARAMETERS, locale);
    } catch (NoSuchMessageException e) {
      throw new RuntimeException("Missing level info for %s.".formatted(key));
    }

    try {
      return NotificationLevel.valueOf(levelProperty.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new RuntimeException(
          "Could not parse toast level for key %s: %s".formatted(key, levelProperty));
    }
  }

  private Optional<Duration> parseDuration(String key, Locale locale) {
    String durationProperty = messageSource.getMessage("%s.duration".formatted(key),
        EMPTY_PARAMETERS, null, locale);
    if (isNull(durationProperty)) {
      return Optional.empty();
    }
    try {
      return Optional.of(Duration.parse(durationProperty));
    } catch (DateTimeParseException e) {
      LOG.warn("Could not parse duration for key %s: %s".formatted(key, durationProperty));
      return Optional.empty();
    }
  }


  /**
   * Creates a toast notification with the contents found for the message key. This method produces
   * a routing toast with the link text read from the message properties file.
   *
   * <p>
   * The following message keys have to be present:
   * <ul>
   *   <li>{@code <key>.message.type}
   *   <li>{@code <key>.message.text}
   *   <li>{@code <key>.routing.link.text}
   * </ul>
   * Optionally, using messages, you can overwrite the closeable property for manual closeing the toast.
   * {@code <key>.cloaseable}
   * <p>
   * For more information please toast-notifications.properties
   *
   * @param key              the key for the messages
   * @param messageArgs      the parameters shown in the message
   * @param routeArgs        the parameters shown in the link text message
   * @param navigationTarget the navigation target to navigate to
   * @param routeParameters  the routing parameters used for navigation
   * @param locale           the locale for which to load the message
   * @return a Toast with loaded content
   * @see #create(String, Object[], Locale)
   */
  private Toast createRouting(String key, Object[] messageArgs, Object[] routeArgs,
      Class<? extends Component> navigationTarget,
      RouteParameters routeParameters, Locale locale) {
    var toast = create(key, messageArgs, locale);
    String linkText = parseLinkText(key, routeArgs, locale);
    return toast.withRouting(linkText, navigationTarget, routeParameters);
  }

  private String parseLinkText(String key, Object[] routeArgs, Locale locale) {
    String linkText;
    try {
      linkText = messageSource.getMessage("%s.routing.link.text".formatted(key),
          routeArgs, locale).strip();
    } catch (NoSuchMessageException e) {
      throw new RuntimeException("No link text specified for " + key, e);
    }
    return linkText;
  }


  private enum MessageType {
    HTML,
    TEXT
  }


}
