package life.qbic.datamanager.views.projects.project.samples.registration.batch;

import life.qbic.projectmanagement.domain.project.experiment.BiologicalReplicateId;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Species;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Specimen;
import life.qbic.projectmanagement.domain.project.sample.AnalysisMethod;

/**
 * <p> SampleRegistrationContent contains the sample information provided by the user during sample batch creation in the {@link life.qbic.datamanager.views.projects.project.samples.registration.batch.BatchRegistrationDialog}
 * </p>
 *
 * @param analysisMethod        The {@link AnalysisMethod} to be performed
 * @param label                 User specified sample label
 * @param biologicalReplicateId the biological replicate id from which the sample was derived
 * @param experimentalGroupId   the experimental group id hosting the conditions applicable for the sample
 * @param species               String representation of the {@link Species}
 * @param specimen              String representation of the {@link Specimen}
 * @param analyte               String representation of the {@link Analyte}
 * @param comment               Sample specific comments
 */

public record SampleRegistrationContent(AnalysisMethod analysisMethod, String label,
                                        BiologicalReplicateId biologicalReplicateId,
                                        Long experimentalGroupId,
                                        String species, String specimen, String analyte,
                                        String comment) {

}
