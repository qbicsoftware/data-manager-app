package life.qbic.datamanager.views.notifications;

import static java.util.Objects.isNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.spring.annotation.SpringComponent;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;
import life.qbic.logging.api.Logger;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
@SpringComponent
public class MessageSourceNotificationFactory {

  private static final Logger log = logger(MessageSourceNotificationFactory.class);
  public static final Object[] EMPTY_PARAMETERS = new Object[]{};
  private static final String DEFAULT_CONFIRM_TEXT = "Okay";
  private final MessageSource messageSource;

  public MessageSourceNotificationFactory(MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  public Toast toast(String key, Object[] parameters, Locale locale) {
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

  public Toast routingToast(String key, Object[] messageArgs, Object[] routeArgs,
      Class<? extends Component> navigationTarget, RouteParameters routeParameters, Locale locale) {
    var toast = toast(key, messageArgs, locale);
    String linkText = parseLinkText(key, routeArgs, locale);
    return toast.withRouting(linkText, navigationTarget, routeParameters);
  }

  public NotificationDialog dialog(String key, Object[] parameters, Locale locale) {
    MessageType type = parseMessageType(key, locale);
    String messageText = parseMessage(key, parameters, locale);
    Component content = switch (type) {
      case HTML -> new Html("<div style=\"display:contents\">%s</div>".formatted(messageText));
      case TEXT -> new Span(messageText);
    };

    NotificationLevel level = parseLevel(key, locale);
    NotificationDialog notificationDialog = new NotificationDialog(level)
        .withContent(content);
    parseTitle(key, locale).ifPresent(notificationDialog::withTitle);
    parseConfirmText(key, locale).ifPresentOrElse(
        notificationDialog::setConfirmText,
        () -> notificationDialog.setConfirmText(DEFAULT_CONFIRM_TEXT));

    return notificationDialog;
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

  private Optional<String> parseTitle(String key, Locale locale) {
    try {
      return Optional.of(messageSource.getMessage("%s.title".formatted(key),
          EMPTY_PARAMETERS, locale).strip());
    } catch (NoSuchMessageException e) {
      log.warn("No title specified for %s.title".formatted(key));
      return Optional.empty();
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
      log.warn("Could not parse duration for key %s: %s".formatted(key, durationProperty));
      return Optional.empty();
    }
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

  private Optional<String> parseConfirmText(String key, Locale locale) {
    return Optional.ofNullable(messageSource.getMessage("%s.confirm-text".formatted(key),
        new Object[]{}, null, locale));
  }

  private enum MessageType {
    HTML,
    TEXT
  }
}
