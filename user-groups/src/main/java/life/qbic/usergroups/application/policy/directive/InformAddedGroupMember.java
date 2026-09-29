package life.qbic.usergroups.application.policy.directive;

import static java.util.Objects.requireNonNull;

import life.qbic.domain.concepts.DomainEvent;
import life.qbic.domain.concepts.DomainEventSubscriber;
import life.qbic.identity.api.UserInfo;
import life.qbic.identity.api.UserInformationService;
import life.qbic.usergroups.application.GroupService;
import life.qbic.usergroups.application.communication.Content;
import life.qbic.usergroups.application.communication.EmailService;
import life.qbic.usergroups.application.communication.Messages;
import life.qbic.usergroups.application.communication.Recipient;
import life.qbic.usergroups.application.communication.Subject;
import life.qbic.usergroups.domain.event.MemberAddedToGroup;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.scheduling.JobScheduler;

/**
 * <b>Directive: inform a user about their new group membership</b>
 * <p>
 * When a member has been added to an ad-hoc group ({@link MemberAddedToGroup}), the added user
 * is notified by email. As a group member they may now access the projects and data the group is
 * shared with.
 * <p>
 * The email is sent asynchronously through a JobRunr job so a slow mail server never blocks the
 * membership write-path transaction.
 *
 * @since 1.20.0
 */
public class InformAddedGroupMember implements DomainEventSubscriber<MemberAddedToGroup> {

  private final EmailService emailService;
  private final JobScheduler jobScheduler;
  private final UserInformationService userInformationService;
  private final GroupService groupService;

  public InformAddedGroupMember(EmailService emailService, JobScheduler jobScheduler,
      UserInformationService userInformationService, GroupService groupService) {
    this.emailService = requireNonNull(emailService, "emailService must not be null");
    this.jobScheduler = requireNonNull(jobScheduler, "jobScheduler must not be null");
    this.userInformationService = requireNonNull(userInformationService,
        "userInformationService must not be null");
    this.groupService = requireNonNull(groupService, "groupService must not be null");
  }

  @Override
  public Class<? extends DomainEvent> subscribedToEventType() {
    return MemberAddedToGroup.class;
  }

  @Override
  public void handleEvent(MemberAddedToGroup event) {
    jobScheduler.enqueue(() -> notifyAddedMember(
        event.groupId(), event.userId()));
  }

  @Job(name = "Notify user %0 about their group membership in group %1")
  public void notifyAddedMember(String groupId, String userId)
      throws DirectiveExecutionException {
    UserInfo user = userInformationService.findById(userId).orElseThrow(
        () -> new DirectiveExecutionException(
            "User with id %s not found".formatted(userId)));
    String groupName = groupService.findGroupById(groupId)
        .map(projection -> projection.groupName().value())
        .orElseThrow(() -> new DirectiveExecutionException(
            "Group with id %s not found".formatted(groupId)));
    emailService.send(new Subject("You have been added to a user group"),
        new Recipient(user.emailAddress(), user.fullName()),
        new Content(Messages.addedToGroup(user.fullName(), groupName)));
  }
}