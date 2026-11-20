/* Confirms a copy action in the browser, without a server round trip.
 *
 * The clipboard write is triggered from the server (CopyToClipboardButton), but the confirmation
 * must not be: with push off, UI.accessLater runs on the *next request*, so a server-side "copied"
 * state stayed on screen and the action looked used up after one click. This script only decorates:
 * after a click on an element marked with `data-copy-feedback`, the element gets `data-copied` for a
 * moment (CSS swaps the copy icon for a check mark) and then returns to the copy state.
 *
 * It is delegated on the document instead of installed per element on attach: an install that runs
 * once and finds nothing, or that races the web component's upgrade, fails silently — that is what
 * happened to the recovery-code field grouping.
 */
const COPY_CONFIRMATION_MILLIS = 1600;

document.addEventListener('click', (event) => {
  const button = event.target instanceof Element
      ? event.target.closest('[data-copy-feedback]')
      : null;
  if (!button) {
    return;
  }
  button.setAttribute('data-copied', '');
  window.clearTimeout(button.__qbicCopyConfirmation);
  button.__qbicCopyConfirmation = window.setTimeout(
      () => button.removeAttribute('data-copied'), COPY_CONFIRMATION_MILLIS);
});
