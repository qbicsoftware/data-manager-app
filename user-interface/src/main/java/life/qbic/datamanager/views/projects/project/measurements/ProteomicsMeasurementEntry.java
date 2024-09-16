package life.qbic.datamanager.views.projects.project.measurements;

/**
 * <b>Proteomics Measurement Entry</b>
 * <p>
 * Record that describes various proteomics measurement metadata properties.
 *
 * @since 1.0.0
 */
public record ProteomicsMeasurementEntry(String measurementCode,
                                         SampleInformation sampleInformation,
                                         String organisationId,
                                         String organisationName,
                                         String msDeviceCURIE,
                                         String msDeviceName,
                                         String samplePoolGroup,
                                         String facility,
                                         String fractionName,
                                         String digestionEnzyme,
                                         String digestionMethod,
                                         String enrichmentMethod,
                                         String injectionVolume,
                                         String lcColumn,
                                         String lcmsMethod,
                                         String labelingType,
                                         String label,
                                         String comment) {


}
