package life.qbic.projectmanagement.application

import life.qbic.identity.api.AuthenticationToUserIdTranslator
import life.qbic.projectmanagement.application.api.PinnedProjectStore
import life.qbic.projectmanagement.application.api.ProjectOverviewLookup
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService
import life.qbic.projectmanagement.application.pinned.PinnedProject
import life.qbic.projectmanagement.application.pinned.PinnedProjectView
import life.qbic.projectmanagement.domain.model.project.ProjectId
import life.qbic.projectmanagement.domain.repository.ProjectRepository
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import spock.lang.Specification

import java.time.Instant

/**
 * Unit tests for {@link PinnedProjectService}.
 *
 * <p>The service is built on a real {@link ProjectInformationService} with mocked ports, so the
 * accessible-project resolution used for pins is the same code path the project overview uses — the
 * behaviour under test is the intersection, not a re-implementation of it.
 */
class PinnedProjectServiceSpec extends Specification {

  static final String USER_ID = "3fa85f64-5717-4562-b3fc-2c963f66afa6"
  static final String SNAPSHOT_CODE = "2024_001"
  static final String SNAPSHOT_TITLE = "Pinned time title"

  Authentication authentication = Mock()
  AuthenticationToUserIdTranslator userIdTranslator = Mock()
  ProjectAccessService projectAccessService = Mock()
  ProjectRepository projectRepository = Mock()
  ProjectOverviewLookup projectOverviewLookup = Mock()
  PinnedProjectStore pinnedProjectStore = Mock()

  ProjectInformationService projectInformationService = new ProjectInformationService(
      projectOverviewLookup, projectRepository, projectAccessService, userIdTranslator)

  PinnedProjectService pinnedProjectService = new PinnedProjectService(
      pinnedProjectStore, projectOverviewLookup, projectInformationService, userIdTranslator)

  def cleanup() {
    SecurityContextHolder.clearContext()
  }

  def "returns accessible pins with live project data and revoked pins with the pin-time label only"() {
    given: "the user pinned two projects but can only read one of them"
    def readable = ProjectId.create()
    def revoked = ProjectId.create()
    authenticateUser()
    pinnedProjectStore.findByUserId(USER_ID) >> [pin(revoked, Instant.parse("2026-01-02T00:00:00Z")),
                                                 pin(readable, Instant.parse("2026-01-01T00:00:00Z"))]
    projectAccessService.getAccessibleProjectsForSid(USER_ID) >> [readable]
    projectOverviewLookup.query("", 0, 1, _, [readable]) >> [overview(readable, "2024_009", "Live title")]

    when:
    def result = pinnedProjectService.findPinnedProjects()

    then: "the stored pin order is kept, so the newest pin stays first"
    result*.projectId() == [revoked, readable]

    and: "the unreadable pin is a revoked view carrying only the captured label"
    result[0].accessState() == PinnedProjectView.AccessState.REVOKED
    result[0].projectCode() == SNAPSHOT_CODE
    result[0].projectTitle() == SNAPSHOT_TITLE
    !result[0].isAccessible()

    and: "the readable pin carries the live overview instead of the snapshot"
    result[1].accessState() == PinnedProjectView.AccessState.ACCESSIBLE
    result[1].projectCode() == "2024_009"
    result[1].projectTitle() == "Live title"
  }

  def "returns every stored pin as revoked when the user can read none of them"() {
    given:
    def pinned = ProjectId.create()
    authenticateUser()
    pinnedProjectStore.findByUserId(USER_ID) >> [pin(pinned, Instant.now())]
    projectAccessService.getAccessibleProjectsForSid(USER_ID) >> []

    when:
    def result = pinnedProjectService.findPinnedProjects()

    then: "no overview query is issued and the pin is still rendered, not dropped"
    0 * projectOverviewLookup.query(*_)
    result.size() == 1
    !result[0].isAccessible()
  }

  def "shows no pins when nobody is authenticated"() {
    given: "the security context carries no authentication"
    userIdTranslator.translateToUserId(null) >> Optional.empty()

    when:
    def result = pinnedProjectService.findPinnedProjects()

    then:
    result.isEmpty()
    0 * pinnedProjectStore.findByUserId(_)
  }

  def "creating a pin stores the label that was readable at pin time"() {
    given: "the user may read the project and has room left"
    def projectId = ProjectId.create()
    authenticateUser()
    pinnedProjectStore.find(USER_ID, projectId) >> Optional.empty()
    pinnedProjectStore.countByUserId(USER_ID) >> 0
    projectAccessService.getAccessibleProjectsForSid(USER_ID) >> [projectId]
    projectOverviewLookup.query("", 0, 1, _, [projectId]) >> [overview(projectId, "2024_042", "A title")]

    when:
    def outcome = pinnedProjectService.pin(projectId)

    then: "the pin is stored with the owner, the live label and a creation timestamp"
    1 * pinnedProjectStore.add({ PinnedProject pin ->
      pin.userId() == USER_ID &&
          pin.projectId() == projectId &&
          pin.projectCodeSnapshot() == "2024_042" &&
          pin.projectTitleSnapshot() == "A title" &&
          pin.pinnedAt() != null
    })
    outcome == PinnedProjectService.PinOutcome.PINNED
  }

  def "rejects pinning a project the user cannot access, without storing anything"() {
    given:
    def projectId = ProjectId.create()
    authenticateUser()
    pinnedProjectStore.find(USER_ID, projectId) >> Optional.empty()
    pinnedProjectStore.countByUserId(USER_ID) >> 1
    projectAccessService.getAccessibleProjectsForSid(USER_ID) >> []

    when:
    def outcome = pinnedProjectService.pin(projectId)

    then:
    0 * pinnedProjectStore.add(_)
    outcome == PinnedProjectService.PinOutcome.NOT_PERMITTED
  }

  def "rejects pinning beyond the allowed number of pins"() {
    given: "the user already holds the maximum number of pins"
    def projectId = ProjectId.create()
    authenticateUser()
    pinnedProjectStore.find(USER_ID, projectId) >> Optional.empty()
    pinnedProjectStore.countByUserId(USER_ID) >> PinnedProjectService.MAX_PINNED_PROJECTS

    when:
    def outcome = pinnedProjectService.pin(projectId)

    then: "no pin is stored and the project is not even looked up"
    0 * pinnedProjectStore.add(_)
    0 * projectAccessService.getAccessibleProjectsForSid(_)
    outcome == PinnedProjectService.PinOutcome.LIMIT_REACHED
  }

  def "pins every project at most once"() {
    given: "the project is pinned already"
    def projectId = ProjectId.create()
    authenticateUser()
    pinnedProjectStore.find(USER_ID, projectId) >> Optional.of(pin(projectId, Instant.now()))

    when:
    def outcome = pinnedProjectService.pin(projectId)

    then:
    0 * pinnedProjectStore.add(_)
    outcome == PinnedProjectService.PinOutcome.ALREADY_PINNED
  }

  def "unpins a project the user can no longer read, because the pin would otherwise hold its place forever"() {
    given: "no authentication-based project access is resolved at all"
    def projectId = ProjectId.create()
    authenticateUser()
    pinnedProjectStore.remove(USER_ID, projectId) >> true

    when:
    def outcome = pinnedProjectService.unpin(projectId)

    then:
    0 * projectAccessService.getAccessibleProjectsForSid(_)
    0 * projectOverviewLookup.query(*_)
    outcome == PinnedProjectService.PinOutcome.UNPINNED
  }

  def "reports that nothing was removed when the user unpins a project they never pinned"() {
    given:
    def projectId = ProjectId.create()
    authenticateUser()
    pinnedProjectStore.remove(USER_ID, projectId) >> false

    when:
    def outcome = pinnedProjectService.unpin(projectId)

    then:
    outcome == PinnedProjectService.PinOutcome.NOT_PINNED
  }

  def "reports no active user when unpinning without authentication"() {
    given:
    userIdTranslator.translateToUserId(null) >> Optional.empty()

    when:
    def outcome = pinnedProjectService.unpin(ProjectId.create())

    then:
    0 * pinnedProjectStore.remove(*_)
    outcome == PinnedProjectService.PinOutcome.NO_ACTIVE_USER
  }

  private void authenticateUser() {
    authentication.getAuthorities() >> []
    userIdTranslator.translateToUserId(authentication) >> Optional.of(USER_ID)
    SecurityContextHolder.getContext().setAuthentication(authentication)
  }

  private static PinnedProject pin(ProjectId projectId, Instant pinnedAt) {
    PinnedProject.create(USER_ID, projectId, pinnedAt, SNAPSHOT_CODE, SNAPSHOT_TITLE)
  }

  private static ProjectOverview overview(ProjectId projectId, String code, String title) {
    def overview = new ProjectOverview()
    overview.@id = projectId
    overview.@projectCode = code
    overview.@projectTitle = title
    return overview
  }
}
