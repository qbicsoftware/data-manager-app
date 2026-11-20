/* Live formatting for the recovery-code field of the second-factor challenge.
 *
 * The field is a Vaadin TextField, whose value-change mode is LAZY: the server only hears about a
 * value when the field loses focus. Formatting therefore cannot live on the server without a round
 * trip per keystroke (and a caret that jumps), so it lives here — in the browser, where the keystroke
 * happens.
 *
 * Two earlier attempts in Java failed silently and cost deployment round trips:
 *   1. a server-side value-change listener — LAZY, so it ran only on blur;
 *   2. an inline executeJs install on attach with a retry loop — the install never happened, and a
 *      script that does nothing also logs nothing, so there was no signal at all.
 * This script is delegated on the document instead of installed per element: it runs on the `input`
 * that already bubbles out of the field's shadow DOM, so there is no attach race and nothing to
 * install. It only acts on fields marked with `data-recovery-code-input`.
 *
 * Rules, matching RecoveryCodes/RecoveryCodeEncoder on the server:
 *   - upper case (codes are printed in upper case; typing lower case must not look wrong);
 *   - alphabet A-Z and 2-7 only, so 0/1/8/9 can never appear in a code;
 *   - grouped XXXX-XXXX-XXXX, at most 12 significant characters;
 *   - the hyphens are fillers: the script creates them, and a backspace at a group boundary deletes
 *     the last character of the group instead of uselessly deleting the hyphen that would be
 *     re-created immediately (that is the "the field is stuck" bug this avoids);
 *   - a pasted code that is already formatted is left alone (it normalises to itself).
 */
const RECOVERY_CODE_LENGTH = 12;
const RECOVERY_CODE_GROUP_SIZE = 4;
const RECOVERY_CODE_ALPHABET = /[A-Z2-7]/g;
const RECOVERY_CODE_FIELD_MARKER = 'data-recovery-code-input';

/** The significant characters of a value: upper case, alphabet only. */
const significantCharacters = (value) =>
    ((value || '').toUpperCase().match(RECOVERY_CODE_ALPHABET) || []);

/** Groups significant characters as XXXX-XXXX-XXXX. */
const grouped = (characters) => {
  const groups = [];
  for (let index = 0; index < characters.length; index += RECOVERY_CODE_GROUP_SIZE) {
    groups.push(characters.slice(index, index + RECOVERY_CODE_GROUP_SIZE).join(''));
  }
  return groups.join('-');
};

/** The caret position that leaves `count` significant characters in front of it. */
const caretAfter = (formatted, count) => {
  let position = 0;
  let seen = 0;
  while (position < formatted.length && seen < count) {
    if (formatted.charAt(position) !== '-') {
      seen += 1;
    }
    position += 1;
  }
  // A hyphen belongs to the group in front of it; leaving the caret before it would drop the next
  // character into the wrong group.
  if (position < formatted.length && formatted.charAt(position) === '-') {
    position += 1;
  }
  return position;
};

/** The marked field of this event, or null if the event is not from one. */
const markedField = (event) => {
  const path = typeof event.composedPath === 'function' ? event.composedPath() : [event.target];
  const field = path.find(
      (node) => node instanceof Element && node.hasAttribute(RECOVERY_CODE_FIELD_MARKER));
  return field ? {field, input: path.find((node) => node instanceof HTMLInputElement)} : null;
};

/** Writes the formatted value into the component (which updates the server) and places the caret. */
const apply = (field, input, characters, countBeforeCaret) => {
  const formatted = grouped(characters);
  // The component's own value, not only the inner input's: the field's state and the server must
  // agree with what the user sees, without waiting for a blur.
  field.value = formatted;
  if (input) {
    input.value = formatted;
    const position = caretAfter(formatted, countBeforeCaret);
    input.setSelectionRange(position, position);
  }
};

document.addEventListener('input', (event) => {
  const target = markedField(event);
  if (!target) {
    return;
  }
  const {field, input} = target;
  const raw = input ? input.value : field.value;
  const caret = input && typeof input.selectionStart === 'number' ? input.selectionStart : raw.length;
  const characters = significantCharacters(raw).slice(0, RECOVERY_CODE_LENGTH);
  if (grouped(characters) === raw) {
    return;
  }
  apply(field, input, characters, significantCharacters(raw.slice(0, caret)).length);
});

document.addEventListener('keydown', (event) => {
  // Backspace directly behind a hyphen: delete the character of the group instead. Letting the
  // browser delete the hyphen would make the keystroke a no-op, because the hyphen is a filler that
  // the formatter creates again on the next `input`.
  if (event.key !== 'Backspace' || event.ctrlKey || event.altKey || event.metaKey) {
    return;
  }
  const target = markedField(event);
  if (!target) {
    return;
  }
  const {field, input} = target;
  if (!input || input.selectionStart !== input.selectionEnd || input.selectionStart === 0) {
    return;
  }
  const caret = input.selectionStart;
  if (input.value.charAt(caret - 1) !== '-') {
    return;
  }
  event.preventDefault();
  const characters = significantCharacters(input.value).slice(0, RECOVERY_CODE_LENGTH);
  const indexToRemove = significantCharacters(input.value.slice(0, caret - 1)).length - 1;
  if (indexToRemove < 0) {
    return;
  }
  characters.splice(indexToRemove, 1);
  apply(field, input, characters, indexToRemove);
});
