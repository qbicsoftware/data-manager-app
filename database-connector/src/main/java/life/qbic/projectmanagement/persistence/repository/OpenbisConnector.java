package life.qbic.projectmanagement.persistence.repository;

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
import java.util.Objects;
import life.qbic.logging.api.Logger;
import life.qbic.openbis.openbisclient.OpenBisClient;
import life.qbic.projectmanagement.domain.project.ProjectCode;
import life.qbic.projectmanagement.domain.project.experiment.repository.ExperimentalDesignVocabularyRepository;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Species;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Specimen;
import life.qbic.projectmanagement.persistence.QbicProjectDataRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

import static life.qbic.logging.service.LoggerFactory.logger;

/**
 * Basic implementation to query project preview information
 *
 * @since 1.0.0
 */
@Component
public class OpenbisConnector implements ExperimentalDesignVocabularyRepository,
    QbicProjectDataRepo {

  private static final Logger log = logger(OpenbisConnector.class);

  private final OpenBisClient openBisClient;

  private static final String DEFAULT_SPACE_CODE = "DATA_MANAGER_SPACE";

  // used by spring to wire it up
  private OpenbisConnector(@Value("${openbis.user.name}") String userName,
      @Value("${openbis.user.password}") String password,
      @Value("${openbis.datasource.url}") String url) {
    openBisClient = new OpenBisClient(
        userName, password, url);
    try {
      login();
    } catch (RuntimeException e) {
      if (!(e instanceof ConnectionException)) {
        log.error("Unexpected runtime exception", e);
      }
      throw new RuntimeException("Could not establish a connection to a data connector.");
    }
  }

  private void login() throws RuntimeException {
    try {
      openBisClient.login();
    } catch (Exception e) {
      // login must not throw any exceptions.
      // so if we log it and return a more generic exception to not expose
      // implementation details
      log.error("Connection to openBIS was not established", e);
      throw new ConnectionException();
    }
    // If the connection is not active, fail early
    if (isNotConnected()) {
      log.error("Login to openBIS was not successful, correct credentials?");
      throw new ConnectionException();
    }
  }

  private boolean isNotConnected() {
    return Objects.isNull(openBisClient.getSessionToken()) || openBisClient.getSessionToken()
        .isEmpty();
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
  public List<Species> retrieveSpecies() {
    return getVocabularyTermsForCode(VocabularyCode.SPECIES).stream()
        .map(it -> it.label().isBlank() ? it.code() : it.label())
        .map(Species::new).toList();
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

  private void createOpenbisProject(String spaceCodeString, ProjectCode projectCode) {
    ProjectCreation project = new ProjectCreation();
    project.setCode(projectCode.value());
    project.setSpaceId(new SpacePermId(spaceCodeString));

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

  @Override
  public void add(ProjectCode projectCode) {
    createOpenbisProject(DEFAULT_SPACE_CODE, projectCode);
  }

  @Override
  public boolean projectExists(ProjectCode projectCode) {
    return !searchProjectsByCode(projectCode.toString()).isEmpty();
  }

  // Convenience RTE to describe connection issues
  class ConnectionException extends RuntimeException {
  }

}
