package life.qbic.projectmanagement.domain.model.measurement;

import java.time.LocalDate;
import life.qbic.projectmanagement.domain.model.OntologyTerm;

/**
 * <b>IP Method Metadata</b>
 * <p>
 * A logical container aggregating business concepts of immunopeptidomics measurement method
 * details together. All fields are at the measurement level (shared across all samples in a
 * pooled measurement).
 *
 * @since 1.11.0
 */
public record IPMethodMetadata(
    OntologyTerm instrument,
    String instrumentName,
    String facility,
    Double sampleMass,
    Double sampleVolume,
    String cycleFractionName,
    String mhcAntibody,
    String mhcTypingMethod,
    String enrichmentMethod,
    LocalDate prepDate,
    LocalDate msRunDate,
    String lcmsMethod,
    String lcColumn,
    String dataAcquisition,
    String massRange,
    Integer retentionTimeRange,
    String chargeRange,
    String ionMobilityRange,
    String comment) {

}
