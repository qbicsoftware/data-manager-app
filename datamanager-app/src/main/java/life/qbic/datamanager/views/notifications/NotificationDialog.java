package life.qbic.datamanager.views.notifications;

import com.vaadin.flow.component.Component;
import life.qbic.datamanager.views.general.dialog.AlertDialog;

/**
 * <b>Deprecated: NotificationDialog</b>
 *
 * <p>This class has been removed. Use {@link life.qbic.datamanager.views.general.dialog.AlertDialog}
 * instead. See {@code front-end-components.md} for the migration guide.</p>
 *
 * @deprecated Use {@link life.qbic.datamanager.views.general.dialog.AlertDialog} directly.
 *             This class is a placeholder for backward compatibility only and should not be
 *             instantiated. It will be removed in a future version.
 * @since 1.12.0
 */
@Deprecated(since = "1.12.0", forRemoval = true)
public final class NotificationDialog {

  private final NotificationLevel level;

  protected NotificationDialog(NotificationLevel level) {
    this.level = level;
  }

  /**
   * @deprecated This method throws UnsupportedOperationException. Use {@link AlertDialog} instead.
   */
  @Deprecated(since = "1.12.0", forRemoval = true)
  public static NotificationDialog errorDialog() {
    throw new UnsupportedOperationException(
        "NotificationDialog.errorDialog() has been removed. "
        + "Use AlertDialog.alert(parent).error().title(...).message(...).confirmButton(...).build().open()");
  }

  /**
   * @deprecated This method throws UnsupportedOperationException. Use {@link AlertDialog} instead.
   */
  @Deprecated(since = "1.12.0", forRemoval = true)
  public static NotificationDialog warningDialog() {
    throw new UnsupportedOperationException(
        "NotificationDialog.warningDialog() has been removed. "
        + "Use AlertDialog.alert(parent).warning().title(...).message(...).build().open()");
  }

  /**
   * @deprecated This method throws UnsupportedOperationException. Use {@link AlertDialog} instead.
   */
  @Deprecated(since = "1.12.0", forRemoval = true)
  public <T extends NotificationDialog> T withTitle(String text) {
    throw new UnsupportedOperationException(
        "NotificationDialog.withTitle() has been removed. Use AlertDialog instead.");
  }

  /**
   * @deprecated This method throws UnsupportedOperationException. Use {@link AlertDialog} instead.
   */
  @Deprecated(since = "1.12.0", forRemoval = true)
  public <T extends NotificationDialog> T withContent(Component content) {
    throw new UnsupportedOperationException(
        "NotificationDialog.withContent() has been removed. Use AlertDialog instead.");
  }

  /**
   * @deprecated This method throws UnsupportedOperationException. Use {@link AlertDialog} instead.
   */
  @Deprecated(since = "1.12.0", forRemoval = true)
  public void setConfirmText(String text) {
    throw new UnsupportedOperationException(
        "NotificationDialog.setConfirmText() has been removed. Use AlertDialog instead.");
  }

  /**
   * @deprecated This method throws UnsupportedOperationException. Use {@link AlertDialog} instead.
   */
  @Deprecated(since = "1.12.0", forRemoval = true)
  public void open() {
    throw new UnsupportedOperationException(
        "NotificationDialog.open() has been removed. Use AlertDialog instead.");
  }
}
