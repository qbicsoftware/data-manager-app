package life.qbic.projectmanagement.infrastructure.sample.openbis;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;
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
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SampleIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SampleSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.update.SampleUpdate;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.update.UpdateSamplesOperation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.SpacePermId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.domain.model.measurement.NGSMeasurement;
import life.qbic.projectmanagement.domain.model.measurement.ProteomicsMeasurement;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectCode;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;
import life.qbic.projectmanagement.infrastructure.experiment.measurement.MeasurementDataRepo;
import life.qbic.projectmanagement.infrastructure.project.QbicProjectDataRepo;
import life.qbic.projectmanagement.infrastructure.sample.QbicSampleDataRepo;
import life.qbic.projectmanagement.infrastructure.sample.openbis.OpenbisSessionFactory.ApiV3;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Basic implementation to query project preview information
 *
 * @since 1.0.0
 */
@Component
public class OpenbisConnector implements QbicProjectDataRepo, QbicSampleDataRepo,
    MeasurementDataRepo {

  private static final Logger log = logger(OpenbisConnector.class);

  private static final String DEFAULT_SPACE_CODE = "DATA_MANAGER_SPACE";
  private static final String DEFAULT_SAMPLE_TYPE = "Q_SAMPLE";
  private static final String DEFAULT_EXPERIMENT_TYPE = "Q_SAMPLE_PREPARATION";
  private static final String EXTERNAL_ID_CODE = "Q_EXTERNAL_ID";
  private static final String DEFAULT_DELETION_REASON = "Commanded by data manager app";
  private final OpenbisSessionFactory sessionFactory;
  private final IApplicationServerApi applicationServer;

  // used by spring to wire it up
  private OpenbisConnector(@Value("${openbis.user.name}") String userName,
      @Value("${openbis.user.password}") String password,
      @Value("${openbis.datasource.url}") String url) {

    String OPENBIS_APPLICATION_URL=url + IApplicationServerApi.SERVICE_URL;

    this.sessionFactory =  new OpenbisSessionFactory(OPENBIS_APPLICATION_URL, userName, password);
    this.applicationServer = ApiV3.applicationServer(OPENBIS_APPLICATION_URL);
  }

  private List<Sample> searchSamplesByCodes(Collection<SampleCode> sampleCodes) {
    SampleSearchCriteria criteria = new SampleSearchCriteria();
    criteria.withCodes().thatIn(new ArrayList<>(sampleCodes.stream().map(SampleCode::code).toList()));
    return applicationServer.searchSamples(sessionFactory.getSession().getToken(), criteria,
        fetchSamplesCompletely()).getObjects();
  }

  private List<Experiment> searchExperimentsByProjectCode(String projectCode,
      ExperimentFetchOptions fetchOptions) {
    ExperimentSearchCriteria criteria = new ExperimentSearchCriteria();
    criteria.withProject().withCode().thatEquals(projectCode);

    SearchResult<Experiment> searchResult =
        applicationServer.searchExperiments(sessionFactory.getSession().getToken(), criteria, fetchOptions);

    return searchResult.getObjects();
  }

  private List<ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project> searchProjectsByCode(
      String code) {
    ProjectSearchCriteria criteria = new ProjectSearchCriteria();
    criteria.withCode().thatEquals(code);
    ProjectFetchOptions options = new ProjectFetchOptions();
    SearchResult<ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project> searchResult =
        applicationServer.searchProjects(sessionFactory.getSession().getToken(), criteria, options);

    return searchResult.getObjects();
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

        props.put("Q_LABEL", sample.label());
        props.put(EXTERNAL_ID_CODE, sample.sampleId().value());
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

  /**
   * Performs a list of provided update operations. Creates a mutable list beforehand, as that is
   * necessary
   * @param samplesToUpdate List of SampleUpdate objects containing changes to one or more samples
   */
  private void updateOpenbisSamples(List<SampleUpdate> samplesToUpdate) {
    List<SampleUpdate> mutableUpdates = new ArrayList<>(samplesToUpdate);
    IOperation operation = new UpdateSamplesOperation(mutableUpdates);
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
  public void deleteAll(ProjectCode projectCode, Collection<SampleCode> sampleCodes) {
    List<Sample> samplesToDelete = searchSamplesByCodes(sampleCodes);
      for (Sample sample : samplesToDelete) {
          if (isSampleWithData(List.of(sample))) {
            throw new SampleNotDeletedException(
                "Did not delete sample " + sample.getCode() + ", because data is attached.");
          }
        }
    // no data found, we can safely delete all samples
    sampleCodes.forEach(code -> deleteOpenbisSample(code.code()));
  }

  @Override
  public boolean canDeleteSample(ProjectCode projectCode, SampleCode codeToDelete) {
    List<Sample> samplesToDelete = searchSamplesByCodes(new ArrayList<>(Arrays.asList(codeToDelete)));
    for (Sample sample : samplesToDelete) {
      if (isSampleWithData(List.of(sample))) {
            return false;
          }
        }
    return true;
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
   * @param project
   * @param samples the batch of {@link Sample}s to be updated in the data repo
   * @since 1.0.0
   */
  @Override
  public void updateAll(
      Project project, Collection<life.qbic.projectmanagement.domain.model.sample.Sample> samples)
      throws SampleNotUpdatedException {
    try {
      String projectCode = project.getProjectCode().value();
      updateOpenbisSamples(convertSamplesToSampleUpdates(projectCode, samples));
    } catch (RuntimeException e) {
      throw new SampleNotUpdatedException(
          "Samples could not be updated due to " + e.getCause() + " with " + e.getMessage());
    }
  }

  private SampleUpdate createSampleUpdate(String projectCode,
      life.qbic.projectmanagement.domain.model.sample.Sample sample) {
    SampleUpdate sampleUpdate = new SampleUpdate();
    sampleUpdate.setSampleId(new SampleIdentifier(DEFAULT_SPACE_CODE, projectCode,
        null, sample.sampleCode().code()));
    sampleUpdate.setProperty("Q_LABEL", sample.label());
    sampleUpdate.setProperty(EXTERNAL_ID_CODE, sample.sampleId().value());

    return sampleUpdate;
  }

  private List<SampleUpdate> convertSamplesToSampleUpdates(
      String projectCode, Collection<life.qbic.projectmanagement.domain.model.sample.Sample> updatedSamples) {
    return updatedSamples.stream().map(s -> createSampleUpdate(projectCode, s)).toList();
  }

  private void registerMeasurementSample(String sampleCode, String measurementTypeCode,
      List<SampleIdentifier> parentIds, Map<String,String> metadata) {
    SampleCreation sampleCreation = new SampleCreation();
    sampleCreation.setCode(sampleCode);
    sampleCreation.setParentIds(new ArrayList<>(parentIds));
    sampleCreation.setTypeId(new EntityTypePermId(measurementTypeCode));
    sampleCreation.setSpaceId(new SpacePermId(DEFAULT_SPACE_CODE));
    sampleCreation.setProperties(metadata);
    createOpenbisSamples(Arrays.asList(sampleCreation));
  }

  @Override
  public void addNGSMeasurement(NGSMeasurement measurement, List<SampleCode> parentCodes) {
    String TYPE_CODE = "Q_NGS_MEASUREMENT";
    Map<String, String> metadata = new HashMap<>();
    metadata.put(EXTERNAL_ID_CODE, measurement.measurementId().value());
    List<SampleIdentifier> parentIds = fetchSampleIdentifiers(parentCodes.stream().map(SampleCode::code).toList());
    registerMeasurementSample(measurement.measurementCode().value(), TYPE_CODE, parentIds, metadata);
  }

  @Override
  public void addProtemicsMeasurement(ProteomicsMeasurement measurement, List<SampleCode> parentCodes) {
    String TYPE_CODE = "Q_PROTEOMICS_MEASUREMENT";
    Map<String, String> metadata = new HashMap<>();
    metadata.put(EXTERNAL_ID_CODE, measurement.measurementId().value());
    List<SampleIdentifier> parentIds = fetchSampleIdentifiers(parentCodes.stream().map(SampleCode::code).toList());
    registerMeasurementSample(measurement.measurementCode().value(), TYPE_CODE, parentIds, metadata);
  }

  private List<SampleIdentifier> fetchSampleIdentifiers(List<String> sampleCodes) {
    SampleSearchCriteria criteria = new SampleSearchCriteria();
    criteria.withCodes().thatIn(new ArrayList<>(sampleCodes));
    criteria.withAndOperator();
    criteria.withSpace().withCode().thatEquals(DEFAULT_SPACE_CODE);
    List<Sample> searchResult = applicationServer.searchSamples(
        sessionFactory.getSession().getToken(), criteria,
        new SampleFetchOptions()).getObjects();
    return new ArrayList<>(searchResult.stream().map(Sample::getIdentifier).toList());
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

  private void deleteOpenbisSample(String sampleCode) {
    SampleDeletionOptions deletionOptions = new SampleDeletionOptions();
    deletionOptions.setReason(DEFAULT_DELETION_REASON);
    List<SampleIdentifier> openBisSampleIds = fetchSampleIdentifiers(new ArrayList<>(Arrays.asList(sampleCode)));

    // we need to handle this deletion operation differently in order to confirm deletion
    IDeletionId deletionId = applicationServer.deleteSamples(sessionFactory.getSession().getToken(), openBisSampleIds,
        deletionOptions);
    applicationServer.confirmDeletions(sessionFactory.getSession().getToken(), Collections.singletonList(deletionId));
  }

  private void deleteOpenbisExperiment(ExperimentIdentifier experimentIdentifier) {
    ExperimentDeletionOptions deletionOptions = new ExperimentDeletionOptions();
    deletionOptions.setReason(DEFAULT_DELETION_REASON);
    List<ExperimentIdentifier> openBisIds = new ArrayList<>();
    openBisIds.add(experimentIdentifier);
    // we need to handle this deletion operation differently in order to confirm deletion
    IDeletionId deletionId = applicationServer.deleteExperiments(sessionFactory.getSession().getToken(), openBisIds,
        deletionOptions);
    applicationServer.confirmDeletions(sessionFactory.getSession().getToken(), Collections.singletonList(deletionId));
  }

  private void handleOperations(IOperation operation) {
    SynchronousOperationExecutionOptions executionOptions = new SynchronousOperationExecutionOptions();
    List<IOperation> operationOptions = Collections.singletonList(operation);
    try {
      applicationServer.executeOperations(sessionFactory.getSession().getToken(), operationOptions, executionOptions);
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
    return !searchProjectsByCode(projectCode.value()).isEmpty();
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

  public static SampleFetchOptions fetchSamplesCompletely() {
    SampleFetchOptions sampleFetchOptions = new SampleFetchOptions();
    sampleFetchOptions.withExperiment();
    sampleFetchOptions.withAttachments();
    sampleFetchOptions.withComponents();
    sampleFetchOptions.withContainer();
    sampleFetchOptions.withDataSets();
    sampleFetchOptions.withHistory();
    sampleFetchOptions.withMaterialProperties();
    sampleFetchOptions.withModifier();
    sampleFetchOptions.withProperties();
    sampleFetchOptions.withRegistrator();
    sampleFetchOptions.withSpace();
    sampleFetchOptions.withTags();
    sampleFetchOptions.withType();
    sampleFetchOptions.withParentsUsing(sampleFetchOptions);
    sampleFetchOptions.withChildrenUsing(sampleFetchOptions);

    return sampleFetchOptions;
  }
}
