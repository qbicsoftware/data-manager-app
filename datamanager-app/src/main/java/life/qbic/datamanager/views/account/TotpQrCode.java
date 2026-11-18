package life.qbic.datamanager.views.account;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.html.Div;
import java.io.Serial;

/**
 * <b>QR code for a TOTP enrolment (AUTH-R-02)</b>
 *
 * <p>Renders the {@code otpauth://} provisioning URI into a canvas so the user scans it instead of
 * typing a 32-character setup key — which is the step people get wrong, and the reason this exists.</p>
 *
 * <h3>Why the QR is drawn in the browser, not on the server</h3>
 * <p>The obvious implementation is a server-side encoder (ZXing). That needs a new Maven dependency,
 * and this repository's build verifies PGP signatures of every artifact: ZXing's signing key is not in
 * the trusted map, and its {@code javase} module pulls in {@code jai-imageio-core}, which is worse.
 * A QR code would therefore have arrived with a security-allow-list change. Drawing it client-side
 * needs neither: {@code qrcode} is an npm package bundled into the frontend at build time, exactly
 * like {@code cropperjs} already is.</p>
 *
 * <p><b>The seed does not leave our origin.</b> The URI is passed as a component argument, so the
 * browser receives the same value the setup-key field already shows; nothing is fetched from a
 * third-party QR service, and no CDN is contacted at runtime. When rendering fails, the canvas stays
 * empty and the setup key below it remains the fallback — that is why the dialog shows both.</p>
 *
 * @since 1.0.0
 */
@NpmPackage(value = "qrcode", version = "1.5.4")
@JsModule("./javascript/totpqrcode.js")
public class TotpQrCode extends Div {

  @Serial
  private static final long serialVersionUID = 1L;

  /** Side length of the rendered code; 240 px keeps modules legible on a phone camera. */
  static final int SIZE_PX = 240;

  /**
   * @param provisioningUri the {@code otpauth://} URI; contains the seed, so never log this component
   */
  public TotpQrCode(String provisioningUri) {
    this(provisioningUri, SIZE_PX);
  }

  TotpQrCode(String provisioningUri, int sizePixels) {
    requireNonNull(provisioningUri, "provisioningUri must not be null");
    if (sizePixels <= 0) {
      throw new IllegalArgumentException("sizePixels must be positive, but was " + sizePixels);
    }
    addClassName("mfa-qr");
    // Deferred to attach: executeJs() on a detached element is queued, but doing it here makes the
    // ordering explicit and lets the dialog be constructed before it is opened.
    addAttachListener(event -> getElement().executeJs(
        "window.qbicTotpQrCode.render($0, $1, $2)", getElement(), provisioningUri, sizePixels));
  }
}
