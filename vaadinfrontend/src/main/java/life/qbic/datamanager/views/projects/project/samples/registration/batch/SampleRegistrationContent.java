package life.qbic.datamanager.views.projects.project.samples.registration.batch;


/**
 * <class short description - One Line!>
 * <p>
 * <More detailed description - When to use, what it solves, etc.>
 *
 * @since <version tag>
 */
public record SampleRegistrationContent(String label, String biologicalReplicateId,
                                        Long experimentalGroupId, String species, String specimen,
                                        String analyte,
                                        String comment) {

}
