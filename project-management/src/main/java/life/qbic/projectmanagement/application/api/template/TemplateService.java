package life.qbic.projectmanagement.application.api.template;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.api.AsyncProjectService.UnsupportedMimeTypeException;
import life.qbic.projectmanagement.application.api.fair.DigitalObject;
import life.qbic.projectmanagement.application.api.template.TemplateProvider.MeasurementInformationCollectionNGS;
import life.qbic.projectmanagement.application.api.template.TemplateProvider.MeasurementInformationCollectionPxP;
import life.qbic.projectmanagement.application.api.template.TemplateProvider.MeasurementInformationNGS;
import life.qbic.projectmanagement.application.api.template.TemplateProvider.MeasurementInformationPxP;
import life.qbic.projectmanagement.application.api.template.TemplateProvider.MeasurementSpecificNGS;
import life.qbic.projectmanagement.application.api.template.TemplateProvider.MeasurementSpecificPxP;
import life.qbic.projectmanagement.application.api.template.TemplateProvider.SampleInformation;
import life.qbic.projectmanagement.application.api.template.TemplateProvider.SampleRegistration;
import life.qbic.projectmanagement.application.api.template.TemplateProvider.SampleUpdate;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.ConfoundingVariableInformation;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.ConfoundingVariableLevel;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.ExperimentReference;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService;
import life.qbic.projectmanagement.application.measurement.MeasurementService;
import life.qbic.projectmanagement.application.sample.PropertyConversion;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.model.measurement.NGSIndex;
import life.qbic.projectmanagement.domain.model.measurement.NGSMeasurement;
import life.qbic.projectmanagement.domain.model.measurement.NGSSpecificMeasurementMetadata;
import life.qbic.projectmanagement.domain.model.measurement.ProteomicsMeasurement;
import life.qbic.projectmanagement.domain.model.measurement.ProteomicsSpecificMeasurementMetadata;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.sample.AnalysisMethod;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import life.qbic.projectmanagement.domain.model.sample.SampleId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;

/**
 * Template service.
 * <p>
 * The interface to generate templates of various types and use cases.
 *
 * @since 1.10.0
 */
@Service
public class TemplateService {

  private static final Logger log = logger(TemplateService.class);
  public static final String UNSUPPORTED_MIME_TYPE = "Unsupported mime type: ";
  private final ExperimentInformationService experimentService;
  private final SampleInformationService sampleService;
  private final ConfoundingVariableService confVariableService;
  private final TemplateProvider templateProvider;
  private final MeasurementService measurementService;

  @Autowired
  public TemplateService(
      ExperimentInformationService experimentService,
      SampleInformationService sampleService,
      ConfoundingVariableService confVariableService,
      TemplateProvider templateProvider,
      MeasurementService measurementService) {
    this.experimentService = Objects.requireNonNull(experimentService);
    this.sampleService = Objects.requireNonNull(sampleService);
    this.confVariableService = confVariableService;
    this.templateProvider = Objects.requireNonNull(templateProvider);
    this.measurementService = Objects.requireNonNull(measurementService);
  }

  private static Predicate<Sample> isInBatch(String targetBatchId) {
    return sample -> sample.assignedBatch().value().equals(targetBatchId);
  }

  /**
   * Creates a {@link DigitalObject} that contains a template in the provided {@link MimeType} that
   * can be used to register one or more sample batches for an experiment.
   * <p>
   * The workbook contains two sheets. The first one is for the user input and contains cell with
   * data-validation, e.g. a list of available conditions in the experiment.
   * <p>
   * In total, the template provides data validation for the properties:
   *
   * <ul>
   *   <li>Species</li>
   *   <li>Specimen</li>
   *   <li>Analyte</li>
   *   <li>Condition</li>
   *   <li>Analysis to perform</li>
   * </ul>
   *
   * @param projectId    the project id of the project that contains the experiment for which the
   *                     template shall be generated
   * @param experimentId the experiment id of the experiment to create the template for
   * @return a pre-configured template workbook
   * @throws NoSuchExperimentException    if no experiment with the provided id can be found.
   * @throws UnsupportedMimeTypeException if there is no support for the requested {@link MimeType}
   * @since 1.5.0
   */
  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ') ")
  public DigitalObject sampleRegistrationTemplate(String projectId, String experimentId,
      MimeType type) {
    if (!isSupportedMimeType(type)) {
      throw new UnsupportedMimeTypeException(UNSUPPORTED_MIME_TYPE + type);
    }
    return generateSampleRegistrationTemplate(experimentSupplier(projectId, experimentId),
        projectId, experimentId);
  }

  private DigitalObject generateSampleRegistrationTemplate(Supplier<Experiment> experimentSupplier,
      String projectId, String experimentId) {
    var experiment = experimentSupplier.get();
    var sampleBasic = querySampleBasicInfo(experiment, projectId, experimentId);
    return templateProvider.getTemplate(new SampleRegistration(
        sampleBasic.analysisMethods(),
        sampleBasic.conditions(),
        sampleBasic.analytes(),
        sampleBasic.species(),
        sampleBasic.specimen(),
        sampleBasic.confoundingVariables()
    ));
  }

  /**
   * Creates a {@link DigitalObject} that contains a template in the provided {@link MimeType} that
   * can be used to update one or more sample batches for an experiment.
   * <p>
   * The workbook contains two sheets. The first one is for the user input and contains cell with
   * data-validation, e.g. a list of available conditions in the experiment.
   * <p>
   * In total, the template provides data validation for the properties:
   *
   * <ul>
   *   <li>Species</li>
   *   <li>Specimen</li>
   *   <li>Analyte</li>
   *   <li>Condition</li>
   *   <li>Analysis to perform</li>
   * </ul>
   *
   * @param projectId    the project id of the project that contains the experiment for which the
   *                     template shall be generated
   * @param experimentId the experiment id of the experiment to create the template for
   * @return a pre-configured template workbook
   * @throws NoSuchExperimentException    if no experiment with the provided id can be found.
   * @throws UnsupportedMimeTypeException if there is no support for the requested {@link MimeType}
   * @since 1.5.0
   */
  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ') ")
  public DigitalObject sampleUpdateTemplate(String projectId, String experimentId, String batchId,
      MimeType type) {
    if (!isSupportedMimeType(type)) {
      throw new UnsupportedMimeTypeException(UNSUPPORTED_MIME_TYPE + type);
    }
    return generateSampleUpdateTemplate(
        experimentSupplier(projectId, experimentId),
        projectId,
        experimentId,
        batchId);
  }

  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ') ")
  public DigitalObject measurementUpdateTemplatePxP(String projectId, List<String> measurementIds, MimeType type) {
    if (!isSupportedMimeType(type)) {
      throw new UnsupportedMimeTypeException(UNSUPPORTED_MIME_TYPE + type);
    }
    return generateMeasurementUpdateTemplatePxP(projectId, measurementIds);
  }

  private DigitalObject generateMeasurementUpdateTemplatePxP(String projectId,
      List<String> measurementIds) {
    var measurements = new ArrayList<MeasurementInformationPxP>();
    for (String measurementId : measurementIds) {
      var measurementQuery = measurementService.findProteomicsMeasurementById(projectId, measurementId);
      if (measurementQuery.isEmpty()) {
        throw new MeasurementNotFound("Cannot find measurement with id: " + measurementId);
      }
      var sampleIdsForSpecificMetadata = measurementQuery.get().specificMetadata().stream()
          .map(ProteomicsSpecificMeasurementMetadata::measuredSample)
          .map(SampleId::value)
          .toList();
      var measuredSamples = fetchSamplesForIds(projectId, sampleIdsForSpecificMetadata);
      var convertedEntry = convertFromDomain(measurementQuery.get(), measuredSamples);
      measurements.add(convertedEntry);
    }
    return templateProvider.getTemplate(new MeasurementInformationCollectionPxP(measurements));
  }

  private MeasurementInformationPxP convertFromDomain(ProteomicsMeasurement measurement,
      List<Sample> measuredSamples) {
    // Create a lookup table for faster sample name queries
    var sampleNameById = measuredSamples.stream()
        .collect(Collectors.toMap(s -> s.sampleId().value(), Function.identity()));
    return new MeasurementInformationPxP(
        measurement.measurementCode().value(),
        measurement.technicalReplicateName().orElse(""),
        measurement.organisation().IRI(),
        measurement.organisation().label(),
        measurement.msDevice().oboId().toString(),
        measurement.msDevice().getLabel(),
        measurement.samplePoolGroup().orElse(""),
        measurement.facility(),
        measurement.digestionEnzyme(),
        measurement.digestionMethod(),
        measurement.enrichmentMethod(),
        String.valueOf(measurement.injectionVolume()),
        measurement.lcColumn(),
        measurement.lcmsMethod(),
        measurement.labelType(),
        convertSpecificMetadataPxP(measurement.specificMetadata().stream().toList(),
            sampleNameById),
        measurement.measurementName()
    );
  }


  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ') ")
  public DigitalObject measurementUpdateTemplateNGS(String projectId,
      List<String> measurementIds, MimeType type) {
    if (!isSupportedMimeType(type)) {
      throw new UnsupportedMimeTypeException(UNSUPPORTED_MIME_TYPE + type);
    }
    return generateMeasurementUpdateTemplateNGS(projectId, measurementIds);
  }

  private DigitalObject generateMeasurementUpdateTemplateNGS(String projectId,
      List<String> measurementIds) {
    var measurements = new ArrayList<MeasurementInformationNGS>();
    for (String measurementId : measurementIds) {
      var measurementQuery = measurementService.findNGSMeasurementById(projectId, measurementId);
      if (measurementQuery.isEmpty()) {
        throw new MeasurementNotFound("Cannot find measurement with id: " + measurementId);
      }
      var sampleIdsForSpecificMetadata = measurementQuery.get().specificMeasurementMetadata().stream()
          .map(NGSSpecificMeasurementMetadata::measuredSample)
          .map(SampleId::value)
          .toList();
      var measuredSamples = fetchSamplesForIds(projectId, sampleIdsForSpecificMetadata);
      var convertedEntry = convertFromDomain(measurementQuery.get(), measuredSamples);
      measurements.add(convertedEntry);
    }
    return templateProvider.getTemplate(new MeasurementInformationCollectionNGS(measurements));
  }

  private MeasurementInformationNGS convertFromDomain(
      NGSMeasurement measurement, List<Sample> measuredSamples) {
    // Create a lookup table for faster sample name queries
    var sampleNameById = measuredSamples.stream()
        .collect(Collectors.toMap(s -> s.sampleId().value(), Function.identity()));
    return new MeasurementInformationNGS(
        measurement.measurementCode().value(),
        measurement.organisation().IRI(),
        measurement.organisation().label(),
        measurement.instrument().oboId().toString(),
        measurement.instrument().getLabel(),
        measurement.facility(),
        measurement.sequencingReadType(),
        measurement.libraryKit().orElse(""),
        measurement.flowCell().orElse(""),
        measurement.sequencingRunProtocol().orElse(""),
        measurement.samplePoolGroup().orElse(""),
        convertSpecificMetadataNGS(measurement.specificMeasurementMetadata().stream().toList(),
            sampleNameById),
        measurement.measurementName()
    );
  }

  private Map<String, MeasurementSpecificPxP> convertSpecificMetadataPxP(
      List<ProteomicsSpecificMeasurementMetadata> metadata, Map<String, Sample> sampleNameById) {
    var convertedMap = new HashMap<String, MeasurementSpecificPxP>();
    for (var specificMetadata : metadata) {
      convertedMap.put(specificMetadata.measuredSample().value(), new MeasurementSpecificPxP(
          sampleNameById.get(specificMetadata.measuredSample().value()).sampleCode().code(),
          sampleNameById.get(specificMetadata.measuredSample().value()).label(),
          specificMetadata.label(),
          specificMetadata.fractionName(),
          specificMetadata.comment().orElse("")));
    }
    return convertedMap;
  }

  private Map<String, MeasurementSpecificNGS> convertSpecificMetadataNGS(
      List<NGSSpecificMeasurementMetadata> metadata, Map<String, Sample> sampleNameById) {
    var convertedMap = new HashMap<String, MeasurementSpecificNGS>();
    for (var specificMetadata : metadata) {
      convertedMap.put(specificMetadata.measuredSample().value(), new MeasurementSpecificNGS(
          sampleNameById.get(specificMetadata.measuredSample().value()).sampleCode().code(),
          sampleNameById.get(specificMetadata.measuredSample().value()).label(),
          specificMetadata.index().map(
              NGSIndex::indexI7).orElse(""),
          specificMetadata.index().map(NGSIndex::indexI5).orElse(""),
          specificMetadata.comment().orElse("")));
    }
    return convertedMap;
  }

  private List<Sample> fetchSamplesForIds(String projectId, List<String> sampleIds) {
    var projectIdParsed = ProjectId.parse(projectId);
    return sampleIds.stream()
        .map(id -> sampleService.findSample(projectIdParsed, SampleId.parse(id)))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList();
  }

  private Supplier<Experiment> experimentSupplier(String projectId, String experimentId) {
    return () -> experimentService.find(projectId, ExperimentId.parse(experimentId))
        .orElseThrow(NoSuchExperimentException::new);
  }

  private DigitalObject generateSampleUpdateTemplate(
      Supplier<Experiment> experimentSupplier,
      String projectId,
      String experimentId,
      String batchId) {
    var experiment = experimentSupplier.get();
    var sampleBasic = querySampleBasicInfo(experiment, projectId, experimentId);
    var sampleExtension = querySampleExtension(experiment, projectId, experimentId);
    var samplesInBatch = sampleExtension.samples().stream().filter(isInBatch(batchId))
        .toList();
    if (samplesInBatch.isEmpty()) {
      log.warn("No samples found for experiment during template generation: " + experimentId);
    }

    return templateProvider.getTemplate(new SampleUpdate(
        new SampleInformation(
            samplesInBatch,
            sampleBasic.analysisMethods(),
            sampleBasic.conditions(),
            sampleBasic.analytes(),
            sampleBasic.species(),
            sampleBasic.specimen(),
            sampleExtension.experimentalGroups(),
            sampleBasic.confoundingVariables(),
            sampleExtension.confoundingVariableLevels())
    ));
  }

  private SampleExtension querySampleExtension(Experiment experiment, String projectId,
      String experimentId) {
    var experimentalGroups = experiment.getExperimentalGroups();
    var samples = sampleService.retrieveSamplesForExperiment(
        ProjectId.parse(projectId), experimentId).stream().toList();
    var confoundingVariablesIds = confVariableService.listConfoundingVariablesForExperiment(
            projectId, new ExperimentReference(experimentId)).stream()
        .map(it -> new ConfoundingVariableInformation(it.id(), it.variableName()))
        .map(ConfoundingVariableInformation::id)
        .toList();
    var confoundingVariableLevels = confVariableService.listLevelsForVariables(
        projectId,
        confoundingVariablesIds);
    return new SampleExtension(
        samples,
        experimentalGroups,
        confoundingVariableLevels
    );
  }

  private SampleBasic querySampleBasicInfo(Experiment experiment, String projectId,
      String experimentId) {
    var conditions = experiment.getExperimentalGroups().stream()
        .map(ExperimentalGroup::condition)
        .map(PropertyConversion::toString).toList();
    var species = experiment.getSpecies().stream().map(PropertyConversion::toString).toList();
    var specimen = experiment.getSpecimens().stream().map(PropertyConversion::toString).toList();
    var analytes = experiment.getAnalytes().stream().map(PropertyConversion::toString).toList();
    var analysisMethods = Arrays.stream(AnalysisMethod.values())
        .map(AnalysisMethod::abbreviation)
        .toList();
    var confoundingVariables = confVariableService.listConfoundingVariablesForExperiment(
            projectId, new ExperimentReference(experimentId)).stream()
        .map(it -> new ConfoundingVariableInformation(it.id(), it.variableName()))
        .toList();
    return new SampleBasic(
        analysisMethods,
        conditions,
        analytes,
        species,
        specimen,
        confoundingVariables
    );
  }

  @PreAuthorize(
      "hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'READ') ")
  public DigitalObject sampleInformationTemplate(String projectId, String experimentId,
      MimeType type) {
    if (!isSupportedMimeType(type)) {
      throw new UnsupportedMimeTypeException(UNSUPPORTED_MIME_TYPE + type);
    }
    return generateSampleInfoTemplate(experimentSupplier(projectId, experimentId), projectId,
        experimentId);
  }

  private DigitalObject generateSampleInfoTemplate(Supplier<Experiment> experimentSupplier,
      String projectId, String experimentId) {
    var experiment = experimentSupplier.get();
    var sampleBasic = querySampleBasicInfo(experiment, projectId, experimentId);
    var sampleExtension = querySampleExtension(experiment, projectId, experimentId);
    return templateProvider.getTemplate(
        new SampleInformation(
            sampleExtension.samples(),
            sampleBasic.analysisMethods(),
            sampleBasic.conditions(),
            sampleBasic.analytes(),
            sampleBasic.species(),
            sampleBasic.specimen(),
            sampleExtension.experimentalGroups(),
            sampleBasic.confoundingVariables(),
            sampleExtension.confoundingVariableLevels()
        ));
  }

  private boolean isSupportedMimeType(MimeType mimeType) {
    return mimeType.equalsTypeAndSubtype(templateProvider.providedMimeType());
  }

  private record SampleBasic(
      List<String> analysisMethods,
      List<String> conditions,
      List<String> analytes,
      List<String> species,
      List<String> specimen,
      List<ConfoundingVariableInformation> confoundingVariables
  ) {

  }

  private record SampleExtension(
      List<Sample> samples,
      List<ExperimentalGroup> experimentalGroups,
      List<ConfoundingVariableLevel> confoundingVariableLevels
  ) {

  }

  static class NoSuchExperimentException extends RuntimeException {

    public NoSuchExperimentException() {
      super();
    }
  }

  static class MeasurementNotFound extends RuntimeException {

    public MeasurementNotFound() {
      super();
    }

    public MeasurementNotFound(String message) {
      super(message);
    }
  }
}
