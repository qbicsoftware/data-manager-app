package life.qbic.datamanager.views.account;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import java.io.Serial;
import life.qbic.datamanager.views.general.dialog.AppDialog;
import life.qbic.datamanager.views.general.dialog.DialogBody;
import life.qbic.datamanager.views.general.dialog.DialogFooter;
import life.qbic.datamanager.views.general.dialog.DialogHeader;
import life.qbic.identity.application.mfa.RecoveryCodeSet;

/**
 * <b>Shows a freshly generated recovery-code set, once (AUTH-R-03)</b>
 *
 * <p>The {@link RecoveryCodeSet} it receives is the only place the plaintext codes ever exist. The
 * dialog therefore has a single "I have saved them" action and no cancel path: whether the user copied
 * the codes is not something the application can verify, so the only honest exit is the acknowledgment.
 * The codes are not stored in this component beyond the render, and nothing here writes them to a log,
 * a notification or the URL.</p>
 *
 * @since 1.0.0
 */
public class RecoveryCodesDialog extends Div {

  @Serial
  private static final long serialVersionUID = 1L;

  private final AppDialog dialog;

  public RecoveryCodesDialog(RecoveryCodeSet codeSet) {
    dialog = AppDialog.medium();
    DialogHeader.with(dialog, "Recovery codes");

    Paragraph warning = new Paragraph(
        "These codes are shown only once. Each code works one time. "
            + "Print them or store them in a password manager — anyone with a code can sign in.");

    Div grid = new Div();
    grid.addClassName("mfa-recovery-grid");
    codeSet.formattedCodes().forEach(code -> {
      Span span = new Span(code);
      span.addClassName("mfa-recovery-code");
      grid.add(span);
    });

    Div body = new Div(warning, grid);
    body.addClassName("mfa-recovery-body");
    DialogBody.withoutUserInput(dialog, body);

    DialogFooter.withConfirmOnly(dialog, "I have saved them");
    dialog.registerConfirmAction(dialog::close);
  }

  public void open() {
    dialog.open();
  }
}
