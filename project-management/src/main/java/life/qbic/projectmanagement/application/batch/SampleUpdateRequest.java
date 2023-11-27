package life.qbic.projectmanagement.application.batch;

import life.qbic.projectmanagement.domain.model.experiment.BiologicalReplicate;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Species;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Specimen;
import life.qbic.projectmanagement.domain.model.sample.AnalysisMethod;
import life.qbic.projectmanagement.domain.model.sample.SampleId;

/**
 * <class short description - One Line!>
 *
 * <More detailed description - When to use, what it solves, etc.>
 *
 * @since <version tag>
 *
 */
public record SampleUpdateRequest(SampleId sampleId, SampleInformation sampleInformation) {

  public record SampleInformation(String sampleLabel, AnalysisMethod analysisMethod,
                                  BiologicalReplicate biologicalReplicate,
                                  ExperimentalGroup experimentalGroup, Species species,
                                  Specimen specimen, Analyte analyte, String comment) {

  }

}
