package life.qbic.projectmanagement.persistence.repository;

import static life.qbic.logging.service.LoggerFactory.logger;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.operation.IOperation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.operation.SynchronousOperationExecutionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.create.CreateProjectsOperation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.create.ProjectCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.fetchoptions.ProjectFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.search.ProjectSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.SpacePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.fetchoptions.VocabularyTermFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.search.VocabularyTermSearchCriteria;
import java.util.Arrays;
import java.util.List;
import life.qbic.logging.api.Logger;
import life.qbic.openbis.openbisclient.OpenBisClient;
import life.qbic.projectmanagement.domain.project.ProjectCode;
import life.qbic.projectmanagement.domain.project.experiment.repository.ExperimentalDesignVocabularyRepository;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Organism;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Specimen;

import life.qbic.projectmanagement.domain.project.repository.ProjectDataRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Basic implementation to query project preview information
 *
 * @since 1.0.0
 */
@Component
public class OpenbisConnector implements ExperimentalDesignVocabularyRepository,
    ProjectDataRepository {

  private static final Logger log = logger(OpenbisConnector.class);

  private final OpenBisClient openBisClient;

  private final String DEFAULT_SPACE_CODE = "DATA_MANAGER_SPACE";

  private OpenbisConnector(@Value("${openbis.user.name}") String userName,
      @Value("${openbis.user.password}") String password,
      @Value("${openbis.datasource.url}") String url) {
    openBisClient = new OpenBisClient(
        userName, password, url);
    openBisClient.login();
  }

  private List<VocabularyTerm> getVocabularyTermsForCode(VocabularyCode vocabularyCode) {
    VocabularyTermSearchCriteria criteria = new VocabularyTermSearchCriteria();
    criteria.withVocabulary().withCode().thatEquals(vocabularyCode.openbisCode());

    VocabularyTermFetchOptions options = new VocabularyTermFetchOptions();
    SearchResult<ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.VocabularyTerm> searchResult =
        openBisClient.getV3()
            .searchVocabularyTerms(openBisClient.getSessionToken(), criteria, options);

    return searchResult.getObjects().stream()
        .map(it -> new VocabularyTerm(it.getCode(), it.getLabel(), it.getDescription()))
        .toList();
  }

  private List<Project> searchProjectsByCode(String code) {
    ProjectSearchCriteria criteria = new ProjectSearchCriteria();
    criteria.withCode().thatEquals(code);

    ProjectFetchOptions options = new ProjectFetchOptions();
    SearchResult<Project> searchResult =
        openBisClient.getV3().searchProjects(openBisClient.getSessionToken(), criteria, options);

    return searchResult.getObjects();
  }

  @Override
  public List<Organism> retrieveOrganisms() {
    return getVocabularyTermsForCode(VocabularyCode.ORGANISM).stream()
        .map(it -> it.label().isBlank() ? it.code() : it.label())
        .map(Organism::new).toList();
  }

  @Override
  public List<Specimen> retrieveSpecimens() {
    return getVocabularyTermsForCode(VocabularyCode.SPECIMEN).stream()
        .map(it -> it.label().isBlank() ? it.code() : it.label())
        .map(Specimen::new).toList();
  }

  @Override
  public List<Analyte> retrieveAnalytes() {
    return getVocabularyTermsForCode(VocabularyCode.ANALYTE).stream()
        .map(it -> it.label().isBlank() ? it.code() : it.label())
        .map(Analyte::new).toList();
  }

  record VocabularyTerm(String code, String label, String description) {

  }

  private void createOpenbisProject(String spaceCodeString, ProjectCode projectCode, String description) {
    ProjectCreation project = new ProjectCreation();
    project.setCode(projectCode.toString());
    project.setSpaceId(new SpacePermId(spaceCodeString));
    project.setDescription(description);

    IOperation operation = new CreateProjectsOperation(project);
    handleOperations(operation);
  }

  private void handleOperations(IOperation operation) {
    IApplicationServerApi api = openBisClient.getV3();

    SynchronousOperationExecutionOptions executionOptions = new SynchronousOperationExecutionOptions();
    List<IOperation> operationOptions = Arrays.asList(operation);
    try {
      api.executeOperations(openBisClient.getSessionToken(), operationOptions, executionOptions);
    } catch (Exception e) {
      log.error("Unexpected exception during openBIS operation.", e);
      throw e;
    }
  }

  /**
   * Saves a {@link Project} entity permanently.
   *
   * @param project the project to store
   * @since 1.0.0
   */
  @Override
  public void add(life.qbic.projectmanagement.domain.project.Project project) {
    createOpenbisProject(DEFAULT_SPACE_CODE, project.getProjectCode(), project.getProjectIntent().experimentalDesign().toString());
  }

  /**
   * Searches for projects that contain the provided project code
   *
   * @param projectCode the project code to search for in projects
   * @return projects that contain the project code
   * @since 1.0.0
   */
  @Override
  public boolean projectExists(ProjectCode projectCode) {
    return !searchProjectsByCode(projectCode.toString()).isEmpty();
  }

}
