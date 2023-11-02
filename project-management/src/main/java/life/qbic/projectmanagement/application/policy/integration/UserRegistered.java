package life.qbic.projectmanagement.application.policy.integration;

import java.util.Optional;
import life.qbic.projectmanagement.application.authorization.authorities.QbicUserAuthorityService;
import life.qbic.projectmanagement.application.communication.broadcasting.IntegrationEvent;
import life.qbic.projectmanagement.application.communication.broadcasting.Subscriber;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.scheduling.JobScheduler;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class UserRegistered implements Subscriber {

  private static final String TOPIC = "userRegistered";

  private final JobScheduler jobScheduler;

  private final QbicUserAuthorityService authorityService;

  public UserRegistered(JobScheduler jobScheduler, QbicUserAuthorityService authorityService) {
    this.jobScheduler = jobScheduler;
    this.authorityService = authorityService;
  }

  @Override
  public String type() {
    return TOPIC;
  }

  @Override
  public void onReceive(IntegrationEvent event) {
    if (!event.type().equals(TOPIC)) {
      throw new WrongTypeException("Unknown type " + event.type());
    }
    var userId = Optional.ofNullable(
        event.content().getOrDefault("userId", null));
    System.out.println("Call JobRunner");
    jobScheduler.enqueue(() -> createInitAuthorizationEntry(userId.orElse(null)));
  }

  @Job(name = "create-init-auth-entry")
  public void createInitAuthorizationEntry(String userId) throws RuntimeException {
    if (userId == null || userId.isBlank()) {
      throw new RuntimeException("No user id was provided: " + userId);
    }
    authorityService.createNewAuthEntry(userId);
  }
}
