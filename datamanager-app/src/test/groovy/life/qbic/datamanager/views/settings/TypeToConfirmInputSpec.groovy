package life.qbic.datamanager.views.settings

import life.qbic.datamanager.views.general.dialog.InputValidation
import life.qbic.datamanager.views.general.dialog.TypeToConfirmInput
import spock.lang.Specification

/**
 * Unit tests for the {@link TypeToConfirmInput} guard used by the group dissolve flow.
 *
 * <p>Matching semantics follow the group-name rules ({@code GROUP-NFR-02}): the value must
 * match the expected name exactly — trimmed and case-insensitive — and partial/substring input
 * never passes. These stay lightweight UI-free specs: the wrapped {@code TextField}'s value can
 * be set directly without a Vaadin UI instance.</p>
 */
class TypeToConfirmInputSpec extends Specification {

  private static TypeToConfirmInput inputFor(String expected) {
    return new TypeToConfirmInput({ -> expected })
  }

  def "an exact match validates as passed"() {
    given:
    def input = inputFor("Advanced Imaging Group")

    when:
    input.textField().setValue("Advanced Imaging Group")

    then:
    input.validate().hasPassed()
  }

  def "a match ignoring surrounding whitespace validates as passed"() {
    given:
    def input = inputFor("Advanced Imaging Group")

    when:
    input.textField().setValue("  Advanced Imaging Group  ")

    then:
    input.validate().hasPassed()
  }

  def "a match ignoring case validates as passed (GROUP-NFR-02 name semantics)"() {
    given:
    def input = inputFor("Advanced Imaging Group")

    when:
    input.textField().setValue("advanced imaging group")

    then:
    input.validate().hasPassed()
  }

  def "a substring never validates as passed"() {
    given:
    def input = inputFor("Advanced Imaging Group")

    when:
    input.textField().setValue("Advanced")

    then:
    !input.validate().hasPassed()
  }

  def "an empty value never validates as passed and has no changes (regression)"() {
    given:
    def input = inputFor("Advanced Imaging Group")

    when:
    input.textField().setValue("")

    then:
    !input.validate().hasPassed()
    !input.hasChanges()
  }

  def "non-whitespace input counts as changes"() {
    given:
    def input = inputFor("Advanced Imaging Group")

    when:
    input.textField().setValue("partially")

    then:
    input.hasChanges()
  }

  def "the expected name is resolved lazily so an inline rename of the guarded resource stays in sync"() {
    given: "the expected name is provided via a mutable holder (like the page's membership)"
    String holder = "Old Name"
    def input = new TypeToConfirmInput({ -> holder })

    when: "the user first matches the old name"
    input.textField().setValue("Old Name")

    then: "the guard passes against the current expected name"
    input.validate().hasPassed()

    when: "the resource is renamed (e.g. inline rename on the page) and the user retypes the old name"
    holder = "New Name"
    input.textField().setValue("Old Name")

    then: "the guard does not pass anymore — the current name must be confirmed"
    !input.validate().hasPassed()

    and: "typing the new name passes"
    input.textField().setValue("New Name")
    input.validate().hasPassed()
  }

  def "surrounding whitespace in the expected name does not trap the user (name is compared trimmed)"() {
    given: "the guarded resource name carries surrounding whitespace"
    def input = new TypeToConfirmInput({ -> "  Padded Name  " })

    when: "the user types the name exactly as it is displayed (trimmed)"
    input.textField().setValue("Padded Name")

    then: "the guard passes — the comparison ignores the expected name's surrounding whitespace"
    input.validate().hasPassed()
  }
}