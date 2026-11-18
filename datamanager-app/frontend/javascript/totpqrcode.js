/*
 * QR code for second-factor enrolment.
 *
 * Renders the otpauth:// provisioning URI into a canvas so an authenticator app can scan it instead
 * of the user typing a 32-character setup key. The URI contains the seed, so it is passed as a Java
 * -> JS argument and never fetched from anywhere; the library is bundled at build time (no CDN, no
 * third-party service sees the seed).
 *
 * Wrapped in a function because Vaadin's @JsModule only guarantees the module is loaded before the
 * element's JS runs, not that a custom element was registered.
 *
 * Exposed as window.qbicTotpQrCode = { render(host, text, sizePx) }.
 */
import QRCode from 'qrcode';

window.qbicTotpQrCode = {
  /**
   * @param host   the element to fill
   * @param text   the otpauth:// provisioning URI
   * @param sizePx side length of the square code
   */
  render(host, text, sizePx) {
    if (!host) {
      return;
    }
    const canvas = document.createElement('canvas');
    canvas.className = 'mfa-qr-canvas';
    canvas.setAttribute('role', 'img');
    canvas.setAttribute('aria-label', 'QR code containing this account\'s setup key');

    // toCanvas returns a promise; on failure the setup key below the QR stays usable, so this
    // degrades instead of showing a broken image.
    QRCode.toCanvas(canvas, text, {
      width: sizePx,
      margin: 2,
      errorCorrectionLevel: 'M',
    })
      .then(() => host.replaceChildren(canvas))
      .catch(() => {
        host.replaceChildren();
        host.classList.add('mfa-qr--unavailable');
      });
  },
};
