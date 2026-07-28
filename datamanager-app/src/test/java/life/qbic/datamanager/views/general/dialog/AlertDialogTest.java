package life.qbic.datamanager.views.general.dialog;

import com.vaadin.flow.component.html.Div;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AlertDialog}.
 *
 * <p>Because {@link AlertDialog} is a Vaadin UI component, tests that
 * require a running UI session (e.g. verifying CSS classes on the
 * inner buttons) belong in integration-test scope. This class focuses
 * on the factory contract: null-safety, builder sequencing, and
 * that actions are wired (no crash on build).</p>
 *
 * @since 1.12.0
 */
class AlertDialogTest {

  /** Non-null {@link com.vaadin.flow.component.Component} used as the parent argument. */
  private final Div parent = new Div();

  // ── Static factory: danger() shortcut ─────────────────────────────────────

  @Test
  void danger_returnsAlertDialogInstance() {
    var dialog = AlertDialog.danger(parent, "Remove?", "Body text.", () -> {});
    Assertions.assertNotNull(dialog, "danger() must return a non-null AlertDialog");
    Assertions.assertNotNull(dialog.dialog(), "internal AppDialog must be non-null");
  }

  @Test
  void danger_throwsOnNullParent() {
    Assertions.assertThrows(NullPointerException.class,
        () -> AlertDialog.danger(null, "Title", "Msg", () -> {}));
  }

  @Test
  void danger_throwsOnNullTitle() {
    Assertions.assertThrows(NullPointerException.class,
        () -> AlertDialog.danger(parent, null, "Msg", () -> {}));
  }

  @Test
  void danger_throwsOnNullMessage() {
    Assertions.assertThrows(NullPointerException.class,
        () -> AlertDialog.danger(parent, "Title", null, () -> {}));
  }

  @Test
  void danger_throwsOnNullConfirmAction() {
    Assertions.assertThrows(NullPointerException.class,
        () -> AlertDialog.danger(parent, "Title", "Msg", null));
  }

  // ── Fluent builder ───────────────────────────────────────────────────────

  @Test
  void builder_requiresConfirmButtonBeforeBuild() {
    Assertions.assertThrows(NullPointerException.class,
        () -> AlertDialog.alert(parent).title("Title").message("Msg").build(),
        "build() without confirmButton() must fail");
  }

  @Test
  void builder_acceptsDangerThenBuilds() {
    var dialog = AlertDialog.alert(parent)
        .danger()
        .title("Delete?")
        .message("Irreversible operation.")
        .confirmButton("Delete", () -> {})
        .cancelButton("No", () -> {})
        .build();

    Assertions.assertNotNull(dialog);
    Assertions.assertNotNull(dialog.dialog());
  }

  @Test
  void builder_acceptsConfirmOnlyDialog() {
    var dialog = AlertDialog.alert(parent)
        .warning()
        .title("Acknowledge")
        .message("Something happened.")
        .confirmButton("OK", () -> {})
        .build();

    Assertions.assertNotNull(dialog);
  }

  @Test
  void builder_allIntentShorthandsBuild() {
    buildWith(AlertDialog.Builder::danger);
    buildWith(AlertDialog.Builder::warning);
    buildWith(AlertDialog.Builder::error);
    buildWith(AlertDialog.Builder::info);
  }

  private void buildWith(java.util.function.Function<AlertDialog.Builder, AlertDialog.Builder> shorthand) {
    var dialog = shorthand
        .apply(AlertDialog.alert(parent))
        .title("Title")
        .message("Msg")
        .confirmButton("OK", () -> {})
        .build();
    Assertions.assertNotNull(dialog);
  }

  // ── Builder: null-safety ────────────────────────────────────────────────

  @Test
  void builder_rejectsNullTitle() {
    Assertions.assertThrows(NullPointerException.class,
        () -> AlertDialog.alert(parent).danger().title(null));
  }

  @Test
  void builder_rejectsNullMessage() {
    Assertions.assertThrows(NullPointerException.class,
        () -> AlertDialog.alert(parent).danger().message(null));
  }

  @Test
  void builder_rejectsNullConfirmLabel() {
    Assertions.assertThrows(NullPointerException.class,
        () -> AlertDialog.alert(parent).danger().confirmButton(null, () -> {}));
  }
}
