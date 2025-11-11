package life.qbic.datamanager.views.notifications;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog.ConfirmEvent;
import com.vaadin.flow.component.html.Span;
import java.util.Locale;
import java.util.function.Consumer;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Component;

@Component
public class CancelConfirmationDialogFactory {

  private static final MessageType DEFAULT_MESSAGE_TYPE = MessageType.HTML; //set to html as text works with it too
  private static final String DEFAULT_TITLE = "Discard Changes?";
  private static final String DEFAULT_CONTENT = "By aborting the editing process and closing the dialog, you will lose all information entered.";
  private static final String DEFAULT_CONFIRM_TEXT = "Discard Changes";
  private static final Object[] EMPTY_PARAMETERS = new Object[]{};
  private static final Logger log = LoggerFactory.logger(CancelConfirmationDialogFactory.class);
  private final MessageSource messageSource;


  public CancelConfirmationDialogFactory(MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  private static com.vaadin.flow.component.Component createContentComponent(MessageType contentType,
      String contentText) {
    return switch (contentType) {
      case HTML -> new Html("<div style=\"display:contents\">%s</div>".formatted(contentText));
      case TEXT -> new Span(contentText);
    };
  }

  /**
   * Creates an instance of an {@link NotificationDialog} with title and message based on the
   * provided key.
   *
   * @param key The message key to determine the content and title of the {@link NotificationDialog}.
   * @param locale
   * @return the notification dialog
   * @since
   */
  public NotificationDialog cancelConfirmationDialog(String key, Locale locale) {
    return buildDialog(key, locale);
  }

  public NotificationDialog cancelConfirmationDialog(Consumer<ConfirmEvent> onCancelConfirmed,
      String key, Locale locale) {
    var confirmCancelDialog = buildDialog(key, locale);
    confirmCancelDialog.addCancelListener(event -> event.getSource().close());

    confirmCancelDialog.addConfirmListener(event -> {
      event.getSource().close();
      onCancelConfirmed.accept(event);
    });
    return confirmCancelDialog;
  }

  private NotificationDialog buildDialog(String key, Locale locale) {
    String title = parseTitle(key, locale);
    MessageType contentType = parseMessageType(key, locale);
    String contentText = parseMessageText(key, locale);
    String confirmText = parseConfirmText(key, locale);
    var content = createContentComponent(contentType, contentText);

    NotificationDialog confirmCancelDialog = NotificationDialog.warningDialog()
        .withTitle(title)
        .withContent(content);
    confirmCancelDialog.setCancelable(true);
    confirmCancelDialog.setCancelText("Continue Editing");
    Button redButton = new Button(confirmText);
    redButton.addClassName("danger");
    confirmCancelDialog.setConfirmButton(redButton);
    return confirmCancelDialog;
  }

  private String parseConfirmText(String key, Locale locale) {
    return messageSource.getMessage("%s.cancel-confirmation.confirm-text".formatted(key),
        new Object[]{}, DEFAULT_CONFIRM_TEXT, locale);
  }

  private String parseMessageText(String key, Locale locale) {
    return messageSource.getMessage("%s.cancel-confirmation.message.text".formatted(key),
        new Object[]{}, DEFAULT_CONTENT, locale);
  }

  private String parseTitle(String key, Locale locale) {
    return messageSource.getMessage("%s.cancel-confirmation.title".formatted(key),
        new Object[]{}, DEFAULT_TITLE, locale);
  }

  private MessageType parseMessageType(String key, Locale locale) {
    try {
      String messageType = messageSource.getMessage(
          "%s.cancel-confirmation.message.type".formatted(key),
          EMPTY_PARAMETERS, locale).strip().toUpperCase();
      return MessageType.valueOf(messageType);
    } catch (NoSuchMessageException e) {
      log.warn("No message type specified for %s.".formatted(key));
      return DEFAULT_MESSAGE_TYPE;
    }
  }

  private enum MessageType {
    HTML,
    TEXT
  }
}
