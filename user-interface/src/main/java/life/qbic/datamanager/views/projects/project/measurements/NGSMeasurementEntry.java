package life.qbic.datamanager.views.projects.project.measurements;

import life.qbic.projectmanagement.domain.model.measurement.NGSMeasurement;

/**
 * <b>NGS Measurement Entry</b>
 * <p>
 * Record that describes various {@link NGSMeasurement} metadata properties.
 * </p>
 */
public record NGSMeasurementEntry(String measurementCode,
                                  SampleInformation sampleInformation,
                                  String organisationId,
                                  String organisationName,
                                  String instrumentCURI,
                                  String instrumentName,
                                  String samplePoolGroup,
                                  String facility,
                                  String readType,
                                  String libraryKit,
                                  String flowCell,
                                  String runProtocol,
                                  String indexI7,
                                  String indexI5,
                                  String comment) {

}
