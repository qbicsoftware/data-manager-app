package life.qbic.datamanager.views.projects.project.samples.registration.batch;

import java.util.List;
import life.qbic.projectmanagement.domain.project.sample.SampleRegistrationRequest;

/**
 * <class short description - One Line!>
 * <p>
 * <More detailed description - When to use, what it solves, etc.>
 *
 * @since <version tag>
 */
public record SampleRegistrationContent(
    List<SampleRegistrationRequest> sampleRegistrationRequests) {

}
