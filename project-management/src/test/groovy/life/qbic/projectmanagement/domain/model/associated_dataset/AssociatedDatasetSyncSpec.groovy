package life.qbic.projectmanagement.domain.model.associated_dataset

import life.qbic.domain.concepts.LocalDomainEventDispatcher
import life.qbic.projectmanagement.domain.model.project.ProjectId
import spock.lang.Specification

/**
 * Unit tests for {@link AssociatedDataset#sync(ResourceMetadata)} — the
 * change-diff used by dataset synchronisation (DATSET-04/08, ADR-0005).
 *
 * @since 1.13.0
 */
class AssociatedDatasetSyncSpec extends Specification {

  private static final String PROJECT_UUID = "0270ce7f-4092-40e3-9c4c-ce7adb688bf5"
  private static final ProjectId PROJECT = ProjectId.parse(PROJECT_UUID)

  private static final String PARENT_HANDLE = "parent-1"

  def setup() {
    LocalDomainEventDispatcher.instance().reset()
  }

  private static AssociatedDataset connected(InvenioRdmResourceMetadata metadata) {
    AssociatedDataset.connect(
        PROJECT, SourceType.INVENIO_RDM,
        new ExternalHandle("ext-1"),
        metadata, "user-1", null)
  }

  private static InvenioRdmResourceMetadata metadata(
      String version,
      InvenioRdmAccessStatus recordAccess = InvenioRdmAccessStatus.PUBLIC,
      InvenioRdmAccessStatus fileAccess = InvenioRdmAccessStatus.PUBLIC,
      String pid = "10.5281/zenodo.1") {
    new InvenioRdmResourceMetadata(
        "Test Dataset", pid, version, "https://zenodo.org/records/123",
        "Zenodo", [], "Dataset", null,
        java.time.LocalDate.of(2025, 1, 15), null,
        recordAccess, fileAccess, "zenodo", null, PARENT_HANDLE)
  }

  def "sync reports no change for an identical snapshot and stamps lastSyncedAt"() {
    given:
    def dataset = connected(metadata("v1"))

    when:
    def change = dataset.sync(metadata("v1"))

    then:
    !change.metadataChanged()
    !change.versionChanged()
    !change.accessStatusChanged()
    change.previousVersion() == "v1"
    change.newVersion() == "v1"
    dataset.lastSyncedAt().isPresent()
  }

  def "sync reports a version change with previous and new version"() {
    given:
    def dataset = connected(metadata("v1"))

    when:
    def change = dataset.sync(metadata("v2", InvenioRdmAccessStatus.PUBLIC,
        InvenioRdmAccessStatus.PUBLIC, "10.5281/zenodo.2"))

    then:
    change.metadataChanged()
    change.versionChanged()
    change.previousVersion() == "v1"
    change.newVersion() == "v2"
    dataset.version() == "v2"
    dataset.pid() == "10.5281/zenodo.2"
  }

  def "sync reports an access status change"() {
    given:
    def dataset = connected(metadata("v1", InvenioRdmAccessStatus.RESTRICTED,
        InvenioRdmAccessStatus.RESTRICTED))

    when: "the embargo is lifted (restricted → public)"
    def change = dataset.sync(metadata("v1", InvenioRdmAccessStatus.PUBLIC,
        InvenioRdmAccessStatus.PUBLIC))

    then:
    change.metadataChanged()
    change.accessStatusChanged()
    dataset.accessLevel() == AccessLevel.PUBLIC
  }

  def "sync keeps universal columns in sync with the snapshot"() {
    given:
    def dataset = connected(metadata("v1"))

    when:
    dataset.sync(metadata("v3", InvenioRdmAccessStatus.RESTRICTED,
        InvenioRdmAccessStatus.PUBLIC, "10.5281/zenodo.3"))

    then:
    dataset.title() == "Test Dataset"
    dataset.pid() == "10.5281/zenodo.3"
    dataset.version() == "v3"
    dataset.accessLevel() == AccessLevel.RESTRICTED
    dataset.resourceMetadata() instanceof InvenioRdmResourceMetadata
  }

  def "sync rejects null metadata"() {
    given:
    def dataset = connected(metadata("v1"))

    when:
    dataset.sync(null)

    then:
    thrown(NullPointerException)
  }

  def "updateMetadata delegates to sync and still updates lastSyncedAt"() {
    given:
    def dataset = connected(metadata("v1"))

    when:
    dataset.updateMetadata(metadata("v2"))

    then:
    dataset.version() == "v2"
    dataset.lastSyncedAt().isPresent()
  }
}