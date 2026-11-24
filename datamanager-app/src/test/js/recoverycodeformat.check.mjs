/**
 * Checks the live formatting of the recovery-code field, without a browser.
 *
 * The formatter is client-side (`frontend/javascript/recoverycodeformat.js`) because the field's value
 * change is LAZY and formatting on the server would round-trip per keystroke. Two earlier Java
 * attempts failed *silently on a deployment*, which is why this check exists: it runs the real script
 * against a fake document and asserts what the user sees while typing, pasting and deleting.
 *
 * Not part of the Maven build (the project has no JS test runner). Run it by hand, from the repository
 * root:
 *
 *     node datamanager-app/src/test/js/recoverycodeformat.check.mjs
 *
 * Exit code 0 means every rule below holds; 1 means at least one does not.
 */
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';

const SCRIPT = new URL('../../../frontend/javascript/recoverycodeformat.js', import.meta.url);

/** The smallest stand-ins for the elements the script touches. */
class FakeInput {

  constructor(value, caret = value.length) {
    this.value = value;
    this.selectionStart = caret;
    this.selectionEnd = caret;
  }

  setSelectionRange(start, end) {
    this.selectionStart = start;
    this.selectionEnd = end;
  }
}

class FakeField {

  constructor(value) {
    this.attrs = new Set(['data-recovery-code-input']);
    this.value = value;
  }

  hasAttribute(name) {
    return this.attrs.has(name);
  }
}

/** Loads the real script with a fake document and returns the handlers it registered. */
function loadFormatter() {
  const handlers = {};
  const context = {
    document: {addEventListener: (type, handler) => { handlers[type] = handler; }},
    console,
    Element: FakeField,
    HTMLInputElement: FakeInput,
  };
  vm.createContext(context);
  vm.runInContext(fs.readFileSync(SCRIPT, 'utf8'), context);
  assert.ok(handlers.input, 'the script must listen for input events');
  assert.ok(handlers.keydown, 'the script must listen for keydown events');
  return handlers;
}

const handlers = loadFormatter();

/** A field as it looks in the DOM: the component element plus the input inside its shadow root. */
function field(value, caret = value.length) {
  const host = new FakeField(value);
  const input = new FakeInput(value, caret);
  const path = [input, host];
  return {
    host,
    input,
    key(key, modifiers = {}) {
      const event = {
        key,
        ctrlKey: false,
        altKey: false,
        metaKey: false,
        ...modifiers,
        composedPath: () => path,
        target: host,
        prevented: false,
        preventDefault() { this.prevented = true; },
      };
      handlers.keydown(event);
      return event;
    },
    /** The browser inserted text at the caret, then the script saw the input event. */
    insert(text) {
      const before = input.value.slice(0, input.selectionStart);
      const after = input.value.slice(input.selectionEnd);
      setValue(host, input, before + text + after, before.length + text.length);
      handlers.input({composedPath: () => path, target: host});
      return input.value;
    },
    /** Backspace: the keydown handler may take over, otherwise the browser deletes and we re-format. */
    backspace() {
      const event = this.key('Backspace');
      if (!event.prevented) {
        const caret = input.selectionStart;
        setValue(host, input, input.value.slice(0, caret - 1) + input.value.slice(caret),
            caret - 1);
        handlers.input({composedPath: () => path, target: host});
      }
      return input.value;
    },
  };
}

function setValue(host, input, value, caret) {
  host.value = value;
  input.value = value;
  input.setSelectionRange(caret, caret);
}

/** Types character by character, as a user does. */
function type(chars) {
  const state = field('');
  for (const character of chars) {
    state.insert(character);
  }
  return state.input.value;
}

const results = [];
function check(name, run) {
  try {
    run();
    results.push(`ok    ${name}`);
  } catch (error) {
    results.push(`FAIL  ${name}\n      ${error.message}`);
  }
}

// --- typing -------------------------------------------------------------------------------------

check('lower case is upper-cased as it is typed', () => {
  assert.equal(type('abcd'), 'ABCD');
});

check('the grouping hyphen appears with the fifth character', () => {
  assert.equal(type('abcde'), 'ABCD-E');
});

check('a full code is grouped XXXX-XXXX-XXXX', () => {
  assert.equal(type('abcdefghijkl'), 'ABCD-EFGH-IJKL');
});

check('a thirteenth character is dropped instead of overflowing the field', () => {
  assert.equal(type('abcdefghijklmn'), 'ABCD-EFGH-IJKL');
});

check('the caret stays at the end while typing at the end', () => {
  const state = field('');
  for (const character of 'abcde') {
    state.insert(character);
  }
  assert.equal(state.input.selectionStart, 6, 'caret must be behind the hyphen and the new letter');
});

// --- pasting ------------------------------------------------------------------------------------

check('a pasted, already formatted code is left alone', () => {
  const state = field('', 0);
  assert.equal(state.insert('ABCD-EFGH-IJKL'), 'ABCD-EFGH-IJKL');
});

check('a pasted lower-case formatted code is normalised', () => {
  const state = field('', 0);
  assert.equal(state.insert('abcd-efgh-ijkl'), 'ABCD-EFGH-IJKL');
});

check('a pasted code without hyphens gets them', () => {
  const state = field('', 0);
  assert.equal(state.insert('ABCDEFGH'), 'ABCD-EFGH');
});

check('characters outside the alphabet are dropped', () => {
  const state = field('', 0);
  assert.equal(state.insert('ab0cd1ef8gh9'), 'ABCD-EFGH');
});

// --- deleting -----------------------------------------------------------------------------------

check('backspace at the end removes the last character', () => {
  assert.equal(field('ABCD-EFGH-IJKL').backspace(), 'ABCD-EFGH-IJK');
});

check('backspace behind a hyphen deletes the character of the group, not the filler', () => {
  assert.equal(field('ABCD-E', 5).backspace(), 'ABCE');
});

check('backspace behind a hyphen mid-code regroups', () => {
  assert.equal(field('ABCD-EFGH-IJKL', 5).backspace(), 'ABCE-FGHI-JKL');
});

check('backspace after a group boundary deletes that group\'s last character', () => {
  assert.equal(field('ABCD-EFGH', 4).backspace(), 'ABCE-FGH');
});

check('backspace with a selection is left to the browser', () => {
  const state = field('ABCD-EFGH', 2);
  state.input.selectionEnd = 4;
  const event = state.key('Backspace');
  assert.equal(event.prevented, false);
});

// --- the marker ---------------------------------------------------------------------------------

check('fields not marked for formatting are untouched', () => {
  const host = new FakeField('');
  host.attrs = new Set();
  const input = new FakeInput('');
  const path = [input, host];
  input.value = 'abcd';
  host.value = 'abcd';
  handlers.input({composedPath: () => path, target: host});
  assert.equal(input.value, 'abcd');
});

const failed = results.filter((line) => line.startsWith('FAIL'));
console.log(results.join('\n'));
console.log(failed.length === 0
    ? `\n${results.length} checks passed`
    : `\n${failed.length} of ${results.length} checks FAILED`);
process.exit(failed.length === 0 ? 0 : 1);
