package life.qbic.projectmanagement.infrastructure.sample.openbis;

import static life.qbic.logging.service.LoggerFactory.logger;
import static life.qbic.openbis.openbisclient.helper.OpenBisClientHelper.fetchSamplesCompletely;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.operation.IOperation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.deletion.id.IDeletionId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.create.CreateExperimentsOperation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.create.ExperimentCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.delete.ExperimentDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.ExperimentIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.search.ExperimentSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.operation.SynchronousOperationExecutionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.create.CreateProjectsOperation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.create.ProjectCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.delete.DeleteProjectsOperation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.delete.ProjectDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.fetchoptions.ProjectFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.id.ProjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.search.ProjectSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.create.CreateSamplesOperation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.create.SampleCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.delete.SampleDeletionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SampleIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.update.SampleUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.update.UpdateSamplesOperation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.SpacePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.fetchoptions.VocabularyTermFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.vocabulary.search.VocabularyTermSearchCriteria;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import life.qbic.logging.api.Logger;
import life.qbic.openbis.openbisclient.OpenBisClient;
import life.qbic.projectmanagement.domain.model.experiment.repository.ExperimentalDesignVocabularyRepository;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Species;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Specimen;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectCode;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;
import life.qbic.projectmanagement.infrastructure.project.QbicProjectDataRepo;
import life.qbic.projectmanagement.infrastructure.sample.QbicSampleDataRepo;
import life.qbic.projectmanagement.infrastructure.sample.translation.SimpleOpenBisTermMapper;
import life.qbic.projectmanagement.infrastructure.sample.translation.VocabularyCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Basic implementation to query project preview information
 *
 * @since 1.0.0
 */
@Component
public class OpenbisConnector implements ExperimentalDesignVocabularyRepository,
    QbicProjectDataRepo, QbicSampleDataRepo {

  private static final Logger log = logger(OpenbisConnector.class);

  private final OpenBisClient openBisClient;
  private static final String DEFAULT_SPACE_CODE = "DATA_MANAGER_SPACE";
  private static final String DEFAULT_SAMPLE_TYPE = "Q_TEST_SAMPLE";
  private static final String DEFAULT_EXPERIMENT_TYPE = "Q_SAMPLE_PREPARATION";
  private static final String DEFAULT_ANALYTE_TYPE = "OTHER";
  private static final String DEFAULT_DELETION_REASON = "Commanded by data manager app";

  private final AnalyteTermMapper analyteMapper = new SimpleOpenBisTermMapper();

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

  private List<Experiment> searchExperimentsByProjectCode(String projectCode,
      ExperimentFetchOptions fetchOptions) {
    ExperimentSearchCriteria criteria = new ExperimentSearchCriteria();
    criteria.withProject().withCode().thatEquals(projectCode);

    SearchResult<Experiment> searchResult =
        openBisClient.getV3().searchExperiments(openBisClient.getSessionToken(), criteria, fetchOptions);

    return searchResult.getObjects();
  }

  private List<ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project> searchProjectsByCode(
      String code) {
    ProjectSearchCriteria criteria = new ProjectSearchCriteria();
    criteria.withCode().thatEquals(code);

    ProjectFetchOptions options = new ProjectFetchOptions();
    SearchResult<ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project> searchResult =
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

  /**
   * Creates a reference to one or more {@link Sample}s in the data repository to connect project
   * data. A project must be provided, a unique experiment is created with each batch.
   *
   * @param project the {@link Project} for which samples should be created
   * @param samples the batch of {@link Sample}s to be created in the data repo
   * @since 1.0.0
   */
  public void addSamplesToProject(Project project,
      List<life.qbic.projectmanagement.domain.model.sample.Sample> samples) {
    String projectCodeString = project.getProjectCode().value();
    List<SampleCreation> samplesToRegister = new ArrayList<>();

    String newExperimentCode = findFreeExperimentCode(projectCodeString);
    createOpenbisExperiment(DEFAULT_SPACE_CODE, projectCodeString, newExperimentCode);
    ExperimentIdentifier newExperimentID = new ExperimentIdentifier(DEFAULT_SPACE_CODE,
        projectCodeString, newExperimentCode);

    try {
      samples.forEach(sample -> {
        SampleCreation sampleCreation = new SampleCreation();
        sampleCreation.setCode(sample.sampleCode().code());
        sampleCreation.setTypeId(new EntityTypePermId(DEFAULT_SAMPLE_TYPE));
        sampleCreation.setSpaceId(new SpacePermId(DEFAULT_SPACE_CODE));
        Map<String, String> props = new HashMap<>();

        props.put("Q_SECONDARY_NAME", sample.label());
        props.put("Q_EXTERNALDB_ID", sample.sampleId().value());
        String analyteValue = sample.sampleOrigin().getAnalyte().value();
        String openBisSampleType = retrieveOpenBisAnalyteCode(analyteValue).or(
                () -> analyteMapper.mapFrom(analyteValue)).orElse(DEFAULT_ANALYTE_TYPE);
        props.put("Q_SAMPLE_TYPE", openBisSampleType);
        if(openBisSampleType.equals(DEFAULT_ANALYTE_TYPE)) {
          logger("No mapping was found for " + analyteValue);
          logger("Using default value and adding " + analyteValue + " to Q_DETAILED_ANALYTE_TYPE.");
          props.put("Q_DETAILED_ANALYTE_TYPE", analyteValue);
        }
        sampleCreation.setProperties(props);

        sampleCreation.setExperimentId(newExperimentID);
        samplesToRegister.add(sampleCreation);
      });
      createOpenbisSamples(samplesToRegister);
    } catch (Exception e) {
      deleteOpenbisExperiment(newExperimentID);
      throw e;
    }
  }

  private Optional<String> retrieveOpenBisAnalyteCode(String analyteLabel) {
    return getVocabularyTermsForCode(VocabularyCode.ANALYTE).stream()
        .filter(vocabularyTerm -> analyteLabel.equals(vocabularyTerm.label))
        .map(vocabularyTerm -> vocabularyTerm.code).findFirst();
  }

  private String findFreeExperimentCode(String projectCode) {
    List<Experiment> experiments = searchExperimentsByProjectCode(projectCode, new ExperimentFetchOptions());
    int lastExperimentNumber = 0;
    for (Experiment experiment : experiments) {
      lastExperimentNumber = Integer.max(lastExperimentNumber,
          getTrailingNumber(experiment.getCode()));
    }
    String newExperimentNumber = Integer.toString(lastExperimentNumber + 1);
    return projectCode + "E" + newExperimentNumber;
  }

  private int getTrailingNumber(String input) {
    int lastNumberInt = 0;
    Pattern lastIntPattern = Pattern.compile("[^0-9]+([0-9]+)$");
    Matcher matcher = lastIntPattern.matcher(input);
    if (matcher.find()) {
      String someNumberStr = matcher.group(1);
      lastNumberInt = Integer.parseInt(someNumberStr);
    }
    return lastNumberInt;
  }

  private void createOpenbisSamples(List<SampleCreation> samplesToRegister) {
    IOperation operation = new CreateSamplesOperation(samplesToRegister);
    handleOperations(operation);
  }

  private void updateOpenbisSamples(List<SampleUpdate> samplesToUpdate) {
    IOperation operation = new UpdateSamplesOperation(samplesToUpdate);
    handleOperations(operation);
  }

  /**
   * Deletes a collection of samples with the provided codes from persistence. Checks if any of the
   * samples has attached data and fails the deletion of the sample batch, if so.
   *
   * @param projectCode the {@link ProjectCode} of the project these samples belong to
   * @param sampleCodes The {@link SampleCode}s of the samples to be deleted in the data repo

   * @since 1.0.0
   */
  @Override
  public void deleteAll(ProjectCode projectCode,
      Collection<SampleCode> sampleCodes) {

    Set<String> sampleCodesToDelete = new HashSet<>(
        sampleCodes.stream().map(SampleCode::code).toList());

    //Fetch samples with potential data - sample search is not working, sorry you have to see this
    ExperimentFetchOptions fetchOptions = new ExperimentFetchOptions();
    fetchOptions.withSamplesUsing(fetchSamplesCompletely());
    for (Experiment experiment : searchExperimentsByProjectCode(projectCode.value(), fetchOptions)) {
      for (Sample sample : experiment.getSamples()) {
        String sampleCode = sample.getCode();
        if (sampleCodesToDelete.contains(sampleCode)) {
          if (isSampleWithData(List.of(sample))) {
            throw new SampleNotDeletedException(
                "Did not delete sample " + sampleCode + ", because data is attached.");
          }
        }
      }
    }
    // no data found, we can safely delete all samples
    sampleCodesToDelete.forEach(code -> deleteOpenbisSample(DEFAULT_SPACE_CODE, code));
  }

  /**
   * Recursive method checking child samples for datasets
   */
  private boolean isSampleWithData(List<Sample> samples) {
    boolean hasData = false;
    for (Sample sample : samples) {
      hasData |= !sample.getDataSets().isEmpty();
      hasData |= isSampleWithData(sample.getChildren());
    }
    return hasData;
  }

  /**
   * Updates the reference to one or more {@link Sample}s in the data repository to connect project
   * data. Samples with metadata must be provided. Since no batch information is stored, changes in
   * the batch are not reflected.
   *
   * @param samples the batch of {@link Sample}s to be updated in the data repo
   * @since 1.0.0
   */
  @Override
  public void updateAll(
      Collection<life.qbic.projectmanagement.domain.model.sample.Sample> samples)
      throws SampleNotUpdatedException {
    try {
      updateOpenbisSamples(convertSamplesToSampleUpdates(samples));
    } catch (RuntimeException e) {
      throw new SampleNotUpdatedException(
          "Samples could not be updated due to " + e.getCause() + " with " + e.getMessage());
    }
  }

  private SampleUpdate createSampleUpdate(life.qbic.projectmanagement.domain.model.sample.Sample sample) {
    SampleUpdate sampleUpdate = new SampleUpdate();
    String sampleId = "/" + DEFAULT_SPACE_CODE + "/" + sample.sampleCode().code();
    sampleUpdate.setSampleId(new SampleIdentifier(sampleId));
    sampleUpdate.setProperty("Q_SECONDARY_NAME", sample.label());
    sampleUpdate.setProperty("Q_EXTERNALDB_ID", sample.sampleId().value());

    String analyteValue = sample.sampleOrigin().getAnalyte().value();

    String openBisSampleType = retrieveOpenBisAnalyteCode(analyteValue).or(
        () -> analyteMapper.mapFrom(analyteValue)).orElse(DEFAULT_ANALYTE_TYPE);
    sampleUpdate.setProperty("Q_SAMPLE_TYPE", openBisSampleType);
    if (openBisSampleType.equals(DEFAULT_ANALYTE_TYPE)) {
      logger("No mapping was found for " + analyteValue + " when updating sample.");
      logger("Using default value and adding " + analyteValue + " to Q_DETAILED_ANALYTE_TYPE.");
      sampleUpdate.setProperty("Q_DETAILED_ANALYTE_TYPE", analyteValue);
    }
    return sampleUpdate;
  }

  private List<SampleUpdate> convertSamplesToSampleUpdates(
      Collection<life.qbic.projectmanagement.domain.model.sample.Sample> updatedSamples) {
    return updatedSamples.stream().map(this::createSampleUpdate).toList();
  }

  record VocabularyTerm(String code, String label, String description) {

  }

  private void createOpenbisProject(String spaceCodeString, String projectCode) {
    ProjectCreation project = new ProjectCreation();
    project.setCode(projectCode);
    project.setSpaceId(new SpacePermId(spaceCodeString));

    IOperation operation = new CreateProjectsOperation(project);
    handleOperations(operation);
  }

  private void createOpenbisExperiment(String spaceCode, String projectCode,
      String experimentCode) {
    ExperimentCreation experiment = new ExperimentCreation();
    experiment.setTypeId(new EntityTypePermId(DEFAULT_EXPERIMENT_TYPE));
    experiment.setProjectId(new ProjectIdentifier(spaceCode, projectCode));
    experiment.setCode(experimentCode);

    IOperation operation = new CreateExperimentsOperation(experiment);
    handleOperations(operation);
  }

  private void deleteOpenbisProject(String spaceCode, String projectCode) {
    ProjectDeletionOptions deletionOptions = new ProjectDeletionOptions();
    deletionOptions.setReason(DEFAULT_DELETION_REASON);
    //OpenBis expects the projectspace and code during deletion
    ProjectIdentifier projectIdentifier = new ProjectIdentifier(spaceCode,
        projectCode);
    List<ProjectIdentifier> openBisProjectsIds = new ArrayList<>();
    openBisProjectsIds.add(projectIdentifier);
    DeleteProjectsOperation operation = new DeleteProjectsOperation(openBisProjectsIds,
        deletionOptions);
    handleOperations(operation);
  }

  private void deleteOpenbisSample(String spaceCode, String sampleCode) {
    SampleDeletionOptions deletionOptions = new SampleDeletionOptions();
    deletionOptions.setReason(DEFAULT_DELETION_REASON);
    //OpenBis expects the projectspace and code during deletion
    SampleIdentifier sampleIdentifier = new SampleIdentifier(spaceCode, null, sampleCode);
    List<SampleIdentifier> openBisSampleIds = new ArrayList<>();
    openBisSampleIds.add(sampleIdentifier);

    // we need to handle this deletion operation differently in order to confirm deletion
    IApplicationServerApi api = openBisClient.getV3();
    IDeletionId deletionId = api.deleteSamples(openBisClient.getSessionToken(), openBisSampleIds,
        deletionOptions);
    api.confirmDeletions(openBisClient.getSessionToken(), Collections.singletonList(deletionId));
  }

  private void deleteOpenbisExperiment(ExperimentIdentifier experimentIdentifier) {
    ExperimentDeletionOptions deletionOptions = new ExperimentDeletionOptions();
    deletionOptions.setReason(DEFAULT_DELETION_REASON);
    List<ExperimentIdentifier> openBisIds = new ArrayList<>();
    openBisIds.add(experimentIdentifier);
    // we need to handle this deletion operation differently in order to confirm deletion
    IApplicationServerApi api = openBisClient.getV3();
    IDeletionId deletionId = api.deleteExperiments(openBisClient.getSessionToken(), openBisIds,
        deletionOptions);
    api.confirmDeletions(openBisClient.getSessionToken(), Collections.singletonList(deletionId));
  }

  private void handleOperations(IOperation operation) {
    IApplicationServerApi api = openBisClient.getV3();
    SynchronousOperationExecutionOptions executionOptions = new SynchronousOperationExecutionOptions();
    List<IOperation> operationOptions = Collections.singletonList(operation);
    try {
      api.executeOperations(openBisClient.getSessionToken(), operationOptions, executionOptions);
    } catch (Exception e) {
      log.error("Unexpected exception during openBIS operation.", e);
      throw e;
    }
  }

  @Override
  public void add(Project project) {
    createOpenbisProject(DEFAULT_SPACE_CODE, project.getProjectCode().value());
  }

  @Override
  public void delete(ProjectCode projectCode) {
    deleteOpenbisProject(DEFAULT_SPACE_CODE, projectCode.value());
  }

  @Override
  public boolean projectExists(ProjectCode projectCode) {
    return !searchProjectsByCode(projectCode.toString()).isEmpty();
  }

  // Convenience RTE to describe connection issues
  static class ConnectionException extends RuntimeException {

  }

  static class MappingNotFoundException extends RuntimeException {

  }

  public static class SampleNotDeletedException extends RuntimeException {

    public SampleNotDeletedException(String s) {
      super(s);
    }
  }

  public static class SampleNotUpdatedException extends RuntimeException {

    public SampleNotUpdatedException(String s) {
      super(s);
    }
  }
}
