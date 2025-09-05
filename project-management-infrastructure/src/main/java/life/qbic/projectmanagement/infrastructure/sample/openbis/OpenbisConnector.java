package life.qbic.projectmanagement.infrastructure.sample.openbis;

import static life.qbic.logging.service.LoggerFactory.logger;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.operation.IOperation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.search.DataSetSearchCriteria;
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
import ch.ethz.sis.openbis.generic.dssapi.v3.IDataStoreServerApi;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.DataSetFile;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.fetchoptions.DataSetFileFetchOptions;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.search.DataSetFileSearchCriteria;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.SortOrder;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.DataRepoConnectionTester;
import life.qbic.projectmanagement.application.dataset.RemoteRawDataLookup;
import life.qbic.projectmanagement.application.dataset.RemoteRawDataService.RawData;
import life.qbic.projectmanagement.application.dataset.RemoteRawDataService.RawDataDatasetInformation;
import life.qbic.projectmanagement.application.sample.SampleIdCodeEntry;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementCode;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementId;
import life.qbic.projectmanagement.domain.model.measurement.NGSMeasurement;
import life.qbic.projectmanagement.domain.model.measurement.ProteomicsMeasurement;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectCode;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;
import life.qbic.projectmanagement.infrastructure.experiment.measurement.MeasurementDataRepo;
import life.qbic.projectmanagement.infrastructure.project.QbicProjectDataRepo;
import life.qbic.projectmanagement.infrastructure.sample.SampleDataRepository;
import life.qbic.projectmanagement.infrastructure.sample.openbis.OpenbisSession.ApiV3;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Basic implementation to query project information
 *
 * @since 1.0.0
 */
@Component
public class OpenbisConnector implements QbicProjectDataRepo, SampleDataRepository,
    MeasurementDataRepo, RemoteRawDataLookup, DataRepoConnectionTester, DisposableBean {

  private static final Instant DATE_MIN = Instant.EPOCH; // lower bound for date conversion

  private static final Logger log = logger(OpenbisConnector.class);

  private static final String DEFAULT_SPACE_CODE = "DATA_MANAGER_SPACE";
  private static final String DEFAULT_SAMPLE_TYPE = "Q_SAMPLE";
  private static final String DEFAULT_EXPERIMENT_TYPE = "Q_SAMPLE_BATCH";
  private static final String EXTERNAL_ID_CODE = "Q_EXTERNAL_ID";
  private static final String DEFAULT_DELETION_REASON = "Commanded by data manager app";
  private static final String NGS_MEASUREMENT_TYPE_CODE = "Q_NGS_MEASUREMENT";
  private static final String PROTEOMICS_MEASUREMENT_TYPE_CODE = "Q_PROTEOMICS_MEASUREMENT";
  public static final Random RANDOM = new Random();
  private final IApplicationServerApi applicationServer;
  private final IDataStoreServerApi datastoreServer;
  private final OpenbisSession openBisSession;
  private static final int RETRY_COUNT_MAX = 10;

  // used by spring to wire it up
  private OpenbisConnector(
      @Value("${openbis.datasource.as.url}") String asUrl,
      @Value("${openbis.datasource.dss.url}") String dssUrl,
      @Autowired VaultConfig vaultConfig) {

    final String openbisApplicationUrl = asUrl + IApplicationServerApi.SERVICE_URL;
    final String openbisDssUrl = dssUrl + IDataStoreServerApi.SERVICE_URL;

    this.openBisSession = new OpenbisSession(vaultConfig.vault(), openbisApplicationUrl);
    this.applicationServer = ApiV3.applicationServer(openbisApplicationUrl);
    this.datastoreServer = ApiV3.dataStoreServer(openbisDssUrl);
  }

  public static String complementOfTwoToHex(int complementOfTwo) {
    long unsigned = Integer.toUnsignedLong(complementOfTwo);
    return Long.toHexString(unsigned);
  }

  public static String getStringSizeLengthFile(long size) {
    DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols();
    otherSymbols.setDecimalSeparator('.');
    DecimalFormat df = new DecimalFormat("0.00", otherSymbols);

    float sizeKb = 1024.0f;
    float sizeMb = sizeKb * sizeKb;
    float sizeGb = sizeMb * sizeKb;
    float sizeTerra = sizeGb * sizeKb;

    if (size < sizeMb) {
      return df.format(size / sizeKb) + " KB";
    } else if (size < sizeGb) {
      return df.format(size / sizeMb) + " MB";
    } else if (size < sizeTerra) {
      return df.format(size / sizeGb) + " GB";
    }

    return "";
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

  private List<Sample> searchSamplesByCodes(Collection<String> sampleCodes) {
    SampleSearchCriteria criteria = new SampleSearchCriteria();
    criteria.withCodes().thatIn(new ArrayList<>(sampleCodes));
    return applicationServer.searchSamples(this.openBisSession.getToken(), criteria,
        fetchSamplesCompletely()).getObjects();
  }

  private List<Experiment> searchExperimentsByProjectCode(String projectCode,
      ExperimentFetchOptions fetchOptions) {
    ExperimentSearchCriteria criteria = new ExperimentSearchCriteria();
    criteria.withProject().withCode().thatEquals(projectCode);

    SearchResult<Experiment> searchResult =
        applicationServer.searchExperiments(openBisSession.getToken(), criteria, fetchOptions);
    return searchResult.getObjects();
  }

  private List<ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project> searchProjectsByCode(
      String code) {
    ProjectSearchCriteria criteria = new ProjectSearchCriteria();
    criteria.withCode().thatEquals(code);
    ProjectFetchOptions options = new ProjectFetchOptions();
    SearchResult<ch.ethz.sis.openbis.generic.asapi.v3.dto.project.Project> searchResult =
        applicationServer.searchProjects(openBisSession.getToken(), criteria, options);
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
      createOpenbisSamples(this.openBisSession, samplesToRegister);
    } catch (Exception e) {
      deleteOpenbisExperiment(this.openBisSession, newExperimentID);
      throw e;
    }
  }

  private String findFreeExperimentCode(String projectCode) {
    List<Experiment> experiments = searchExperimentsByProjectCode(projectCode,
        new ExperimentFetchOptions());
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
    Pattern lastIntPattern = Pattern.compile("\\D+(\\d+)$");
    Matcher matcher = lastIntPattern.matcher(input);
    if (matcher.find()) {
      String someNumberStr = matcher.group(1);
      lastNumberInt = Integer.parseInt(someNumberStr);
    }
    return lastNumberInt;
  }

  private void createOpenbisSamples(OpenbisSession session,
      List<SampleCreation> samplesToRegister) {
    IOperation operation = new CreateSamplesOperation(samplesToRegister);
    handleOperations(session, operation);
  }

  /**
   * Performs a list of provided update operations. Creates a mutable list beforehand, as that is
   * necessary
   *
   * @param samplesToUpdate List of SampleUpdate objects containing changes to one or more samples
   */
  private void updateOpenbisSamples(List<SampleUpdate> samplesToUpdate) {
    List<SampleUpdate> mutableUpdates = new ArrayList<>(samplesToUpdate);
    IOperation operation = new UpdateSamplesOperation(mutableUpdates);
    handleOperations(openBisSession, operation);
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
    List<Sample> samplesToDelete = searchSamplesByCodes(sampleCodes.stream()
        .map(SampleCode::code).toList());
    for (Sample sample : samplesToDelete) {
      if (isSampleWithData(List.of(sample))) {
        throw new SampleNotDeletedException(
            "Did not delete sample " + sample.getCode() + ", because data is attached.");
      }
    }
    // no data found, we can safely delete all samples
    sampleCodes.forEach(code -> deleteOpenbisSample(openBisSession, code.code()));
  }

  @Override
  public boolean canDeleteSample(SampleCode codeToDelete) {
    return canDeleteSampleObject(codeToDelete.code());
  }

  private boolean canDeleteSampleObject(String code) {
    List<Sample> samplesToDelete = searchSamplesByCodes(List.of(code));
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
      String projectCode,
      Collection<life.qbic.projectmanagement.domain.model.sample.Sample> updatedSamples) {
    return updatedSamples.stream().map(s -> createSampleUpdate(projectCode, s)).toList();
  }

  @Override
  public void addNGSMeasurement(NGSMeasurement measurement, List<SampleCode> parentCodes) {
    createMeasurementInOpenbis(parentCodes, NGS_MEASUREMENT_TYPE_CODE, measurement.measurementId(),
        measurement.measurementCode());
  }

  @Override
  public void saveAllNGS(
      Map<NGSMeasurement, Collection<SampleIdCodeEntry>> ngsMeasurementsMapping) {
    var objectsToCreate = ngsMeasurementsMapping
        .entrySet().stream()
        .map(
            entry -> {
              NGSMeasurement measurement = entry.getKey();
              Collection<SampleIdCodeEntry> sampleIdCodes = entry.getValue();

              List<String> parentCodes = sampleIdCodes.stream()
                  .map(sampleEntry -> sampleEntry.sampleCode().code())
                  .toList();
              return prepareSampleCreation(NGS_MEASUREMENT_TYPE_CODE, openBisSession, parentCodes,
                  measurement.measurementId(),
                  measurement.measurementCode());
            }
        ).collect(Collectors.toCollection(ArrayList::new));
    createOpenbisSamples(openBisSession, objectsToCreate);
  }

  @Override
  public void addProteomicsMeasurement(ProteomicsMeasurement measurement,
      List<SampleCode> parentCodes) {
    createMeasurementInOpenbis(parentCodes, PROTEOMICS_MEASUREMENT_TYPE_CODE,
        measurement.measurementId(),
        measurement.measurementCode());
  }

  private void createMeasurementInOpenbis(List<SampleCode> parentCodes, String typeCode,
      MeasurementId measurementId, MeasurementCode measurementCode) {
    Map<String, String> metadata = new HashMap<>();
    metadata.put(EXTERNAL_ID_CODE, measurementId.value());
    List<SampleIdentifier> parentIds = fetchSampleIdentifiers(openBisSession,
        parentCodes.stream().map(SampleCode::code).toList());
    String sampleCode = measurementCode.value();
    SampleCreation sampleCreation = new SampleCreation();
    sampleCreation.setCode(sampleCode);
    sampleCreation.setParentIds(new ArrayList<>(parentIds));
    sampleCreation.setTypeId(new EntityTypePermId(typeCode));
    sampleCreation.setSpaceId(new SpacePermId(DEFAULT_SPACE_CODE));
    sampleCreation.setProperties(metadata);
    createOpenbisSamples(openBisSession, List.of(
        sampleCreation));
  }

  @Override
  public void saveAllProteomics(
      Map<ProteomicsMeasurement, Collection<SampleIdCodeEntry>> proteomicsMeasurementsMapping) {

    List<SampleCreation> objectsToCreate = proteomicsMeasurementsMapping
        .entrySet().stream()
        .map(
            entry -> {
              ProteomicsMeasurement measurement = entry.getKey();
              Collection<SampleIdCodeEntry> sampleIdCodes = entry.getValue();

              List<String> parentCodes = sampleIdCodes.stream()
                  .map(sampleEntry -> sampleEntry.sampleCode().code())
                  .toList();
              return prepareSampleCreation(PROTEOMICS_MEASUREMENT_TYPE_CODE, openBisSession,
                  parentCodes,
                  measurement.measurementId(),
                  measurement.measurementCode());
            }
        ).collect(Collectors.toCollection(ArrayList::new));
    createOpenbisSamples(openBisSession, objectsToCreate);
  }

  private SampleCreation prepareSampleCreation(String typeCode, OpenbisSession session,
      List<String> parentCodes, MeasurementId measurementId, MeasurementCode measurementCode) {
    List<SampleIdentifier> parentIds = fetchSampleIdentifiers(session, parentCodes);

    Map<String, String> metadata = new HashMap<>();
    metadata.put(EXTERNAL_ID_CODE, measurementId.value());

    String sampleCode = measurementCode.value();
    SampleCreation sampleCreation = new SampleCreation();
    sampleCreation.setCode(sampleCode);
    sampleCreation.setParentIds(new ArrayList<>(parentIds));
    sampleCreation.setTypeId(new EntityTypePermId(typeCode));
    sampleCreation.setSpaceId(new SpacePermId(DEFAULT_SPACE_CODE));
    sampleCreation.setProperties(metadata);
    return sampleCreation;
  }

  @Override
  public void deleteProteomicsMeasurements(List<ProteomicsMeasurement> measurements) {
    deleteMeasurements(measurements.stream().map(ProteomicsMeasurement::measurementCode).toList());
  }

  @Override
  public void deleteNGSMeasurements(List<NGSMeasurement> measurements) {
    deleteMeasurements(measurements.stream().map(NGSMeasurement::measurementCode).toList());
  }

  private void deleteMeasurements(List<MeasurementCode> measurementCodes) {
    for (MeasurementCode code : measurementCodes) {
      String sampleCode = code.value();
      // measurement has been deleted in JPA at this moment. We don't fail, but we keep data in
      // openbis that might have been registered between the check and deletion
      if (canDeleteSampleObject(sampleCode)) {
        deleteOpenbisSample(openBisSession, sampleCode);
      }
    }
  }

  @Override
  public boolean hasDataAttached(List<MeasurementCode> measurements) {
    for (MeasurementCode code : measurements) {
      List<Sample> samplesToDelete = searchSamplesByCodes(
          new ArrayList<>(Arrays.asList(code.value())));
      for (Sample sample : samplesToDelete) {
        if (isSampleWithData(List.of(sample))) {
          return true;
        }
      }
    }
    return false;
  }

  private List<SampleIdentifier> fetchSampleIdentifiers(OpenbisSession session,
      List<String> sampleCodes) {
    SampleSearchCriteria criteria = new SampleSearchCriteria();
    criteria.withCodes().thatIn(new ArrayList<>(sampleCodes));
    criteria.withAndOperator();
    criteria.withSpace().withCode().thatEquals(DEFAULT_SPACE_CODE);
    List<Sample> searchResult = applicationServer.searchSamples(
        session.getToken(), criteria,
        new SampleFetchOptions()).getObjects();
    return new ArrayList<>(searchResult.stream().map(Sample::getIdentifier).toList());
  }

  private void createOpenbisProject(String spaceCodeString, String projectCode) {
    ProjectCreation project = new ProjectCreation();
    project.setCode(projectCode);
    project.setSpaceId(new SpacePermId(spaceCodeString));

    IOperation operation = new CreateProjectsOperation(project);
    handleOperations(openBisSession, operation);
  }

  private void createOpenbisExperiment(String spaceCode, String projectCode,
      String experimentCode) {
    ExperimentCreation experiment = new ExperimentCreation();
    experiment.setTypeId(new EntityTypePermId(DEFAULT_EXPERIMENT_TYPE));
    experiment.setProjectId(new ProjectIdentifier(spaceCode, projectCode));
    experiment.setCode(experimentCode);

    IOperation operation = new CreateExperimentsOperation(experiment);

    handleOperations(openBisSession, operation);
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
    handleOperations(openBisSession, operation);
  }

  private void deleteOpenbisSample(OpenbisSession session, String sampleCode) {
    SampleDeletionOptions deletionOptions = new SampleDeletionOptions();
    deletionOptions.setReason(DEFAULT_DELETION_REASON);
    List<SampleIdentifier> openBisSampleIds = fetchSampleIdentifiers(session,
        new ArrayList<>(Arrays.asList(sampleCode)));

    // we need to handle this deletion operation differently in order to confirm deletion
    IDeletionId deletionId = applicationServer.deleteSamples(session.getToken(),
        openBisSampleIds,
        deletionOptions);
    applicationServer.confirmDeletions(session.getToken(),
        Collections.singletonList(deletionId));
  }

  private void deleteOpenbisExperiment(OpenbisSession session,
      ExperimentIdentifier experimentIdentifier) {
    ExperimentDeletionOptions deletionOptions = new ExperimentDeletionOptions();
    deletionOptions.setReason(DEFAULT_DELETION_REASON);
    List<ExperimentIdentifier> openBisIds = new ArrayList<>();
    openBisIds.add(experimentIdentifier);
    // we need to handle this deletion operation differently in order to confirm deletion
    IDeletionId deletionId = applicationServer.deleteExperiments(session.getToken(), openBisIds,
        deletionOptions);
    applicationServer.confirmDeletions(session.getToken(), Collections.singletonList(deletionId));
  }

  /**
   * Queries {@link RawData} with a provided offset and limit that supports pagination.
   *
   * @param filter           the results fields will be checked for the value within this filter
   * @param measurementCodes the list of {@link MeasurementCode}s for which the raw Data should be
   *                         fetched
   * @param offset           the offset for the search result to start
   * @param limit            the maximum number of results that should be returned
   * @param sortOrders       the ordering to sort by
   * @return the results in the provided range
   */
  @Override
  public List<RawDataDatasetInformation> queryRawDataByMeasurementCodes(String filter,
      Collection<MeasurementCode> measurementCodes, int offset, int limit,
      List<SortOrder> sortOrders) {
    DataSetSearchCriteria searchCriteria = new DataSetSearchCriteria();
    List<String> codes = new ArrayList<>(measurementCodes.stream().map(MeasurementCode::value)
        .toList());
    searchCriteria.withSample().withCodes().thatIn(codes);
    DataSetFetchOptions fetchOptions = new DataSetFetchOptions();
    fetchOptions.from(offset);
    fetchOptions.count(limit);
    fetchOptions.withSample();
    return getRawDataDatasetInformation(filter, searchCriteria, fetchOptions);
  }

  private List<RawDataDatasetInformation> getRawDataDatasetInformation(
      String filter,
      DataSetSearchCriteria searchCriteria,
      DataSetFetchOptions fetchOptions) {
    List<RawDataDatasetInformation> result = new ArrayList<>();

    // Ensures to query only in the assigned openBIS space
    searchCriteria.withSample().withSpace().withPermId().thatEquals(DEFAULT_SPACE_CODE);

    if (!filter.isBlank()) {
      searchCriteria.withAndOperator();
      DataSetSearchCriteria filterCriteria = searchCriteria.withSubcriteria().withOrOperator();
      filterCriteria.withSample().withCode().thatContains(filter);
    }

    List<DataSet> searchResult = applicationServer.searchDataSets(
        openBisSession.getToken(), searchCriteria,
        fetchOptions).getObjects();
    Map<String, List<DataSetFile>> fileInfos = fetchFileInformationForDatasets(openBisSession,
        searchResult.stream().map(DataSet::getCode).toList());
    for (DataSet dataset : searchResult) {
      String datasetCode = dataset.getCode();
      List<DataSetFile> dsFileInfos = fileInfos.get(datasetCode);
      Date registrationDate = dataset.getRegistrationDate();
      Set<String> suffixes = new HashSet<>();
      long dataSetSize = 0;
      int numOfFiles = 0;
      for (DataSetFile file : dsFileInfos) {
        if (!file.isDirectory()) {
          numOfFiles++;
          String path = file.getPath();
          if (path.contains(".")) {
            suffixes.add(path.substring(path.indexOf(".") + 1));
          }
          dataSetSize += file.getFileLength();
        }
      }
      result.add(
          new RawDataDatasetInformation(MeasurementCode.parse(dataset.getSample().getCode()),
              getStringSizeLengthFile(dataSetSize), numOfFiles, suffixes, registrationDate,
              dataSetSize));
    }

    return result;
  }

  @Override
  public int countRawDataByMeasurementIds(Collection<MeasurementCode> measurementCodes) {
    DataSetFetchOptions fetchOptions = new DataSetFetchOptions();
    DataSetSearchCriteria searchCriteria = new DataSetSearchCriteria();
    List<String> codes = new ArrayList<>(
        measurementCodes.stream().map(MeasurementCode::value).toList());
    searchCriteria.withSample().withCodes().thatIn(codes);
    return applicationServer.searchDataSets(
        openBisSession.getToken(), searchCriteria,
        fetchOptions).getObjects().size();
  }

  private static Instant instantWithinBoundaries(Instant instant) {
    if (instant.isBefore(DATE_MIN)) {
      return DATE_MIN;
    }
    if (instant.isAfter(Instant.now())) {
      return Instant.now();
    }
    return instant;
  }

  @Override
  public List<RawDataDatasetInformation> queryRawDataSince(Instant registeredSince, int offset,
      int limit) {
    var datasetSearchCriteria = new DataSetSearchCriteria();
    datasetSearchCriteria.withRegistrationDate()
        .thatIsLaterThanOrEqualTo(Date.from(instantWithinBoundaries(registeredSince)));

    var datasetFetchOptions = new DataSetFetchOptions();
    datasetFetchOptions.from(offset);
    datasetFetchOptions.count(limit);
    datasetFetchOptions.withSample();

    return getRawDataDatasetInformation("", datasetSearchCriteria, datasetFetchOptions);
  }

  private Map<String, List<DataSetFile>> fetchFileInformationForDatasets(OpenbisSession session,
      List<String> datasetCodes) {

    Map<String, List<DataSetFile>> result = new HashMap<>();
    for (String code : datasetCodes) {
      result.put(code, new ArrayList<>());
    }

    DataSetFileSearchCriteria criteria = new DataSetFileSearchCriteria();

    DataSetSearchCriteria dataSetCriteria = criteria.withDataSet().withOrOperator();
    dataSetCriteria.withCodes().thatIn(new ArrayList<>(datasetCodes));

    SearchResult<DataSetFile> searchResult = datastoreServer.searchFiles(session.getToken(),
        criteria,
        new DataSetFileFetchOptions());

    for (DataSetFile file : searchResult.getObjects()) {
      result.get(file.getDataSetPermId().getPermId()).add(file);
    }
    return result;
  }

  private void handleOperations(OpenbisSession session, IOperation operation) {
    List<IOperation> operationOptions = Collections.singletonList(operation);
    SynchronousOperationExecutionOptions options = new SynchronousOperationExecutionOptions();
    var round = 1;
    while (round <= RETRY_COUNT_MAX) {
      log.debug("Try operation in openBIS: " + round + " of " + RETRY_COUNT_MAX);
      try {
        applicationServer.executeOperations(session.getToken(), operationOptions, options);
        log.debug("Operations executed successfully");
        break;
      } catch (Exception e) {
        log.error("Unexpected exception during openBIS operation.", e);
      }
      if (round == RETRY_COUNT_MAX) {
        throw new ApplicationException("Unexpected exception during openBIS operation.");
      }
      try {
        Thread.sleep((long) ((RANDOM.nextInt(200) + 500) * Math.pow(2, round)));
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      round++;
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

  @Override
  public void testDatastoreServer() {
    int major = datastoreServer.getMajorVersion();
    int minor = datastoreServer.getMinorVersion();
    log.info(
        "Successfully tested connection to openBIS datastore server version: %d.%d".formatted(major,
            minor));
  }

  @Override
  public void testApplicationServer() {
    if (!applicationServer.isSessionActive(openBisSession.getToken())) {
      throw new ConnectionException("Could not connect to openBIS application server.");
    }
    log.info("Successfully tested connection to openBIS application server.");
  }

  @Override
  public void destroy() throws Exception {
    openBisSession.logout();
  }

  // Convenience RTE to describe connection issues
  static class ConnectionException extends RuntimeException {

    public ConnectionException(String message) {
      super(message);
    }
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
