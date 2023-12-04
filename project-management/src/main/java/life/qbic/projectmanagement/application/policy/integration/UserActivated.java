package life.qbic.projectmanagement.application.policy.integration;

import life.qbic.projectmanagement.application.authorization.authorities.AuthorityService;
import life.qbic.projectmanagement.application.communication.broadcasting.IntegrationEvent;
import life.qbic.projectmanagement.application.communication.broadcasting.Subscriber;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.scheduling.JobScheduler;

/**
 * Subscriber to UserActivated integration events.
 * @since 1.0.0
 */
public class UserActivated implements Subscriber {

  private static final String TOPIC = "userActivated";

  private final JobScheduler jobScheduler;

  private final AuthorityService authorityService;

  public UserActivated(JobScheduler jobScheduler, AuthorityService authorityService) {
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
    var userId = event.content().getOrDefault("userId", null);
    jobScheduler.enqueue(() -> createAuthorizationIdentity(userId));
  }

  /**
   * Creates a new auth entry for a user with a given ID.
   *
   * @param userId the user's unique identifier
   * @throws RuntimeException if the user id is empty or null
   * @since 1.0.0
   */
  @Job(name = "create new authorization identity for user %0")
  public void createAuthorizationIdentity(String userId) {
    if (userId == null || userId.isBlank()) {
      throw new IllegalArgumentException("No user id was provided");
    }
    authorityService.createNewAuthEntry(userId);
  }
}
