package life.qbic.datamanager.views.projects.project.measurements;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public record ProteomicsMeasurementEntry(String measurementCode,
                                         SampleInformation sampleIdCodeEntry,
                                         String organisationId,
                                         String organisationName,
                                         String instrumentCURI,
                                         String instrumentName,
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
