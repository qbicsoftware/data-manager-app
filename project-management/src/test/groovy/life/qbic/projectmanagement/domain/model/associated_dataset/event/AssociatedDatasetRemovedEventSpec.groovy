package life.qbic.projectmanagement.domain.model.associated_dataset.event

import life.qbic.domain.concepts.DomainEvent
import life.qbic.projectmanagement.domain.model.associated_dataset.AssociatedDatasetId
import life.qbic.projectmanagement.domain.model.project.ProjectId
import spock.lang.Specification

/**
 * Unit tests for {@link AssociatedDatasetRemovedEvent}.
 *
 * @since 1.12.0
 */
class AssociatedDatasetRemovedEventSpec extends Specification {

  private static final String VALID_PROJECT_ID = "0270ce7f-4092-40e3-9c4c-ce7adb688bf5"

  def "create() constructs event with all fields"() {
    given:
    def datasetId = AssociatedDatasetId.create()
    def projectId = ProjectId.parse(VALID_PROJECT_ID)
    def userId = "user-123"
    def title = "Test Dataset"
    def pid = "10.12345/test"

    when:
    def event = AssociatedDatasetRemovedEvent.create(
        datasetId, projectId, userId, title, pid)

    then:
    event.associatedDatasetId() == datasetId
    event.projectId() == projectId
    event.actorUserId() == userId
    event.datasetTitle() == title
    event.datasetPid() == pid
  }

  def "create() throws NullPointerException for null associatedDatasetId"() {
    when:
    AssociatedDatasetRemovedEvent.create(null, ProjectId.parse(VALID_PROJECT_ID), "user", "title", "pid")

    then:
    thrown(NullPointerException)
  }

  def "create() throws NullPointerException for null projectId"() {
    when:
    AssociatedDatasetRemovedEvent.create(AssociatedDatasetId.create(), null, "user", "title", "pid")

    then:
    thrown(NullPointerException)
  }

  def "create() throws NullPointerException for null actorUserId"() {
    when:
    AssociatedDatasetRemovedEvent.create(AssociatedDatasetId.create(), ProjectId.parse(VALID_PROJECT_ID), null, "title", "pid")

    then:
    thrown(NullPointerException)
  }

  def "create() throws NullPointerException for null datasetTitle"() {
    when:
    AssociatedDatasetRemovedEvent.create(AssociatedDatasetId.create(), ProjectId.parse(VALID_PROJECT_ID), "user", null, "pid")

    then:
    thrown(NullPointerException)
  }

  def "create() throws NullPointerException for null datasetPid"() {
    when:
    AssociatedDatasetRemovedEvent.create(AssociatedDatasetId.create(), ProjectId.parse(VALID_PROJECT_ID), "user", "title", null)

    then:
    thrown(NullPointerException)
  }

  def "event extends DomainEvent"() {
    given:
    def event = AssociatedDatasetRemovedEvent.create(
        AssociatedDatasetId.create(), ProjectId.parse(VALID_PROJECT_ID), "user", "title", "pid")

    expect:
    event instanceof DomainEvent
  }
}