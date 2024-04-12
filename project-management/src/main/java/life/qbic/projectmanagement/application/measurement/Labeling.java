package life.qbic.projectmanagement.application.measurement;

/**
 * <b>Labeling</b>
 * <p>
 * Describing an association between a sample code and the label strategy for a measurement.
 *
 * @since 1.0.0
 */
public record Labeling(String sampleCode, String labelType, String label) {

}
