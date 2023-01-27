package life.qbic.projectmanagement.persistence.repository;

import static life.qbic.logging.service.LoggerFactory.logger;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.operation.IOperation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.operation.SynchronousOperationExecutionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.create.CreateProjectsOperation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.create.ProjectCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.SpacePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.VocabularyTerm;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.fetchoptions.VocabularyTermFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.search.VocabularyTermSearchCriteria;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import life.qbic.logging.api.Logger;
import life.qbic.openbis.openbisclient.OpenBisClient;
import life.qbic.projectmanagement.application.ProjectCreationService;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.ProjectCode;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.repository.ExperimentalDesignVocabularyRepository;
import life.qbic.projectmanagement.domain.project.repository.ProjectDataRepository;
import life.qbic.projectmanagement.domain.project.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.project.vocabulary.ControlledVocabulary;
import life.qbic.projectmanagement.domain.project.vocabulary.Species;
import life.qbic.projectmanagement.domain.project.vocabulary.Specimen;
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

  public OpenbisConnector(@Value("${openbis.user.name}") String userName,
      @Value("${openbis.user.password}") String password,
      @Value("${openbis.datasource.url}") String url) {
    openBisClient = new OpenBisClient(
        userName, password, url);
    openBisClient.login();
  }

  public SearchResult<VocabularyTerm> searchVocabularyTerms(VocabularyTermSearchCriteria criteria,
      VocabularyTermFetchOptions options) {
    return openBisClient.getV3()
        .searchVocabularyTerms(openBisClient.getSessionToken(), criteria, options);
  }

  private ControlledVocabulary getVocabularyForCode(String vocabularyCode) {
    VocabularyTermSearchCriteria criteria = new VocabularyTermSearchCriteria();
    criteria.withVocabulary().withCode().thatEquals(vocabularyCode);

    VocabularyTermFetchOptions options = new VocabularyTermFetchOptions();
    SearchResult<VocabularyTerm> searchResult =
        searchVocabularyTerms(criteria, options);

    Map<String, String> termsByLabel = new HashMap<>();
    for (VocabularyTerm term : searchResult.getObjects()) {
      String code = term.getCode();
      // we assign code to label, in case there is no label to display
      String label = code;
      if (term.getLabel() != null && !term.getLabel().isEmpty()) {
        label = term.getLabel();
      }
      termsByLabel.put(label, code);
    }
    return new ControlledVocabulary(vocabularyCode, termsByLabel);
  }

  public String getCodeForVocabularyTermLabel(String vocabularyCode, String termLabel) {
    return getVocabularyForCode(vocabularyCode).getVocabularyTermCode(termLabel);
  }

  @Override
  public List<Species> retrieveOrganisms() {
    List<Species> organisms = new ArrayList<>();
    for (String label : getVocabularyForCode(
        OpenbisVocabularyCodes.Q_NCBI_TAXONOMY.toString()).getVocabularyTermLabels()) {
      organisms.add(Species.create(label));
    }
    return organisms;
  }

  @Override
  public List<Specimen> retrieveSpecimens() {
    List<Specimen> specimen = new ArrayList<>();
    for (String label : getVocabularyForCode(
        OpenbisVocabularyCodes.Q_PRIMARY_TISSUES.toString()).getVocabularyTermLabels()) {
      specimen.add(Specimen.create(label));
    }
    return specimen;
  }

  @Override
  public List<Analyte> retrieveAnalytes() {
    List<Analyte> analyte = new ArrayList<>();
    for (String label : getVocabularyForCode(
        OpenbisVocabularyCodes.Q_SAMPLE_TYPES.toString()).getVocabularyTermLabels()) {
      analyte.add(Analyte.create(label));
    }
    return analyte;
  }

  private void createOpenbisProject(ProjectSpace space, ProjectCode projectCode, String description) {
    ProjectCreation project = new ProjectCreation()
    project.setCode(projectCode.toString())
    project.setSpaceId(new SpacePermId(space.toString()))
    project.setDescription(description)

    IOperation operation = new CreateProjectsOperation(project)
    handleOperations(operation)
  }
  private void handleOperations(IOperation operation) {
    IApplicationServerApi api = openBisClient.getV3();

    SynchronousOperationExecutionOptions executionOptions = new SynchronousOperationExecutionOptions()
    List<IOperation> operationOptions = Arrays.asList(operation);
    try {
      api.executeOperations(openBisClient.getSessionToken(), operationOptions, executionOptions)
    } catch (Exception e) {
      log.error("Unexpected exception during openBIS operation.", e);
      throw e
    }
  }

  /**
   * Saves a {@link Project} entity permanently.
   *
   * @param project the project to store
   * @since 1.0.0
   */
  @Override
  public void add(Project project) {

  }

  /**
   * Searches for projects that contain the provided project code
   *
   * @param projectCode the project code to search for in projects
   * @return projects that contain the project code
   * @since 1.0.0
   */
  @Override
  public List<Project> find(ProjectCode projectCode) {
    return null;
  }

}
