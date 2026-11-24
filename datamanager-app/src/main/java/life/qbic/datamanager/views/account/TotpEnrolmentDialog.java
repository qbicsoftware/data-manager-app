package life.qbic.datamanager.views.account;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import life.qbic.datamanager.views.general.CopyToClipboardButton;
import life.qbic.datamanager.views.general.dialog.AppDialog;
import life.qbic.datamanager.views.general.dialog.DialogBody;
import life.qbic.datamanager.views.general.dialog.DialogFooter;
import life.qbic.datamanager.views.general.dialog.DialogHeader;
import life.qbic.datamanager.views.general.dialog.InputValidation;
import life.qbic.datamanager.views.general.dialog.UserInput;
import life.qbic.identity.application.mfa.EnrolmentExpiredException;
import life.qbic.identity.application.mfa.InvalidTotpCodeException;
import life.qbic.identity.application.mfa.PendingTotpEnrolment;
import life.qbic.identity.application.mfa.SecondFactorSecretUnavailableException;
import life.qbic.identity.domain.model.mfa.SecondFactor;

/**
 * <b>Setting up an authenticator app (AUTH-R-02)</b>
 *
 * <p>Shows the setup key together with a copy action, then verifies a code from the app. The dialog
 * holds no service: the verification is a callback supplied by {@link TwoFactorAuthenticationMain},
 * so the transaction and the "no factor without a verified code" rule live in the application layer,
 * and this class cannot accidentally create a factor on its own.</p>
 *
 * <p><b>Why the key is extracted from the provisioning URI.</b> {@link PendingTotpEnrolment} carries
 * the ciphertext seed only, on purpose — it can sit in a session without being a plaintext secret. The
 * URI is the one place the plaintext key exists, because that is what an authenticator consumes, so the
 * manual-entry field reads the value from it rather than the pending object growing a second copy.
 * The QR image is not rendered yet: that needs a QR encoder dependency through the BOM and its
 * signature check, which is tracked separately.</p>
 *
 * @since 1.0.0
 */
public class TotpEnrolmentDialog extends Div {

  @Serial
  private static final long serialVersionUID = 1L;

  /** What the factor is called when the user does not choose a name. */
  public static final String DEFAULT_DISPLAY_NAME = TwoFactorSettingsCopy.DEFAULT_FACTOR_NAME;

  private final AppDialog dialog;
  private final TextField displayName = new TextField("Name this app (optional)");
  private final TextField code = new TextField("Code from the app");
  private final Span error = new Span();
  private final List<ComponentEventListener<EnrolledEvent>> enrolledListeners = new ArrayList<>();

  /**
   * @param pending the enrolment to confirm
   * @param verify  verifies the typed code <em>and</em> stores the factor under the chosen name; throws
   *                {@link InvalidTotpCodeException} when the code does not match,
   *                {@link EnrolmentExpiredException} when the setup window has passed
   */
  public TotpEnrolmentDialog(PendingTotpEnrolment pending,
      BiFunction<String, String, SecondFactor> verify) {
    dialog = AppDialog.medium();
    DialogHeader.with(dialog, "Set up an authenticator app");

    String setupKey = seedFrom(pending.provisioningUri());
    TextField setupKeyField = new TextField("Setup key (if you can't scan)");
    setupKeyField.setValue(setupKey);
    setupKeyField.setReadOnly(true);
    setupKeyField.setWidthFull();

    // CopyToClipboardButton, not CopyToClipBoardComponent: the latter swaps its icon for a check mark
    // and restores it from the server, which without push only happens on the next request — so after
    // one click the check stays and the key looks copied-for-good. This button keeps its label, is
    // keyboard reachable, and confirms in the browser (see CopyToClipboardButton).
    CopyToClipboardButton copySetupKey = new CopyToClipboardButton("Copy");
    copySetupKey.addClickListener(event -> copySetupKey.copy(setupKey));

    Div setupKeyRow = new Div(setupKeyField, copySetupKey);
    setupKeyRow.addClassName("mfa-setup-key-row");

    Paragraph instructions = new Paragraph(
        "Scan the QR code, then type the six-digit code your app shows.");

    Div qrBlock = new Div(new TotpQrCode(pending.provisioningUri()));
    qrBlock.addClassName("mfa-qr-block");

    code.setWidthFull();
    code.setMaxLength(8);
    code.setPlaceholder("123456");

    // Optional, and last before the code: the user is mid-task with an app open, so the name must not
    // stand between them and the two things they have to do. It defaults to DEFAULT_DISPLAY_NAME, which
    // is what every factor is called until the account has more than one.
    displayName.setValue(DEFAULT_DISPLAY_NAME);
    displayName.setWidthFull();
    displayName.setMaxLength(SecondFactor.MAX_DISPLAY_NAME_LENGTH);
    displayName.setHelperText("How this app appears in your settings. Useful if you add more than one.");
    // Clearing the field means "call it the default", not "save an empty name" — the domain refuses a
    // blank label, so an empty submission would fail with a validation error after the code was accepted.
    displayName.setClearButtonVisible(true);

    error.addClassName("mfa-error");
    error.getStyle().set("color", "var(--lumo-error-text-color)");
    error.setVisible(false);

    Div body = new Div(instructions, qrBlock, setupKeyRow, displayName, code, error);
    body.addClassName("mfa-enrol-body");

    DialogBody.with(dialog, body, new CodeInput(verify));
    DialogFooter.with(dialog, "Cancel", "Verify and enable");
    dialog.registerCancelAction(dialog::close);
    dialog.registerConfirmAction(() -> {
      dialog.close();
      enrolledListeners.forEach(
          listener -> listener.onComponentEvent(new EnrolledEvent(this, false)));
    });
  }

  public void open() {
    dialog.open();
  }


  public com.vaadin.flow.shared.Registration addEnrolledListener(
      ComponentEventListener<EnrolledEvent> listener) {
    enrolledListeners.add(listener);
    return () -> enrolledListeners.remove(listener);
  }

  private void showError(String message) {
    error.setText(message);
    error.setVisible(true);
    code.setInvalid(true);
  }

  /**
   * The name the factor will carry.
   *
   * <p>An empty field means the default rather than "no name": the domain refuses a blank label, and
   * failing the whole submission after the code was accepted would be a confusing way to learn that a
   * label is required. The field is prefilled with the default, so this only matters when somebody
   * clears it on purpose.</p>
   *
   * @return the label to store
   */
  String chosenDisplayName() {
    return TwoFactorSettingsCopy.factorName(displayName.getValue());
  }

  /**
   * Reads the shared key out of the {@code otpauth://} URI.
   *
   * <p>The URI is the domain's interface for "what the authenticator needs"; the manual-entry field is
   * just a second rendering of it.</p>
   */
  static String seedFrom(String provisioningUri) {
    String marker = "secret=";
    int start = provisioningUri.indexOf(marker);
    if (start < 0) {
      throw new IllegalArgumentException(
          "the provisioning URI does not contain a secret parameter");
    }
    int valueStart = start + marker.length();
    int end = provisioningUri.indexOf('&', valueStart);
    return end < 0
        ? provisioningUri.substring(valueStart)
        : provisioningUri.substring(valueStart, end);
  }

  /**
   * Verification runs on the confirm action: a failed code keeps the dialog open and changes nothing
   * anywhere else (M2: half-enrolled is not a state).
   */
  private class CodeInput implements UserInput {

    private final BiFunction<String, String, SecondFactor> verify;

    CodeInput(BiFunction<String, String, SecondFactor> verify) {
      this.verify = verify;
    }

    @Override
    public InputValidation validate() {
      error.setVisible(false);
      code.setInvalid(false);
      String presented = code.getValue() == null ? "" : code.getValue().strip();
      if (presented.isEmpty()) {
        showError("Enter the six-digit code shown by the authenticator app.");
        return InputValidation.failed();
      }
      try {
        // The code and the name go together: the factor cannot be stored without both, so passing them
        // as one call keeps "half enrolled" impossible rather than merely unlikely.
        verify.apply(presented, chosenDisplayName());
        return InputValidation.passed();
      } catch (InvalidTotpCodeException e) {
        // The one case worth advising on: a phone whose clock has drifted is the usual cause.
        showError("That code is not valid. Check that the phone's clock is set automatically, "
            + "then try the current code.");
      } catch (EnrolmentExpiredException e) {
        showError("This setup took too long. Close the dialog and start again.");
      } catch (SecondFactorSecretUnavailableException e) {
        // Fail closed and say so: the alternative would be silently treating setup as done.
        showError("Second-factor setup is temporarily unavailable. Please try again later.");
      }
      return InputValidation.failed();
    }

    @Override
    public boolean hasChanges() {
      return !code.isEmpty();
    }
  }

  /**
   * The factor was created. Carries no payload on purpose: the parent reloads from the repository,
   * so the list it renders is the stored state, not a value passed through an event.
   */
  public static class EnrolledEvent extends ComponentEvent<TotpEnrolmentDialog> {

    @Serial
    private static final long serialVersionUID = 1L;

    public EnrolledEvent(TotpEnrolmentDialog source, boolean fromClient) {
      super(source, fromClient);
    }
  }
}
