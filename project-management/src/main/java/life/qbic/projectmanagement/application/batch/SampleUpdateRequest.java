package life.qbic.projectmanagement.application.batch;

import life.qbic.projectmanagement.domain.model.experiment.BiologicalReplicate;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Species;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Specimen;
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
   * @param sampleLabel          a human-readable semantic descriptor of the sample
   * @param analysisMethod       analysis method to be performed
   * @param biologicalReplicate  the biological replicate the sample has been taken from
   * @param experimentalGroup    the experimental group the sample is part of
   * @param species              the species the sample belongs to
   * @param specimen             the specimen the sample belongs to.
   * @param analyte              the analyte the sample belongs to
   * @param comment              comment relating to the sample
   */
  public record SampleInformation(String sampleLabel, AnalysisMethod analysisMethod,
                                  BiologicalReplicate biologicalReplicate,
                                  ExperimentalGroup experimentalGroup, Species species,
                                  Specimen specimen, Analyte analyte, String comment) {

  }

}
