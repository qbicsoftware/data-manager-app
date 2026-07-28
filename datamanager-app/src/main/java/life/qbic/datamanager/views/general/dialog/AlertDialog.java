package life.qbic.datamanager.views.general.dialog;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import java.util.Objects;
import life.qbic.datamanager.views.general.icon.IconFactory;

/**
 * <b>Alert Dialog</b>
 *
 * <p>A centralized dialog for confirming destructive actions and showing
 * warnings or errors. Implements the UX designer's Alert Dialog pattern:</p>
 * <ul>
 *   <li>Minimal content — icon, title, short description</li>
 *   <li>Intent-driven styling — icon colour and confirm-button style
 *       reflect severity ({@link Intent})</li>
 *   <li>1–2 actions maximum (cancel + confirm, or confirm-only)</li>
 *   <li>No complex layouts or scrolling</li>
 * </ul>
 *
 * <p>Uses {@link AppDialog#small()} as its underlying dialog. Icon is
 * styled via {@link IconFactory} for the WARNING intent and
 * {@link VaadinIcon} for ERROR/INFO. The danger-style confirm button
 * leverages the existing {@code .button-danger} CSS class and
 * {@link DialogFooter#withDangerousConfirm}.</p>
 *
 * <p>Two entry points:</p>
 * <pre>{@code
 * // Shortcut for the most common case: danger-intent confirm dialog
 * AlertDialog.danger(parent, "Remove dataset?", "This will disconnect…", onRemove).open();
 *
 * // Fluent builder for full control
 * AlertDialog.alert(parent)
 *     .danger()
 *     .title("Remove dataset?")
 *     .message("This will disconnect…")
 *     .confirmButton("Remove", onRemove)
 *     .cancelButton("Cancel", onCancel)
 *     .build().open();
 * }</pre>
 *
 * <p><b>When to use:</b></p>
 * <ul>
 *   <li>{@code AlertDialog} — for <em>action confirmation</em> (destructive,
 *       irreversible ops). Uses {@link AppDialog} which has the
 *       {@code BeforeLeaveObserver} pattern for unsaved-changes protection.</li>
 *   <li>{@code MessageSourceNotificationFactory.dialog()} — for
 *       <em>informational</em> pop-ups (warnings, errors, info notices
 *       that don't require a destructive action).</li>
 * </ul>
 *
 * @since 1.12.0
 */
public class AlertDialog {

  /**
   * Indicates the severity and visual treatment of an {@linkplain AlertDialog alert}.
   */
  public enum Intent {
    /** Destructive action (red confirm button). */
    DANGER,
    /** Non-destructive but important confirmation. */
    WARNING,
    /** Something went wrong. */
    ERROR,
    /** Informational notice. */
    INFO
  }

  /**
   * Builder for constructing {@link AlertDialog}s with explicit control
   * over intent, title, message, and actions.
   *
   * @since 1.12.0
   */
  public static class Builder {

    private Intent intent = Intent.WARNING;
    private String title;
    private String message;
    private String confirmLabel;
    private DialogAction confirmAction;
    private String cancelLabel;
    private DialogAction cancelAction;

    private Builder() {
    }

    /**
     * Sets the intent explicitly.
     */
    public Builder intent(Intent intent) {
      this.intent = Objects.requireNonNull(intent, "intent must not be null");
      return this;
    }

    /** Shorthand for {@code intent(Intent.DANGER)}. */
    public Builder danger() {
      return intent(Intent.DANGER);
    }

    /** Shorthand for {@code intent(Intent.WARNING)}. */
    public Builder warning() {
      return intent(Intent.WARNING);
    }

    /** Shorthand for {@code intent(Intent.ERROR)}. */
    public Builder error() {
      return intent(Intent.ERROR);
    }

    /** Shorthand for {@code intent(Intent.INFO)}. */
    public Builder info() {
      return intent(Intent.INFO);
    }

    /** Sets the dialog title (header text next to the icon). */
    public Builder title(String title) {
      this.title = Objects.requireNonNull(title, "title must not be null");
      return this;
    }

    /** Sets the body message shown below the title. */
    public Builder message(String message) {
      this.message = Objects.requireNonNull(message, "message must not be null");
      return this;
    }

    /**
     * Configures the confirm button label and action. At least one call
     * to {@link #confirmButton(String, DialogAction)} is required.
     *
     * @throws NullPointerException if either argument is null
     */
    public Builder confirmButton(String label, DialogAction action) {
      this.confirmLabel = Objects.requireNonNull(label, "confirm label must not be null");
      this.confirmAction = Objects.requireNonNull(action, "confirm action must not be null");
      return this;
    }

    /**
     * Configures the cancel button label and action. Optional — omitting
     * this produces a confirm-only dialog.
     *
     * @throws NullPointerException if either argument is null
     */
    public Builder cancelButton(String label, DialogAction action) {
      this.cancelLabel = Objects.requireNonNull(label, "cancel label must not be null");
      this.cancelAction = Objects.requireNonNull(action, "cancel action must not be null");
      return this;
    }

    /**
     * Builds the {@link AlertDialog}, composing the underlying
     * {@link AppDialog} with all configured properties.
     *
     * @throws IllegalStateException if no confirm button has been set
     */
    public AlertDialog build() {
      Objects.requireNonNull(confirmLabel,
          "confirmButton(label, action) must be set before building");
      Objects.requireNonNull(confirmAction,
          "confirmButton(label, action) must be set before building");

      var dialog = AppDialog.small();

      // Icon — intent-driven colour via IconFactory for WARNING/DANGER,
      // colour-neutral for ERROR/INFO.
      var icon = resolveIcon();

      DialogHeader.withIcon(dialog, title, icon);

      // Body — message text wrapped in a span so Vaadin does not interpret
      // HTML; the existing dialog styling handles typography.
      var messageDiv = new Div();
      messageDiv.add(new Span(message));
      DialogBody.withoutUserInput(dialog, messageDiv);

      // Footer — danger intent gets a red confirm button via the existing
      // withDangerousConfirm helper; other intents use the default primary
      // style. If no cancel action is set, the builder skips the cancel
      // button (single-action confirmation).
      if (intent == Intent.DANGER && cancelLabel != null && cancelAction != null) {
        DialogFooter.withDangerousConfirm(dialog, cancelLabel, confirmLabel);
      } else if (cancelLabel != null && cancelAction != null) {
        DialogFooter.with(dialog, cancelLabel, confirmLabel);
      } else {
        DialogFooter.withConfirmOnly(dialog, confirmLabel);
      }

      // Actions
      dialog.registerConfirmAction(() -> {
        dialog.close();
        confirmAction.execute();
      });
      if (cancelAction != null) {
        dialog.registerCancelAction(() -> {
          dialog.close();
          cancelAction.execute();
        });
      }

      return new AlertDialog(dialog);
    }

    /**
     * Selects the intent-appropriate icon. WARNING and DANGER share the
     * same orange exclamation-circle icon (from {@link IconFactory});
     * ERROR and INFO use semantically distinct icons.
     */
    private Icon resolveIcon() {
      return switch (intent) {
        case DANGER, WARNING -> IconFactory.warningIcon();
        case ERROR -> {
          var icon = VaadinIcon.CLOSE_CIRCLE.create();
          icon.addClassName("icon-color-warning");
          icon.addClassName("icon-size-m");
          yield icon;
        }
        case INFO -> {
          var icon = VaadinIcon.INFO_CIRCLE.create();
          icon.addClassName("icon-size-m");
          yield icon;
        }
      };
    }
  }

  private final AppDialog dialog;

  private AlertDialog(AppDialog dialog) {
    this.dialog = Objects.requireNonNull(dialog, "dialog must not be null");
  }

  /**
   * Opens the dialog (modal on top of the current UI).
   */
  public void open() {
    dialog.open();
  }

  /**
   * Closes the dialog without dispatching the confirm action.
   */
  public void close() {
    dialog.close();
  }

  /**
   * Returns the underlying {@link AppDialog}, for cases where the
   * caller needs a reference (e.g. to add a {@code BeforeLeaveObserver}
   * or attach to a specific component tree).
   */
  public AppDialog dialog() {
    return dialog;
  }

  // ── Static factory method: Builder entry point ────────────────────────────

  /**
   * Entry point for the fluent builder. Call this first, then chain
   * {@code .danger()}/{@code .title()}/{@code .message()}/etc., ending
   * with {@link Builder#build()}.
   *
   * <p>The {@code parent} parameter is reserved for future use (e.g.
   * overlay parent). Not used currently but required by the design for
   * explicit ownership.</p>
   */
  public static Builder alert(Component parent) {
    Objects.requireNonNull(parent, "parent must not be null");
    return new Builder();
  }

  // ── Static factory: danger-intent shortcut ────────────────────────────────

  /**
   * Builds and returns a pre-configured {@link AlertDialog} for the
   * destructive-action pattern: orange warning icon, red "danger"
   * confirm button, a cancel button labelled "Cancel" that simply
   * closes the dialog, and a body message with the given text.
   *
   * <p>The cancel action only closes the dialog (no external side-effect).
   * If the caller needs a custom cancel handler, use {@link #alert(Component)}
   * and {@code .cancelButton(...)} explicitly.</p>
   *
   * <p>Usage:</p>
   * <pre>{@code
   *   AlertDialog.danger(this,
   *       "Remove dataset connection?",
   *       "This will disconnect the dataset from the project.",
   *       () -> performRemove())
   *       .open();
   * }</pre>
   *
   * @param parent     the component that owns this dialog (for UI tree
   *                   attachment)
   * @param title      the header text next to the icon
   * @param message    the body description
   * @param onConfirm  the action invoked when the user confirms
   * @return a ready-to-{@code open()} AlertDialog
   * @throws NullPointerException if any argument is null
   */
  public static AlertDialog danger(
      Component parent,
      String title,
      String message,
      DialogAction onConfirm) {
    return alert(parent)
        .danger()
        .title(title)
        .message(message)
        .confirmButton("Confirm", onConfirm)
        .cancelButton("Cancel", () -> { /* dismiss only */ })
        .build();
  }
}
