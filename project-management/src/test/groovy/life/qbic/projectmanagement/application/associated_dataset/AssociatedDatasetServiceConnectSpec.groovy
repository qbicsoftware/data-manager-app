package life.qbic.projectmanagement.application.associated_dataset

import life.qbic.application.commons.Result
import life.qbic.domain.concepts.LocalDomainEventDispatcher
import life.qbic.identity.api.UserInformationService
import life.qbic.projectmanagement.application.ProjectInformationService
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService
import life.qbic.projectmanagement.domain.model.associated_dataset.*
import life.qbic.projectmanagement.domain.model.associated_dataset.repository.AssociatedDatasetRepository
import life.qbic.projectmanagement.domain.model.project.ProjectId
import spock.lang.Specification

/**
 * Unit tests for the credential gate in
 * {@link AssociatedDatasetService#connectDataset}.
 *
 * <p>Verifies that connecting an access-restricted dataset is blocked
 * when the user has no valid credential for the source instance.</p>
 *
 * @since 1.12.0
 */
class AssociatedDatasetServiceConnectSpec extends Specification {

  private static final String VALID_PROJECT_ID = "0270ce7f-4092-40e3-9c4c-ce7adb688bf5"

  def "connectDataset succeeds for a public dataset without credential check"() {
    given:
    def projectId = ProjectId.parse(VALID_PROJECT_ID)
    def userId = "user-1"
    def config = new InstanceConfig("zenodo", "Zenodo", "https://zenodo.org")
    def publicMetadata = createMetadata(InvenioRdmAccessStatus.PUBLIC, InvenioRdmAccessStatus.PUBLIC)

    def source = Mock(DatasetSource) {
      resolveMetadata("ext-1", config, userId) >> Optional.of(publicMetadata)
    }
    def repository = Mock(AssociatedDatasetRepository) {
      isActiveConnectionPresent(projectId, _) >> false
      save(_) >> { }
    }
    def registry = Mock(SourceInstanceRegistry) {
      find("zenodo") >> Optional.of(
          new SourceInstanceDescriptor("zenodo", "Zenodo",
              "https://zenodo.org", SourceType.INVENIO_RDM))
    }
    def service = createService(source, repository, registry)

    LocalDomainEventDispatcher.instance().reset()

    when:
    def result = service.connectDataset(
        projectId, SourceType.INVENIO_RDM, "zenodo",
        "ext-1", null, userId)

    then:
    result instanceof Result.Value
    // Credential check is NOT invoked for public datasets
    0 * source.hasValidCredential(_, _)
  }

  def "connectDataset succeeds for a restricted dataset when valid credential exists"() {
    given:
    def projectId = ProjectId.parse(VALID_PROJECT_ID)
    def userId = "user-1"
    def config = new InstanceConfig("zenodo", "Zenodo", "https://zenodo.org")
    def restrictedMetadata = createMetadata(InvenioRdmAccessStatus.RESTRICTED, InvenioRdmAccessStatus.RESTRICTED)

    def source = Mock(DatasetSource) {
      resolveMetadata("ext-1", config, userId) >> Optional.of(restrictedMetadata)
      hasValidCredential(userId, config) >> true
    }
    def repository = Mock(AssociatedDatasetRepository) {
      isActiveConnectionPresent(projectId, _) >> false
      save(_) >> { }
    }
    def registry = Mock(SourceInstanceRegistry) {
      find("zenodo") >> Optional.of(
          new SourceInstanceDescriptor("zenodo", "Zenodo",
              "https://zenodo.org", SourceType.INVENIO_RDM))
    }
    def service = createService(source, repository, registry)

    LocalDomainEventDispatcher.instance().reset()

    when:
    def result = service.connectDataset(
        projectId, SourceType.INVENIO_RDM, "zenodo",
        "ext-1", null, userId)

    then:
    result instanceof Result.Value
  }

  def "connectDataset returns CREDENTIAL_REQUIRED for restricted dataset without credential"() {
    given:
    def projectId = ProjectId.parse(VALID_PROJECT_ID)
    def userId = "user-1"
    def config = new InstanceConfig("zenodo", "Zenodo", "https://zenodo.org")
    def restrictedMetadata = createMetadata(InvenioRdmAccessStatus.RESTRICTED, InvenioRdmAccessStatus.RESTRICTED)

    def source = Mock(DatasetSource) {
      resolveMetadata("ext-1", config, userId) >> Optional.of(restrictedMetadata)
      hasValidCredential(userId, config) >> false
    }
    def repository = Mock(AssociatedDatasetRepository)
    def registry = Mock(SourceInstanceRegistry) {
      find("zenodo") >> Optional.of(
          new SourceInstanceDescriptor("zenodo", "Zenodo",
              "https://zenodo.org", SourceType.INVENIO_RDM))
    }
    def service = createService(source, repository, registry)

    when:
    def result = service.connectDataset(
        projectId, SourceType.INVENIO_RDM, "zenodo",
        "ext-1", null, userId)

    then:
    result instanceof Result.Error
    result.getError() == ConnectDatasetError.CREDENTIAL_REQUIRED
    // Duplicate check and save are never reached
    0 * repository.isActiveConnectionPresent(_, _)
    0 * repository.save(_)
  }

  def "connectDataset returns CREDENTIAL_REQUIRED for restricted dataset with invalidated credential"() {
    given:
    def projectId = ProjectId.parse(VALID_PROJECT_ID)
    def userId = "user-1"
    def config = new InstanceConfig("zenodo", "Zenodo", "https://zenodo.org")
    // Record is public but files are restricted → overall RESTRICTED
    def restrictedMetadata = createMetadata(InvenioRdmAccessStatus.PUBLIC, InvenioRdmAccessStatus.RESTRICTED)

    def source = Mock(DatasetSource) {
      resolveMetadata("ext-1", config, userId) >> Optional.of(restrictedMetadata)
      hasValidCredential(userId, config) >> false
    }
    def repository = Mock(AssociatedDatasetRepository)
    def registry = Mock(SourceInstanceRegistry) {
      find("zenodo") >> Optional.of(
          new SourceInstanceDescriptor("zenodo", "Zenodo",
              "https://zenodo.org", SourceType.INVENIO_RDM))
    }
    def service = createService(source, repository, registry)

    when:
    def result = service.connectDataset(
        projectId, SourceType.INVENIO_RDM, "zenodo",
        "ext-1", null, userId)

    then:
    result instanceof Result.Error
    result.getError() == ConnectDatasetError.CREDENTIAL_REQUIRED
  }

  def "searchDatasets with accessFilter passes filter to DatasetSource"() {
    given:
    def source = Mock(DatasetSource)
    def registry = Mock(SourceInstanceRegistry) {
      find("zenodo") >> Optional.of(
          new SourceInstanceDescriptor("zenodo", "Zenodo",
              "https://zenodo.org", SourceType.INVENIO_RDM))
    }
    def service = createService(source, Mock(AssociatedDatasetRepository), registry)
    def expectedResult = new SearchResult([], 0, 0, 10)

    when:
    service.searchDatasets(SourceType.INVENIO_RDM, "zenodo",
        "proteomics", "restricted", 0, 10, "user-1")

    then:
    1 * source.search({ SearchQuery q ->
      q.effectiveQuery() == "proteomics" &&
      q.accessFilter() == "restricted" &&
      q.page() == 0 &&
      q.pageSize() == 10
    }, _, "user-1") >> expectedResult
  }

  def "searchDatasets without accessFilter passes null filter"() {
    given:
    def source = Mock(DatasetSource)
    def registry = Mock(SourceInstanceRegistry) {
      find("zenodo") >> Optional.of(
          new SourceInstanceDescriptor("zenodo", "Zenodo",
              "https://zenodo.org", SourceType.INVENIO_RDM))
    }
    def service = createService(source, Mock(AssociatedDatasetRepository), registry)
    def expectedResult = new SearchResult([], 0, 0, 10)

    when:
    service.searchDatasets(SourceType.INVENIO_RDM, "zenodo",
        "proteomics", 0, 10, "user-1")

    then:
    1 * source.search({ SearchQuery q ->
      q.effectiveQuery() == "proteomics" &&
      q.accessFilter() == null
    }, _, "user-1") >> expectedResult
  }

  // ── Helpers ──────────────────────────────────────────────────────

  private AssociatedDatasetService createService(
      DatasetSource source,
      AssociatedDatasetRepository repository,
      SourceInstanceRegistry registry) {
    new AssociatedDatasetService(
        source, repository, registry,
        Mock(ProjectInformationService),
        Mock(UserInformationService),
        Mock(ExperimentInformationService))
  }

  private static InvenioRdmResourceMetadata createMetadata(
      InvenioRdmAccessStatus recordAccess,
      InvenioRdmAccessStatus fileAccess) {
    new InvenioRdmResourceMetadata(
        "Test Dataset", "10.1234/test", "v1",
        "https://zenodo.org/records/12345",
        "Zenodo", [], "Dataset", "QBiC",
        java.time.LocalDate.of(2025, 1, 15), null,
        recordAccess, fileAccess)
  }
}
