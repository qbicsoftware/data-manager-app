package life.qbic.datamanager.views.notifications;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog.CancelEvent;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog.ConfirmEvent;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog.RejectEvent;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
@Service
public class Notifications {

  private final MessageSource messageSource;

  public Notifications(MessageSource messageSource) {
    this.messageSource = requireNonNull(messageSource, "messageSource must not be null");
    ;
  }


  private enum NotificationType {
    SUCCESS, WARNING, ERROR, INFO
  }

  public DialogFactory dialog() {
    return new DialogFactory(messageSource);
  }

  public ToastFactory toast() {
    return new ToastFactory(messageSource);
  }


  private interface WithDynamicMessages<T extends WithDynamicMessages<T>> {

    T withDynamicMessage(String messageKey);

    T withDynamicMessage(String messageKey, Object[] messageArgs);
  }

  public static class DialogFactory implements WithDynamicMessages<DialogFactory> {

    private final NotificationDialog dialog;
    private NotificationType notificationType = NotificationType.INFO;
    private String messageKey;
    private Object[] messageArgs;
    private final MessageSource messageSource;
    private String definedTitle;
    private String definedMessage;

    private DialogFactory(MessageSource messageSource) {
      this.dialog = new NotificationDialog();
      this.messageArgs = new Object[0];
      this.messageSource = requireNonNull(messageSource, "messageSource must not be null");
    }

    public DialogFactory forSuccess() {
      this.notificationType = NotificationType.SUCCESS;
      return this;
    }

    public DialogFactory forWarning() {
      this.notificationType = NotificationType.WARNING;
      return this;
    }

    public DialogFactory forError() {
      this.notificationType = NotificationType.ERROR;
      return this;
    }

    public DialogFactory withConfirm(String text) {
      dialog.setConfirmText(text);
      return this;
    }

    public DialogFactory withConfirm(Component component) {
      dialog.setConfirmButton(component);
      return this;
    }

    public DialogFactory withConfirm(String confirmText, String theme) {
      dialog.setConfirmButton(confirmText, noop -> {
      }, theme);
      return this;
    }

    public DialogFactory onConfirmed(ComponentEventListener<ConfirmEvent> confirmListener) {
      dialog.addConfirmListener(confirmListener);
      return this;
    }

    public DialogFactory withCancel() {
      dialog.setCancelable(true);
      return this;
    }

    public DialogFactory withCancel(String text) {
      dialog.setCancelText(text);
      dialog.setCancelable(true);
      return this;
    }

    public DialogFactory withCancel(Component component) {
      dialog.setCancelButton(component);
      dialog.setCancelable(true);
      return this;
    }

    public DialogFactory withCancel(String cancelText, String theme) {
      dialog.setCancelButton(cancelText, noop -> {
      }, theme);
      dialog.setCancelable(true);
      return this;
    }

    public DialogFactory onCancelled(ComponentEventListener<CancelEvent> cancelListener) {
      dialog.addCancelListener(cancelListener);
      return this;
    }

    public DialogFactory withReject() {
      dialog.setRejectable(true);
      return this;
    }

    public DialogFactory withReject(String text) {
      dialog.setRejectText(text);
      dialog.setRejectable(true);
      return this;
    }

    public DialogFactory withReject(Component component) {
      dialog.setRejectButton(component);
      dialog.setRejectable(true);
      return this;
    }

    public DialogFactory withReject(String rejectText, String theme) {
      dialog.setRejectButton(rejectText, noop -> {
      }, theme);
      dialog.setRejectable(true);
      return this;
    }

    public DialogFactory onRejected(ComponentEventListener<RejectEvent> rejectListener) {
      dialog.addRejectListener(rejectListener);
      return this;
    }

    public DialogFactory forInformation() {
      this.notificationType = NotificationType.INFO;
      return this;
    }

    public DialogFactory withTitle(String title) {
      this.definedTitle = title;
      return this;
    }

    @Override
    public DialogFactory withDynamicMessage(String messageKey, Object[] messageArgs) {
      this.messageKey = requireNonNull(messageKey, "messageKey must not be null");
      this.messageArgs = requireNonNull(messageArgs, "messageArgs must not be null");
      return this;
    }

    @Override
    public DialogFactory withDynamicMessage(String messageKey) {
      this.messageKey = requireNonNull(messageKey, "messageKey must not be null");
      this.messageArgs = new Object[0];
      return this;
    }

    public DialogFactory withMessage(String message) {
      this.definedMessage = requireNonNull(message, "message must not be null");
      return this;
    }

    public NotificationDialog create() {
      dialog.addAttachListener(
          attachEvent -> loadMessage(attachEvent.getSource(), attachEvent.getUI().getLocale()));
      if (nonNull(definedMessage)) {
        dialog.content.removeAll();
        dialog.content.add(new Span(definedMessage));
      }
      if (nonNull(definedTitle)) {
        dialog.setTitle(definedTitle);
      }
      applyNotificationType();

      return this.dialog;
    }

    private void loadMessage(Component source, Locale locale) {
      if (source instanceof NotificationDialog notificationDialog) {
        if (isNull(messageKey)) {
          return;
        }
        var titleKey = messageKey + ".title";
        var contentKey = this.messageKey + ".message";
        String loadedTitle = messageSource.getMessage(titleKey, messageArgs, locale);
        String loadedContent = messageSource.getMessage(contentKey, messageArgs, locale);
        if (isNull(definedMessage)) {
          notificationDialog.content.removeAll();
          notificationDialog.content.add(new Html("<div>%s</div>".formatted(loadedContent)));
        }
        if (isNull(definedTitle)) {
          notificationDialog.setTitle(loadedTitle);
        }
      } else {
        throw new RuntimeException("Dialog attach event without attached dialog.");
      }
    }

    private void applyNotificationType() {
      var cssClass = switch (notificationType) {
        case SUCCESS -> "success-dialog";
        case WARNING -> "warning-dialog";
        case ERROR -> "error-dialog";
        case INFO -> "info-dialog";
      };
      dialog.addClassName(cssClass);
      var icon = switch (notificationType) {
        case SUCCESS -> VaadinIcon.CHECK.create();
        case WARNING -> VaadinIcon.WARNING.create();
        case ERROR -> VaadinIcon.CLOSE_CIRCLE.create();
        case INFO -> VaadinIcon.INFO.create();
      };
      var iconCssClass = switch (notificationType) {
        case SUCCESS -> "success-icon";
        case WARNING -> "warning-icon";
        case ERROR -> "error-icon";
        case INFO -> "info-icon";
      };
      icon.addClassName(iconCssClass);
      dialog.setHeaderIcon(icon);
    }
  }


  public static class ToastFactory implements WithDynamicMessages<ToastFactory> {

    private final MessageSource messageSource;
    private NotificationType notificationType = NotificationType.INFO;
    private String messageKey;
    private Object[] messageArgs;
    private String message;
    private Component content;

    private ToastFactory(MessageSource messageSource) {
      this.messageSource = messageSource;
    }

    public Notification create() {
      Notification notification = new Notification();
      notification.addOpenedChangeListener(
          it -> {
            if (it.isOpened()) {
              loadMessage(it.getSource(), UI.getCurrent().getLocale());
              refreshContent(it.getSource());
            }
          });
      notification.setPosition(Position.BOTTOM_START);
      notification.setDuration(8_000);
      refreshContent(notification);
      return notification;
    }


    @Override
    public ToastFactory withDynamicMessage(String messageKey) {
      this.messageKey = messageKey;
      this.messageArgs = new Object[0];
      return this;
    }

    @Override
    public ToastFactory withDynamicMessage(String messageKey, Object[] messageArgs) {
      this.messageKey = messageKey;
      this.messageArgs = messageArgs;
      return this;
    }

    public ToastFactory withContent(Component content) {
      this.content = content;
      return this;
    }

    private void refreshContent(Notification notification) {
      notification.removeAll();
      if (nonNull(content)) {
        notification.add(content);
        return;
      }
      var messageComp = new Html("<div>%s</div>".formatted(message));
      notification.add(messageComp);
    }

    private void loadMessage(Component source, Locale locale) {
      System.out.println("loading message for " + source);
      if (source instanceof Notification) {
        if (isNull(messageKey)) {
          return;
        }
        var contentKey = this.messageKey + ".message";
        this.message = messageSource.getMessage(contentKey, messageArgs, locale);
      }

    }
  }


}
