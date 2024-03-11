package life.qbic.projectmanagement.domain.model.measurement;

/**
 * <b>Proteomics Labeling Method</b>
 *
 * <p>Describes a proteomics labeling method applied to prepare for a measurement</p>
 * <p>
 * Example: SILAC with N15 label.
 *
 * @since 1.0.0
 */
public record ProteomicsLabeling(String labelType, String label) {

}
