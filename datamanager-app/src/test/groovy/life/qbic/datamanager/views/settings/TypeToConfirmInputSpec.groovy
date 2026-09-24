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
    return new TypeToConfirmInput(expected)
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
}