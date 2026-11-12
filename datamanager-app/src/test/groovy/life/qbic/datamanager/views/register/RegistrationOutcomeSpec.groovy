package life.qbic.datamanager.views.register

import life.qbic.datamanager.views.register.RegistrationOutcome.Action
import life.qbic.identity.application.user.IdentityService.EmptyUserNameException
import life.qbic.identity.application.user.IdentityService.UserExistsException
import life.qbic.identity.application.user.IdentityService.UserNameNotAvailableException
import life.qbic.identity.application.user.IdentityService.UserNameTooLongException
import spock.lang.Specification
import spock.lang.Unroll

/**
 * <b>A duplicate registration must be indistinguishable from a successful one (AUTH-NFR-03,
 * FEAT-AUTH-FLOWS-03).</b>
 *
 * <p>The disclosure existed in two places — the field validator and the submit handler — and both had
 * to change. The validator removal is pinned by {@code UserRegistrationAccountOracleSpec}; this spec
 * pins the submit decision, which is the half that is easy to reintroduce, because the domain still
 * raises {@code UserExistsException} and rendering it looks helpful.</p>
 *
 * <p>The decision is a pure function precisely so it can be asserted here without a Vaadin session, a
 * router, or a database. That is a deliberate consequence of how the bug survived: the old logic lived
 * inside a private method on a {@code @UIScope} view, where no spec could reach it.</p>
 */
class RegistrationOutcomeSpec extends Specification {

  def "a duplicate address is treated as a success, not as an error"() {
    expect: 'the view is told to behave exactly as it would after a real registration'
    RegistrationOutcome.decide([new UserExistsException()]) == Action.UNIFORM_SUCCESS
  }

  def "a duplicate address is not reported even when other failures accompany it"() {
    given: 'the domain reports more than one problem'
    def failures = [new UserNameNotAvailableException(), new UserExistsException()]

    expect: 'the duplicate still wins, because reporting the username would imply the address is free'
    RegistrationOutcome.decide(failures) == Action.UNIFORM_SUCCESS
  }

  @Unroll
  def "a #reason failure is still reported, because it reveals nothing about an account"() {
    expect:
    RegistrationOutcome.decide([failure]) == Action.REPORT_FAILURE

    where:
    reason           | failure
    'taken username' | new UserNameNotAvailableException()
    'empty username' | new EmptyUserNameException()
    'long username'  | new UserNameTooLongException()
    'unknown'        | new RuntimeException('boom')
  }

  def "an empty failure list means the registration succeeded"() {
    expect:
    RegistrationOutcome.decide([]) == Action.UNIFORM_SUCCESS
    RegistrationOutcome.decide(null) == Action.UNIFORM_SUCCESS
  }

  def "the duplicate check matches by type, not by message"() {
    given: 'an exception whose text says nothing about duplicates'
    def disguised = new UserExistsException()

    expect: 'it is still recognised, so a message rewrite cannot disable the uniform response'
    RegistrationOutcome.isDuplicateAccount([disguised])

    and: 'and an unrelated failure is not'
    !RegistrationOutcome.isDuplicateAccount([new UserNameNotAvailableException()])
    !RegistrationOutcome.isDuplicateAccount([])
  }
}
