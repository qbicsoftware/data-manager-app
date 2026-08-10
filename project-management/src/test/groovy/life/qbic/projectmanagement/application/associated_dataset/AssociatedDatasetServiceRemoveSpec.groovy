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
 * Unit tests for {@link AssociatedDatasetService#removeDataset}.
 *
 * @since 1.12.0
 */
class AssociatedDatasetServiceRemoveSpec extends Specification {

  private static final String VALID_PROJECT_ID = "0270ce7f-4092-40e3-9c4c-ce7adb688bf5"

  def "removeDataset returns success for a valid connected dataset"() {
    given:
    def datasetId = AssociatedDatasetId.create()
    def projectId = ProjectId.parse(VALID_PROJECT_ID)
    def userId = "user-1"
    def dataset = createConnectedDataset(datasetId, projectId, userId)
    def repository = Mock(AssociatedDatasetRepository) {
      findById(datasetId) >> Optional.of(dataset)
      save(_) >> { }
    }
    def registry = Mock(SourceInstanceRegistry)
    def projectService = Mock(ProjectInformationService)
    def userService = Mock(UserInformationService)
    def experimentService = Mock(ExperimentInformationService)
    def source = Mock(DatasetSource)
    def service = new AssociatedDatasetService(
        source, repository, registry, projectService, userService, experimentService)

    // Ensure domain event dispatcher is clean
    LocalDomainEventDispatcher.instance().reset()

    when:
    def result = service.removeDataset(datasetId.value(), userId)

    then:
    result instanceof Result.Value
    result.getValue() == datasetId
    dataset.connectionState() == ConnectionState.REMOVED
    1 * repository.save(dataset)
  }

  def "removeDataset returns DATASET_NOT_FOUND when dataset does not exist"() {
    given:
    def datasetId = AssociatedDatasetId.create()
    def repository = Mock(AssociatedDatasetRepository) {
      findById(datasetId) >> Optional.empty()
    }
    def registry = Mock(SourceInstanceRegistry)
    def projectService = Mock(ProjectInformationService)
    def userService = Mock(UserInformationService)
    def experimentService = Mock(ExperimentInformationService)
    def source = Mock(DatasetSource)
    def service = new AssociatedDatasetService(
        source, repository, registry, projectService, userService, experimentService)

    when:
    def result = service.removeDataset(datasetId.value(), "user-1")

    then:
    result instanceof Result.Error
    result.getError() == RemoveDatasetError.DATASET_NOT_FOUND
  }

  def "removeDataset returns DATASET_ALREADY_REMOVED when dataset is already in REMOVED state"() {
    given:
    def datasetId = AssociatedDatasetId.create()
    def projectId = ProjectId.parse(VALID_PROJECT_ID)
    def userId = "user-1"
    def dataset = createConnectedDataset(datasetId, projectId, userId)
    // Pre-set to REMOVED state (direct field access for test)
    dataset.connectionState = ConnectionState.REMOVED
    def repository = Mock(AssociatedDatasetRepository) {
      findById(datasetId) >> Optional.of(dataset)
    }
    def registry = Mock(SourceInstanceRegistry)
    def projectService = Mock(ProjectInformationService)
    def userService = Mock(UserInformationService)
    def experimentService = Mock(ExperimentInformationService)
    def source = Mock(DatasetSource)
    def service = new AssociatedDatasetService(
        source, repository, registry, projectService, userService, experimentService)

    when:
    def result = service.removeDataset(datasetId.value(), userId)

    then:
    result instanceof Result.Error
    result.getError() == RemoveDatasetError.DATASET_ALREADY_REMOVED
    0 * repository.save(_)
  }

  def "removeDataset returns REMOVAL_FAILED when persistence throws"() {
    given:
    def datasetId = AssociatedDatasetId.create()
    def projectId = ProjectId.parse(VALID_PROJECT_ID)
    def userId = "user-1"
    def dataset = createConnectedDataset(datasetId, projectId, userId)
    def repository = Mock(AssociatedDatasetRepository) {
      findById(datasetId) >> Optional.of(dataset)
      save(_) >> { throw new RuntimeException("DB error") }
    }
    def registry = Mock(SourceInstanceRegistry)
    def projectService = Mock(ProjectInformationService)
    def userService = Mock(UserInformationService)
    def experimentService = Mock(ExperimentInformationService)
    def source = Mock(DatasetSource)
    def service = new AssociatedDatasetService(
        source, repository, registry, projectService, userService, experimentService)

    when:
    def result = service.removeDataset(datasetId.value(), userId)

    then:
    result instanceof Result.Error
    result.getError() == RemoveDatasetError.REMOVAL_FAILED
  }

  def "removeDataset throws NullPointerException for null associatedDatasetId"() {
    given:
    def service = createServiceWithEmptyRepo()

    when:
    service.removeDataset(null, "user-1")

    then:
    thrown(NullPointerException)
  }

  def "removeDataset throws NullPointerException for null removedByUserId"() {
    given:
    def service = createServiceWithEmptyRepo()

    when:
    service.removeDataset("dataset-id", null)

    then:
    thrown(NullPointerException)
  }

  private AssociatedDatasetService createServiceWithEmptyRepo() {
    def repository = Mock(AssociatedDatasetRepository) {
      findById(_) >> Optional.empty()
    }
    new AssociatedDatasetService(
        Mock(DatasetSource), repository, Mock(SourceInstanceRegistry),
        Mock(ProjectInformationService), Mock(UserInformationService),
        Mock(ExperimentInformationService))
  }

  private static AssociatedDataset createConnectedDataset(
      AssociatedDatasetId id, ProjectId projectId, String connectedBy) {
    new AssociatedDataset(
        id, projectId, SourceType.INVENIO_RDM, new ExternalHandle("ext-1"),
        new InvenioRdmResourceMetadata(
            "Test Dataset", "10.1234/test", "v1",
            "https://zenodo.org/records/12345",
            "Zenodo", [], "Dataset", "QBiC",
            java.time.LocalDate.of(2025, 1, 15), null,
            InvenioRdmAccessStatus.PUBLIC, InvenioRdmAccessStatus.PUBLIC),
        connectedBy, null)
  }
}
