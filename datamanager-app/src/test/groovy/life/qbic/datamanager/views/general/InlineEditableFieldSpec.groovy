package life.qbic.datamanager.views.general

import spock.lang.Specification

/**
 * Unit tests for the {@link InlineEditableField} blur handling and value semantics.
 *
 * <p>Blur-to-outside uses a deferred client-side check (Vaadin's BlurEvent carries no
 * relatedTarget), so the browser-side decision itself is not testable here. We pin the
 * server-side contract instead: commit-on-blur fires a SaveEvent and clears the editing chrome,
 * an unchanged value just exits edit mode quietly, and the {@code setCommitOnBlurToOutside}
 * toggle switches to cancel-on-blur semantics.</p>
 */
class InlineEditableFieldSpec extends Specification {

  def "commit-on-blur-to-outside fires a SaveEvent and leaves edit mode when value changed"() {
    given: "an editable field in edit mode with a draft"
    def field = new InlineEditableField("Name", "old value")
    List<String> saved = []
    field.addSaveListener { e -> saved << e.value() }
    field.startEditing()
    assert saved.isEmpty()

    when: "a draft is typed and blur-to-outside commits"
    field.setValueInternal("new value")
    field.attemptBlurCommitForTest()

    then: "the draft is saved and edit mode is left"
    saved == ["new value"]
    !field.isEditingForTest()
  }

  def "commit-on-blur with an unchanged value just exits edit mode silently"() {
    given: "a field in edit mode with no draft"
    def field = new InlineEditableField("Name", "same")
    List<String> saved = []
    field.addSaveListener { e -> saved << e.value() }
    field.startEditing()

    when: "blur-to-outside is triggered without changes"
    field.attemptBlurCommitForTest()

    then: "no SaveEvent fires and edit mode is left quietly"
    saved.isEmpty()
    !field.isEditingForTest()
  }

  def "setCommitOnBlurToOutside(false) reverts instead of saving"() {
    given: "a field configured to cancel on outside blur"
    def field = new InlineEditableField("Name", "old value")
    field.setCommitOnBlurToOutside(false)
    List<String> saved = []
    field.addSaveListener { e -> saved << e.value() }
    field.startEditing()
    field.setValueInternal("draft")

    when: "blur-to-outside occurs"
    field.attemptBlurCommitForTest()

    then: "no SaveEvent fires and the value reverts to the committed one"
    saved.isEmpty()
    field.getValue() == "old value"
    !field.isEditingForTest()
  }

  def "explicit Save still fires a SaveEvent with the trimmed value"() {
    given:
    def field = new InlineEditableField("Name", "old")
    List<String> saved = []
    field.addSaveListener { e -> saved << e.value() }
    field.startEditing()
    field.setValueInternal("  new  ")

    when:
    field.saveForTest()

    then:
    saved == ["new"]
    // the committed value is unchanged until the parent persists and calls setValue
    field.getValue() == "old"
  }

  def "a textarea-backed field sets a stable min-width bound instead of growing to fit long content"() {
    given: "a textarea field with a long multi-line value (like a 500-char group description)"
    def longDescription = "word ".repeat(120).trim() // ~600 characters, longer than any max len
    def field = new InlineEditableField(InlineEditableField.InputKind.TEXTAREA, "Description", longDescription)
    field.setMinDisplayWidth(40)

    when: "the display min-width is applied"
    int minWidthCh = field.displayWidthForTest()

    then: "the lower bound stays at the configured minimum (40ch) regardless of the long content"
    minWidthCh == 40
  }
}