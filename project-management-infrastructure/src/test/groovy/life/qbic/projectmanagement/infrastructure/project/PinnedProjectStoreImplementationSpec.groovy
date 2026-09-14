package life.qbic.projectmanagement.infrastructure.project

import life.qbic.application.commons.ApplicationException
import life.qbic.projectmanagement.application.pinned.PinnedProject
import life.qbic.projectmanagement.domain.model.project.ProjectId
import spock.lang.Specification

import java.time.Instant

/**
 * Unit tests for the JPA pin store, focusing on the guards that the composite key alone cannot
 * express: the store must never overwrite an existing pin and must never touch the database for an
 * unusable user id.
 */
class PinnedProjectStoreImplementationSpec extends Specification {

  static final String USER_ID = "3fa85f64-5717-4562-b3fc-2c963f66afa6"

  PinnedProjectRepository repository = Mock()
  PinnedProjectStoreImplementation store = new PinnedProjectStoreImplementation(repository)

  def "refuses to overwrite an existing pin instead of replacing its label and pin time"() {
    given: "the same user already pinned the same project"
    def projectId = ProjectId.create()
    repository.isPinned(USER_ID, projectId.value()) >> true

    when:
    store.add(PinnedProject.create(USER_ID, projectId, Instant.now(), "2024_001", "A title"))

    then: "nothing is written"
    0 * repository.saveAndFlush(_)
    thrown ApplicationException
  }

  def "stores a pin that does not exist yet"() {
    given:
    def projectId = ProjectId.create()
    def newPin = PinnedProject.create(USER_ID, projectId, Instant.now(), "2024_001", "A title")
    repository.isPinned(USER_ID, projectId.value()) >> false

    when:
    store.add(newPin)

    then:
    1 * repository.saveAndFlush(newPin)
  }

  def "reads and counts nothing for a missing user id, without touching the database"() {
    when:
    def found = store.findByUserId(missingUserId)
    def count = store.countByUserId(missingUserId)

    then:
    found.isEmpty()
    count == 0
    0 * repository.findAllOfUser(_)
    0 * repository.countAllOfUser(_)

    where:
    missingUserId << [null, "", "   "]
  }

  def "removes a pin and reports whether a row was actually deleted"() {
    given: "the database reports the number of deleted rows"
    def projectId = ProjectId.create()
    repository.removePin(USER_ID, projectId.value()) >> deletedRows

    expect:
    store.remove(USER_ID, projectId) == removed

    where:
    deletedRows | removed
    1           | true
    0           | false
  }

  def "looks up a single pin of the user"() {
    given:
    def projectId = ProjectId.create()
    def pin = PinnedProject.create(USER_ID, projectId, Instant.now(), "2024_001", "A title")
    repository.findPin(USER_ID, projectId.value()) >> [pin]

    expect:
    store.find(USER_ID, projectId).get() == pin
  }
}
