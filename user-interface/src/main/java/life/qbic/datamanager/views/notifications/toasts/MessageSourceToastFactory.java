package life.qbic.datamanager.views.notifications.toasts;

import static java.util.Objects.nonNull;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.RouteParameters;
import java.io.Serializable;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Locale;
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
  public Toast create(String key, Object[] parameters, Locale locale) {
    MessageType type;
    try {
      String messageType = messageSource.getMessage("%s.message.type".formatted(key),
          EMPTY_PARAMETERS, locale).strip().toUpperCase();
      type = MessageType.valueOf(messageType);
    } catch (NoSuchMessageException e) {
      throw new RuntimeException("No message type specified for " + key, e);
    }
    String messageText;
    try {
      messageText = messageSource.getMessage("%s.message.text".formatted(key),
          parameters, locale).strip();
    } catch (NoSuchMessageException e) {
      throw new RuntimeException("No message specified for " + key, e);
    }

    Component content = switch (type) {
      case HTML -> new Html("<div style=\"display:contents\">%s</div>".formatted(messageText));
      case TEXT -> new Span(messageText);
    };
    Toast toast = Toast.create(content);

    String closeableProperty = messageSource.getMessage("%s.closeable".formatted(key), parameters,
        null, locale);
    if (nonNull(closeableProperty)) {
      toast.setCloseable(Boolean.parseBoolean(closeableProperty));
    }

    String durationProperty = messageSource.getMessage("%s.duration".formatted(key),
        EMPTY_PARAMETERS, null, locale);
    if (nonNull(durationProperty)) {
      if (!durationProperty.startsWith("P")) {
        durationProperty = "P" + durationProperty;
      }
      try {
        toast.setDuration(Duration.parse(durationProperty));
      } catch (DateTimeParseException e) {
        LOG.error("Could not parse duration for key %s: %s".formatted(key, durationProperty), e);
      }
    }

    return toast;
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
  public Toast createRouting(String key, Object[] messageArgs, Object[] routeArgs,
      Class<? extends Component> navigationTarget,
      RouteParameters routeParameters, Locale locale) {
    var toast = create(key, messageArgs, locale);
    String linkText;
    try {
      linkText = messageSource.getMessage("%s.routing.link.text".formatted(key),
          routeArgs, locale).strip();
    } catch (NoSuchMessageException e) {
      throw new RuntimeException("No link text specified for " + key, e);
    }
    return toast.withRouting(linkText, navigationTarget, routeParameters);
  }


  private enum MessageType {
    HTML,
    TEXT
  }


}
