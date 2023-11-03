package life.qbic.datamanager.views.projects.project.samples.registration.batch;

import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;

/**
 * <p> BatchRegistrationContent contains the batch specific information provided by the user during
 * sample batch creation in the
 * {@link
 * life.qbic.datamanager.views.projects.project.samples.registration.batch.BatchRegistrationDialog}
 * </p>
 *
 * @param batchLabel   name of the to be registered batch
 * @param experimentId the {@link ExperimentId} to which the samples within this batch will be
 *                     registered
 * @param isPilot      specifies if the to be registered batch functions as a pilot
 */
public record BatchRegistrationContent(String batchLabel, ExperimentId experimentId,
                                       boolean isPilot) {

}
