package life.qbic.projectmanagement.application.api.template;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import life.qbic.projectmanagement.application.api.fair.DigitalObject;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.ConfoundingVariableInformation;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.ConfoundingVariableLevel;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import org.springframework.util.MimeType;

/**
 * Template provider.
 * <p>
 * Templates can be used to provide information for various purposes in the application (e.g. sample
 * registration, update tasks, batch information display, etc.).
 * <p>
 * A template provider prepares these templates to comply with a certain {@link MimeType}.
 *
 * @since 1.10.0
 */
public interface TemplateProvider {

  MimeType providedMimeType();

  /**
   * Requests a template as {@link DigitalObject}.
   * <p>
   * Implementations must guarantee that all known implementations of {@link TemplateRequest} are
   * handled.
   *
   * @param request the template request
   * @return a {@link DigitalObject} that provides the actual template
   * @since 1.10.0
   */
  DigitalObject getTemplate(TemplateRequest request);

  /**
   * A template request is the general typification for requests for different kind of template
   * generation.
   *
   * @since 1.10.0
   */
  sealed interface TemplateRequest permits MeasurementInformationCollectionNGS,
      MeasurementInformationCollectionPxP, SampleInformation, SampleRegistration, SampleUpdate {

  }

  /**
   * Information container for sample registration template requests.
   *
   * @param analysisMethods
   * @param conditions
   * @param analytes
   * @param species
   * @param specimen
   * @param confoundingVariables
   * @since 1.10.0
   */
  record SampleRegistration(
      List<String> analysisMethods,
      List<String> conditions,
      List<String> analytes,
      List<String> species,
      List<String> specimen,
      List<ConfoundingVariableInformation> confoundingVariables
  ) implements TemplateRequest {

  }

  /**
   * Information container for sample update template requests.
   *
   * @param information the sample information to prepare in the template for update
   * @since 1.10.0
   */
  record SampleUpdate(SampleInformation information) implements TemplateRequest {

  }

  /**
   * Information container for sample information template requests.
   *
   * @param samples
   * @param analysisMethods
   * @param conditions
   * @param analytes
   * @param species
   * @param specimen
   * @param experimentalGroups
   * @param confoundingVariables
   * @param confoundingVariableLevels
   * @since 1.10.0
   */
  record SampleInformation(
      List<Sample> samples,
      List<String> analysisMethods,
      List<String> conditions,
      List<String> analytes,
      List<String> species,
      List<String> specimen,
      List<ExperimentalGroup> experimentalGroups,
      List<ConfoundingVariableInformation> confoundingVariables,
      List<ConfoundingVariableLevel> confoundingVariableLevels
  ) implements TemplateRequest {

  }

  record MeasurementInformationCollectionNGS(
      List<MeasurementInformationNGS> measurements) implements TemplateRequest {

    public MeasurementInformationCollectionNGS {
      requireNonNull(measurements);
      measurements = List.copyOf(measurements);
    }
  }

  record MeasurementInformationCollectionPxP(
      List<MeasurementInformationPxP> measurements) implements TemplateRequest {

    public MeasurementInformationCollectionPxP {
      requireNonNull(measurements);
      measurements = List.copyOf(measurements);
    }
  }

  /**
   * Information container for an NGS measurement.
   *
   * @param measurementId         the identifier of the measurement
   * @param organisationIRI       the ROR ID of the organization that performed the measurement
   * @param instrumentIRI         the CURIE of the measurement device used
   * @param facility              the facility within the organization that actually performed the
   *                              measurement
   * @param sequencingReadType    the sequencing read type used
   * @param libraryKit            the library kit used
   * @param flowCell              the flow cell used
   * @param sequencingRunProtocol the sequencing run protocol
   * @param samplePoolGroup       the name of the sample pool
   * @param specificMetadata      specific metadata that differentiates pooled samples as a
   *                              {@link Map}, with the sample ids as keys and the sample-specific
   *                              measurement annotations as values. Will have only one entry if no
   *                              pooling was done. {@link MeasurementSpecificNGS}
   * @since 1.11.0
   */
  record MeasurementInformationNGS(String measurementId, String organisationIRI,
                                   String organisationName,
                                   String instrumentIRI, String instrumentName, String facility,
                                   String sequencingReadType, String libraryKit, String flowCell,
                                   String sequencingRunProtocol, String samplePoolGroup,
                                   Map<String, MeasurementSpecificNGS> specificMetadata,
                                   String measurementName) {

    public MeasurementInformationNGS {
      requireNonNull(measurementId);
      requireNonNull(organisationIRI);
      requireNonNull(organisationName);
      requireNonNull(instrumentIRI);
      requireNonNull(instrumentName);
      requireNonNull(facility);
      requireNonNull(sequencingReadType);
      requireNonNull(libraryKit);
      requireNonNull(flowCell);
      requireNonNull(sequencingRunProtocol);
      requireNonNull(samplePoolGroup);
      requireNonNull(specificMetadata);
      specificMetadata = new HashMap<>(specificMetadata);
      requireNonNull(measurementName);
    }

    /**
     * Returns the {@link List} of sample identifiers this measurement refers to.
     *
     * @return the {@link List} of sample identifiers
     * @since 1.11.0
     */
    public List<String> measuredSamples() {
      return List.copyOf(specificMetadata.keySet());
    }
  }

  /**
   * Metadata that describes measurement properties, that are unique to the sample presented in the
   * measurement (e.g., when pooling was done)
   *
   * @param sampleId   the natural ID of the sample
   * @param sampleName the name of the sample to provide additional context for the measurement
   * @param indexI7    the i7 index used in the measurement to discriminate a sample
   * @param indexI5    the i5 index used in the measurement to discriminate a sample
   * @param comment    some comment from the measuring lab
   * @since 1.11.0
   */
  record MeasurementSpecificNGS(
      String sampleId,
      String sampleName,
      String indexI7,
      String indexI5,
      String comment
  ) {

  }

  /**
   * Information container for a proteomics measurement.
   *
   * @param measurementId          the identifier of the measurement
   * @param technicalReplicateName the name of the technical replicate
   * @param organisationIRI        the ROR ID of the organization that performed the measurement
   * @param msDeviceIRI            the CURIE of the mass spectrometry device used for the
   *                               measurement
   * @param samplePoolGroup        the name of the sample pool
   * @param facility               the name of the facility that performed the measurement
   * @param digestionEnzyme        the enzyme used for proteolytic digestion
   * @param digestionMethod        the digestion method
   * @param enrichmentMethod       the enrichment method used
   * @param injectionVolume        the amount of the analyte injected for the measurement
   * @param lcColumn               the liquid chromatography column used to separate compounds
   * @param lcmsMethod             the method used
   * @param labelingType           the type of the labeling used
   * @param specificMetadata       specific metadata that differentiates pooled samples as a
   *                               {@link Map}, with the sample ids as keys and the sample-specific
   *                               measurement annotations as values. Will have only one entry if no
   *                               pooling was done. {@link MeasurementSpecificPxP}
   * @since 1.11.0
   */
  record MeasurementInformationPxP(
      String measurementId,
      String technicalReplicateName,
      String organisationIRI,
      String organisationName,
      String msDeviceIRI,
      String deviceName,
      String samplePoolGroup,
      String facility,
      String digestionEnzyme,
      String digestionMethod,
      String enrichmentMethod,
      String injectionVolume,
      String lcColumn,
      String lcmsMethod,
      String labelingType,
      Map<String, MeasurementSpecificPxP> specificMetadata,
      String measurementName
  ) {

    public MeasurementInformationPxP {
      requireNonNull(measurementId);
      requireNonNull(technicalReplicateName);
      requireNonNull(organisationIRI);
      requireNonNull(organisationName);
      requireNonNull(msDeviceIRI);
      requireNonNull(deviceName);
      requireNonNull(facility);
      requireNonNull(digestionEnzyme);
      requireNonNull(digestionMethod);
      requireNonNull(enrichmentMethod);
      requireNonNull(injectionVolume);
      requireNonNull(lcColumn);
      requireNonNull(lcmsMethod);
      requireNonNull(labelingType);
      requireNonNull(specificMetadata);
      specificMetadata = new HashMap<>(specificMetadata);
      requireNonNull(measurementName);
    }

    /**
     * Returns the {@link List} of sample identifiers this measurement refers to.
     *
     * @return the {@link List} of sample identifiers
     * @since 1.11.0
     */
    public List<String> measuredSamples() {
      return List.copyOf(specificMetadata.keySet());
    }
  }

  /**
   * Metadata that describes a measurement properties, that are unique to the sample presented in
   * the measurement (e.g., when pooling was done)
   *
   * @param sampleName   the name of the sample to provide additional context for the measurement
   * @param label        the label used to discriminate the sample
   * @param fractionName the fraction name
   * @param comment      some comment from the measuring lab
   * @since 1.11.0
   */
  record MeasurementSpecificPxP(
      String sampleId,
      String sampleName,
      String label,
      String fractionName,
      String comment
  ) {

  }

}



