package life.qbic.projectmanagement.application.policy.integration;

import java.util.Optional;
import life.qbic.projectmanagement.application.authorization.authorities.AuthorityService;
import life.qbic.projectmanagement.application.communication.broadcasting.IntegrationEvent;
import life.qbic.projectmanagement.application.communication.broadcasting.Subscriber;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.scheduling.JobScheduler;

/**
 * <b>User registered integration</b>
 *
 * <p>Policy that is executed to integrate the received integration event about a new user
 * that has registered.</p>
 * <p>
 * The current implementation makes sure, that a new user has an authorization entry that can be
 * further used to provide users with access to any project.
 * <p>
 * To achieve this, the policy interacts with the {@link AuthorityService}.
 *
 * @since 1.0.0
 */
public class UserRegistered implements Subscriber {

  private static final String TOPIC = "User";

  private final JobScheduler jobScheduler;

  private final AuthorityService authorityService;

  public UserRegistered(JobScheduler jobScheduler, AuthorityService authorityService) {
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
    jobScheduler.enqueue(() -> createInitAuthorizationEntry(userId.orElse(null)));
  }

  /**
   * Creates a new auth entry for a user with a given ID.
   *
   * @param userId the user's unique identifier
   * @throws RuntimeException if the user id is empty or null
   * @since 1.0.0
   */
  @Job(name = "create-init-auth-entry")
  public void createInitAuthorizationEntry(String userId) throws RuntimeException {
    if (userId == null || userId.isBlank()) {
      throw new RuntimeException("No user id was provided: " + userId);
    }
    authorityService.createNewAuthEntry(userId);
  }
}
