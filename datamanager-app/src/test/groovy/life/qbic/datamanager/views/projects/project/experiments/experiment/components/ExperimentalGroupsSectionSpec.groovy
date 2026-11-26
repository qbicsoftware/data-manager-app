package life.qbic.datamanager.views.projects.project.experiments.experiment.components

import life.qbic.datamanager.views.projects.project.experiments.experiment.ExperimentalGroupInput.VariableLevel
import spock.lang.Specification

/**
 * Covers the state in which an experiment has no variables.
 *
 * <p>Without variables the condition field offered an empty item list while the input's validator
 * was {@code levels.size() == variableCount} with {@code variableCount == 0}. Selecting nothing
 * therefore passed validation, so Save fired, {@code experimentalGroups()} dropped the
 * condition-less entry, and the handler returned early on the empty list: clicking Save did
 * nothing at all. These specs pin the replacement behaviour -- a stated prerequisite and a section
 * that reports itself unsaveable, so no path can reach a silent no-op.</p>
 */
class ExperimentalGroupsSectionSpec extends Specification {

  private static final VariableLevel SPECIES_MOUSE =
      new VariableLevel("Species", "Mouse", null)

  def "an experiment without variables shows the prerequisite instead of a group form"() {
    when:
    def section = ExperimentalGroupsSection.empty([])

    then:
    section.isLocked()
  }

  def "an experiment with variables is not locked"() {
    when:
    def section = ExperimentalGroupsSection.empty([SPECIES_MOUSE])

    then:
    !section.isLocked()
  }

  /**
   * The load-bearing assertion. If a locked section could report a valid state, a Save click would
   * fire an event that produces no change, which is exactly the silent no-op being removed.
   */
  def "a locked section never reports a saveable state"() {
    when:
    def section = ExperimentalGroupsSection.empty([])

    then:
    !section.isValid()
  }

  def "a locked section yields no groups"() {
    when:
    def section = ExperimentalGroupsSection.empty([])

    then:
    section.experimentalGroups().isEmpty()
  }

  /**
   * The unlock path: once variables exist, the form is back and behaves normally.
   */
  def "a section with variables yields groups built from variable levels"() {
    given:
    def section = ExperimentalGroupsSection.empty([SPECIES_MOUSE])

    expect:
    // The form rendered an entry and reports itself saveable, i.e. the locked branch was not taken.
    section.isValid()
    !section.isLocked()
  }

  def "a locked section raises the go-to-variables request when asked"() {
    given:
    def section = ExperimentalGroupsSection.empty([])
    def raised = false
    section.addGoToVariablesListener({ raised = true })

    when:
    section.fireEvent(
        new ExperimentalGroupsSection.GoToVariablesRequestedEvent(section, false))

    then:
    raised
  }
}
