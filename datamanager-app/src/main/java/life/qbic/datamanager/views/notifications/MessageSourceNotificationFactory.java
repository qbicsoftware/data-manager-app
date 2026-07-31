package life.qbic.datamanager.views.notifications;

import static java.util.Objects.isNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.progressbar.ProgressBar;
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
 * Notifications created by this factory can be shown to the user. There are multiply types of
 * notifications.
 * <ul>
 *   <li>Toast notification</li>
 *   <li>Notification dialog</li>
 * </ul>
 * <b>Toasts</b> are notifications shown to the user only briefly in a non-blocking way.
 * The user can choose to ignore them and won't be disturbed in what he/she is doing.
 * <p>
 * <b>Notification dialogs</b> are notifications shown to the user in a blocking way. The user has to interact with the dialog in order to continue working.
 * <p>
 * Notification dialogs show cases of error or warning notifications. Toast show informational and success notifications.
 */
@SpringComponent
public class MessageSourceNotificationFactory {

  public static final Object[] EMPTY_PARAMETERS = new Object[]{};
  private static final Logger log = logger(MessageSourceNotificationFactory.class);
  private static final String DEFAULT_CONFIRM_TEXT = "Okay";
  private static final NotificationLevel DEFAULT_LEVEL = NotificationLevel.INFO;
  private static final MessageType DEFAULT_MESSAGE_TYPE = MessageType.HTML;
  private final MessageSource messageSource;

  public MessageSourceNotificationFactory(MessageSource messageSource) {
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
   * For more information please the toast-notifications.properties
   *
   * @param key        the key for the messages
   * @param parameters the parameters shown in the message
   * @param locale     the locale for which to load the message
   * @return a Toast with loaded content
   * @see #routingToast(String, Object[], Object[], Class, RouteParameters, Locale)
   */
  public Toast toast(String key, Object[] parameters, Locale locale) {
    MessageType type = parseMessageType(key, locale);
    String messageText = parseMessage(key, parameters, locale);

    Component content = switch (type) {
      case HTML -> new Html("<div>%s</div>".formatted(messageText));
      case TEXT -> new Span(messageText);
    };

    NotificationLevel level = parseLevel(key, locale);
    Duration duration = parseDuration(key, locale).orElse(Toast.DEFAULT_OPEN_DURATION);

    Toast toast = new Toast(level);
    toast.withLevelIcon(level);
    toast.withContent(content);
    toast.setDuration(duration);

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
   * <p>
   * For more information please see toast-notifications.properties
   *
   * @param key              the key for the messages
   * @param messageArgs      the parameters shown in the message
   * @param routeArgs        the parameters shown in the link text message
   * @param navigationTarget the navigation target to navigate to
   * @param routeParameters  the routing parameters used for navigation
   * @param locale           the locale for which to load the message
   * @return a Toast with loaded content
   * @see #toast(String, Object[], Locale)
   */
  public Toast routingToast(String key, Object[] messageArgs, Object[] routeArgs,
      Class<? extends Component> navigationTarget, RouteParameters routeParameters, Locale locale) {
    var toast = toast(key, messageArgs, locale);
    String linkText = parseLinkText(key, routeArgs, locale);
    return toast.withRouting(linkText, navigationTarget, routeParameters);
  }

  /**
   * Creates a toast that indicates a pending task. The display duration is set to
   * {@link Duration#ZERO}, since it is the client's job to close the toast explicitly after the
   * pending task has finished.
   * <p>
   * The toast uses the progress layout (no icon) with:
   * <ul>
   *   <li>Title (from message.text) in 18px bold</li>
   *   <li>Indeterminate progress bar</li>
   *   <li>Optional subtext (from message.subtext) in 16px regular</li>
   * </ul>
   * <p>
   * For more information please see toast-notifications.properties
   *
   * @param key         the key for the messages
   * @param messageArgs the parameters shown in the message
   * @param locale      the locale for which to load the message
   * @return a Toast with progress indicator
   */
  public Toast pendingTaskToast(String key, Object[] messageArgs, Locale locale) {
    String messageText = parseMessage(key, messageArgs, locale);
    NotificationLevel level = parseLevel(key, locale);

    Toast toast = new Toast(level);
    toast.withTitle(messageText);
    parseSubtext(key, locale).ifPresent(toast::withSubtext);

    var progressBar = new ProgressBar();
    progressBar.setIndeterminate(true);
    toast.withProgressBar(progressBar);
    toast.setDuration(Duration.ZERO);

    return toast;
  }

  /**
   * Creates a toast with an action button (e.g., "Retry", "Try Again") for error scenarios.
   * Useful for error toasts where the user can take corrective action.
   * <p>
   * The toast includes:
   * <ul>
   *   <li>Error icon (red close-circle)</li>
   *   <li>Error message</li>
   *   <li>Action button (styled with #66A8FF)</li>
   *   <li>Close button</li>
   * </ul>
   *
   * @param key              the key for the messages
   * @param parameters       the parameters shown in the message
   * @param actionLabel      the action button label (e.g., "Try Again")
   * @param actionListener   the click handler for the action button
   * @param locale           the locale for which to load the message
   * @return a Toast with an action button
   */
  public Toast actionToast(String key, Object[] parameters, String actionLabel,
      com.vaadin.flow.component.ComponentEventListener<com.vaadin.flow.component.ClickEvent<com.vaadin.flow.component.button.Button>> actionListener,
      Locale locale) {
    Toast toast = toast(key, parameters, locale);
    toast.withAction(actionLabel, actionListener);
    return toast;
  }

  /**
   * Parses an optional subtext key for progress toasts.
   */
  private Optional<String> parseSubtext(String key, Locale locale) {
    try {
      return Optional.of(messageSource.getMessage("%s.message.subtext".formatted(key),
          new Object[]{}, locale).strip());
    } catch (NoSuchMessageException e) {
      log.debug("No subtext specified for %s.message.subtext".formatted(key));
      return Optional.empty();
    }
  }

  private NotificationLevel parseLevel(String key, Locale locale) {
    String levelProperty;
    try {
      levelProperty = messageSource.getMessage("%s.level".formatted(key),
          EMPTY_PARAMETERS, locale);
    } catch (NoSuchMessageException e) {
      log.warn(
          "Missing message level for `%s.level` falling back to the default of `%s`".formatted(key,
              DEFAULT_LEVEL));
      return DEFAULT_LEVEL;
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
      throw new RuntimeException("No message specified for `%s.message.text`".formatted(key), e);
    }
  }

  private MessageType parseMessageType(String key, Locale locale) {
    try {
      String messageType = messageSource.getMessage("%s.message.type".formatted(key),
          EMPTY_PARAMETERS, locale).strip().toUpperCase();
      return MessageType.valueOf(messageType);
    } catch (NoSuchMessageException e) {
      log.warn("No message type specified for `%s.message.type`. Falling back to %s".formatted(key,
          DEFAULT_MESSAGE_TYPE));
      log.debug("No message type specified for `%s.message.type`. Falling back to %s".formatted(key,
          DEFAULT_MESSAGE_TYPE), e);
      return DEFAULT_MESSAGE_TYPE;
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

  private enum MessageType {
    HTML,
    TEXT
  }
}
