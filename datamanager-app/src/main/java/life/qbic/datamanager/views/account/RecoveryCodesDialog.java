package life.qbic.datamanager.views.account;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AttachmentType;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import java.io.ByteArrayInputStream;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.time.ZoneOffset;
import java.util.List;
import life.qbic.datamanager.views.general.CopyToClipBoardComponent;
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
 * <p>Because ten hand-transcribed codes are the most likely way to lose the only reset-free path into
 * the account (the helpdesk cannot reset by e-mail), the sheet offers two mechanical ways out: copy
 * the whole sheet to the clipboard, or download it as a plain-text file to print or file away. Both
 * render the same text, and neither is stored server-side — the text exists only in this component
 * and in the response body of one download.</p>
 *
 * @since 1.0.0
 */
public class RecoveryCodesDialog extends Div {

  @Serial
  private static final long serialVersionUID = 1L;

  /** Deliberately UTC and unambiguous: a sheet found months later should not need a timezone guess. */
  private static final DateTimeFormatter SHEET_TIMESTAMP =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneOffset.UTC);

  private static final DateTimeFormatter FILE_DATE =
      DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

  private static final String FILE_PREFIX = "data-manager-recovery-codes-";

  private final AppDialog dialog;

  /**
   * @param codeSet      the one-time plaintext set, straight from generation
   * @param accountEmail the account the codes belong to, so a printed sheet is identifiable
   */
  public RecoveryCodesDialog(RecoveryCodeSet codeSet, String accountEmail) {
    requireNonNull(codeSet, "codeSet must not be null");
    requireNonNull(accountEmail, "accountEmail must not be null");

    dialog = AppDialog.medium();
    DialogHeader.with(dialog, "Recovery codes");

    Paragraph warning = new Paragraph(
        "These codes are shown only once. Each code works one time. Download or copy them and keep "
            + "them somewhere safe — anyone with a code can sign in.");

    Span account = new Span("Account: " + accountEmail);
    account.addClassName("mfa-recovery-account");

    Div grid = new Div();
    grid.addClassName("mfa-recovery-grid");
    codeSet.formattedCodes().forEach(code -> {
      Span span = new Span(code);
      span.addClassName("mfa-recovery-code");
      grid.add(span);
    });

    Div actions = new Div(copyAction(codesAsText(codeSet, accountEmail)),
        downloadAction(codeSet, accountEmail));
    actions.addClassName("mfa-recovery-actions");

    Div body = new Div(warning, account, grid, actions);
    body.addClassName("mfa-recovery-body");
    DialogBody.withoutUserInput(dialog, body);

    DialogFooter.withConfirmOnly(dialog, "I have saved them");
    dialog.registerConfirmAction(dialog::close);
  }

  public void open() {
    dialog.open();
  }

  /**
   * Copies the whole sheet, not only the bare codes: the account and the timestamp are what make a
   * pasted sheet usable later, and a password-manager entry benefits from the same context.
   */
  private static CopyToClipBoardComponent copyAction(String sheetText) {
    CopyToClipBoardComponent copyAll = new CopyToClipBoardComponent(sheetText);
    copyAll.add(new Span("Copy all codes"));
    copyAll.addClassName("mfa-recovery-action");
    copyAll.getElement().setAttribute("title", "Copy the codes and their account to the clipboard");
    return copyAll;
  }

  /**
   * The download is a plain {@link Anchor} with a {@link DownloadHandler}: no round trip, no
   * JavaScript, so it behaves the same in every browser and for keyboard users.
   */
  private static Anchor downloadAction(RecoveryCodeSet codeSet, String accountEmail) {
    byte[] content = codesAsText(codeSet, accountEmail).getBytes(StandardCharsets.UTF_8);
    String fileName = fileName(codeSet);
    DownloadHandler handler = DownloadHandler.fromInputStream(event -> new DownloadResponse(
        new ByteArrayInputStream(content), fileName, "text/plain;charset=UTF-8", content.length));

    Anchor download = new Anchor();
    download.setHref(handler, AttachmentType.DOWNLOAD);
    download.add(VaadinIcon.DOWNLOAD.create(), new Span("Download as text file"));
    download.addClassName("mfa-recovery-action");
    return download;
  }

  /**
   * Renders the set as the sheet that is copied and downloaded.
   *
   * <p>Deliberately plain ASCII with {@code \n} line endings: the file is opened in whatever editor
   * or printer a user has, and a mangled dash is a support ticket on a break-glass path.</p>
   *
   * @param codeSet      the one-time plaintext set
   * @param accountEmail the account the codes belong to
   * @return the complete text of the sheet
   */
  static String codesAsText(RecoveryCodeSet codeSet, String accountEmail) {
    requireNonNull(codeSet, "codeSet must not be null");
    requireNonNull(accountEmail, "accountEmail must not be null");

    StringBuilder sheet = new StringBuilder();
    sheet.append("Data Manager - recovery codes\n");
    sheet.append("Account: ").append(accountEmail).append('\n');
    sheet.append("Generated: ").append(SHEET_TIMESTAMP.format(codeSet.generatedAt())).append('\n');
    sheet.append('\n');
    sheet.append("Each code works once, instead of a code from your authenticator app.\n");
    sheet.append("Anyone who has a code can sign in to this account.\n");
    sheet.append("Keep this file in a password manager and delete it once the codes are stored.\n");
    sheet.append('\n');
    List<String> codes = codeSet.formattedCodes();
    for (int index = 0; index < codes.size(); index++) {
      // Right-aligned numbers keep the printed column straight for a set of ten.
      sheet.append("%2d. %s\n".formatted(index + 1, codes.get(index)));
    }
    sheet.append('\n');
    sheet.append("New device? Sign in with one code, then set up the authenticator app again\n");
    sheet.append("under Settings > Two-factor authentication. A used code stays used.\n");
    return sheet.toString();
  }

  /**
   * @param codeSet the set the file belongs to
   * @return a dated file name, so a second set does not silently overwrite a kept first one
   */
  static String fileName(RecoveryCodeSet codeSet) {
    requireNonNull(codeSet, "codeSet must not be null");
    return FILE_PREFIX + FILE_DATE.format(codeSet.generatedAt()) + ".txt";
  }
}
