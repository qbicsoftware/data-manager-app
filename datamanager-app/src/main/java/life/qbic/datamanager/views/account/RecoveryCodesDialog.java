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
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import life.qbic.datamanager.views.general.CopyToClipboardButton;
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

  private static final DateTimeFormatter COPY_CONFIRMATION_TIME =
      DateTimeFormatter.ofPattern("HH:mm:ss");

  private static final String FILE_PREFIX = "data-manager-recovery-codes-";

  /**
   * Why the sheet is on screen, which decides how hard the warning is.
   *
   * @since 1.0.0
   */
  public enum Reason {
    /**
     * Shown right after the first factor was confirmed. The user may not cancel: this is the only
     * moment at which they are guaranteed to be looking, and the codes are the only self-service way
     * back in.
     */
    FIRST_FACTOR,
    /** The user asked for a new set on the settings page; they can back out. */
    REGENERATED
  }

  private final AppDialog dialog;

  /**
   * @param codeSet      the one-time plaintext set, straight from generation
   * @param accountEmail the account the codes belong to, so a printed sheet is identifiable
   */
  public RecoveryCodesDialog(RecoveryCodeSet codeSet, String accountEmail) {
    this(codeSet, accountEmail, Reason.REGENERATED);
  }

  /**
   * @param codeSet      the one-time plaintext set, straight from generation
   * @param accountEmail the account the codes belong to, so a printed sheet is identifiable
   * @param reason       why the sheet is shown; decides whether backing out is allowed and how loudly
   *                     the lockout consequence is stated
   */
  public RecoveryCodesDialog(RecoveryCodeSet codeSet, String accountEmail, Reason reason) {
    requireNonNull(codeSet, "codeSet must not be null");
    requireNonNull(accountEmail, "accountEmail must not be null");
    requireNonNull(reason, "reason must not be null");
    boolean firstFactor = reason == Reason.FIRST_FACTOR;

    dialog = AppDialog.medium();
    DialogHeader.with(dialog, firstFactor ? "Save your recovery codes" : "Recovery codes");

    Paragraph warning = new Paragraph(firstFactor
        ? "Your authenticator app is set up. Before you finish, save these recovery codes."
        : "These codes are shown only once. Each code works one time. Download or copy them and keep "
            + "them somewhere safe — anyone with a code can sign in.");

    Div body = new Div(warning);
    if (firstFactor) {
      Paragraph lockout = new Paragraph(firstFactorLockoutWarning());
      lockout.addClassName("mfa-recovery-lockout");
      body.add(lockout);
    }

    Span account = new Span("Account: " + accountEmail);
    account.addClassName("mfa-recovery-account");

    Div grid = new Div();
    grid.addClassName("mfa-recovery-grid");
    codeSet.formattedCodes().forEach(code -> {
      Span span = new Span(code);
      span.addClassName("mfa-recovery-code");
      grid.add(span);
    });

    Div actions = new Div();
    actions.addClassName("mfa-recovery-actions");
    // Invisible, but not silent: the check mark tells the eye, this tells a screen reader. It is a
    // sibling of the buttons, never a state of them, and it carries a time stamp so repeated copies
    // are announced each time instead of being swallowed as "the same text again".
    Span copyStatus = new Span();
    copyStatus.addClassName("mfa-recovery-feedback");
    copyStatus.getElement().setAttribute("role", "status");
    copyStatus.getElement().setAttribute("aria-live", "polite");
    actions.add(copyAction(codesAsText(codeSet, accountEmail), copyStatus),
        downloadAction(codeSet, accountEmail), copyStatus);

    body.add(account, grid, actions);
    body.addClassName("mfa-recovery-body");
    DialogBody.withoutUserInput(dialog, body);

    // The first-factor sheet has no way out but the acknowledgement. Whether the user copied the
    // codes is not something the application can verify, so the only honest exit is "I have saved
    // them" — and offering a cancel would let the one guaranteed moment pass silently.
    DialogFooter.withConfirmOnly(dialog,
        firstFactor ? "I have saved my recovery codes" : "I have saved them");
    dialog.registerConfirmAction(dialog::close);
  }

  /**
   * The one sentence on the first-factor sheet that decides whether the user keeps the codes.
   *
   * <p>It has to say three things and no more: losing both means no self-service way in; the helpdesk
   * can help, but only after verifying identity in person; and there is no e-mail link that skips the
   * second step. The last part matters because "contact support" alone reads as "they will reset it
   * by mail", which is exactly the path the milestone forbids (mailbox control must never be the way
   * past the factor).</p>
   *
   * <p>Tested in {@code RecoveryCodesDialogSpec}: the wording is a security statement, not styling,
   * so it is pinned rather than left to drift.</p>
   *
   * @return the warning text shown right after the first factor is confirmed
   */
  static String firstFactorLockoutWarning() {
    return "If you lose access to your app and these codes, you cannot sign in by yourself. Your QBiC "
        + "contact can only help after verifying who you are in person. There is no e-mail link "
        + "that skips the second step.";
  }

  public void open() {
    dialog.open();
  }

  /**
   * Copies the whole sheet, not only the bare codes: the account and the timestamp are what make a
   * pasted sheet usable later, and a password-manager entry benefits from the same context.
   *
   * <p>A {@link CopyToClipboardButton}, not the icon-switching
   * {@link life.qbic.datamanager.views.general.CopyToClipBoardComponent}: the latter left this action
   * looking used up after one click on a deployment. See the button's class documentation.</p>
   */
  private static CopyToClipboardButton copyAction(String sheetText, Span copyStatus) {
    CopyToClipboardButton copyAll = new CopyToClipboardButton("Copy all codes");
    copyAll.addClassName("mfa-recovery-action");
    copyAll.getElement().setAttribute("title",
        "Copies the codes, the account and the generation time");
    copyAll.addClickListener(event -> {
      copyAll.copy(sheetText);
      // The visible confirmation is the button's own check mark, which the browser sets and clears
      // (see CopyToClipboardButton). This line exists for screen readers only.
      copyStatus.setText("Copied to the clipboard at "
          + LocalTime.now().format(COPY_CONFIRMATION_TIME) + ".");
    });
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
