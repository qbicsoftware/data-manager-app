package life.qbic.projectmanagement.application.batch;

import life.qbic.projectmanagement.domain.model.OntologyTermV1;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.model.sample.AnalysisMethod;
import life.qbic.projectmanagement.domain.model.sample.SampleId;

/**
 * Sample update request.
 * <p>
 * Serves as a parameter object for sample update.
 * @param sampleId            the {@link SampleId} of the sample to be updated
 * @param sampleInformation   the {@link SampleInformation} containing the content of the sample to be updated
 */
public record SampleUpdateRequest(SampleId sampleId, SampleInformation sampleInformation) {

  /**
   * Sample update request.
   * <p>
   * @param sampleName           a human-readable semantic descriptor of the sample
   * @param biologicalReplicate  optional identifier of the sample's source patient or organism, to
   *                             be able to group biological replicates
   * @param analysisMethod       analysis method to be performed
   * @param experimentalGroup    the experimental group the sample is part of
   * @param species              the species the sample belongs to
   * @param specimen             the specimen the sample belongs to.
   * @param analyte              the analyte the sample belongs to
   * @param comment              comment relating to the sample
   */
  public record SampleInformation(String sampleName, String biologicalReplicate,
                                  AnalysisMethod analysisMethod,
                                  ExperimentalGroup experimentalGroup, OntologyTermV1 species,
                                  OntologyTermV1 specimen, OntologyTermV1 analyte,
                                  String comment) {

  }

}
