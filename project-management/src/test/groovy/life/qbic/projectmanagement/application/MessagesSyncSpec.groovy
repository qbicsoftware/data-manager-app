package life.qbic.projectmanagement.application

import spock.lang.Specification

/**
 * Unit tests for the combined dataset-sync email templates
 * (DATSET-04/08, ADR-0005 N1).
 *
 * @since 1.13.0
 */
class MessagesSyncSpec extends Specification {

  def "updatedRecordLine renders version progression"() {
    expect:
    Messages.updatedRecordLine("My Dataset", "10.5281/zenodo.1", "v1", "v2", false)
        == "My Dataset (10.5281/zenodo.1): v1 → v2"
  }

  def "updatedRecordLine annotates access status changes"() {
    expect:
    Messages.updatedRecordLine("My Dataset", "10.5281/zenodo.1", "v1", "v2", true)
        == "My Dataset (10.5281/zenodo.1): v1 → v2 (access status changed)"
  }

  def "updatedRecordLine handles missing versions"() {
    expect:
    Messages.updatedRecordLine("My Dataset", "10.5281/zenodo.1", null, null, false)
        == "My Dataset (10.5281/zenodo.1): —"
  }

  def "datasetsSyncedToProject lists all updated records in one message"() {
    given:
    def records = [
        Messages.updatedRecordLine("A", "10.5281/zenodo.a", "v1", "v2", false),
        Messages.updatedRecordLine("B", "10.5281/zenodo.b", "v2", "v3", true)
    ]

    when:
    def message = Messages.datasetsSyncedToProject("Grace Hopper", "My Project", records,
        "https://datamanager.example/projects/1")

    then:
    message.contains("Dear Grace Hopper")
    message.contains("in the project 'My Project'")
    message.contains("A (10.5281/zenodo.a): v1 → v2")
    message.contains("B (10.5281/zenodo.b): v2 → v3 (access status changed)")
    message.contains("https://datamanager.example/projects/1")
  }
}