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
import life.qbic.usergroups.domain.event.MemberRemovedFromGroup;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.scheduling.JobScheduler;

/**
 * <b>Directive: inform a user about their revoked group membership</b>
 * <p>
 * When a member has been removed from an ad-hoc group ({@link MemberRemovedFromGroup}), the
 * removed user is notified by email. As a result they may no longer access the projects and data
 * the group was shared with.
 * <p>
 * The email is sent asynchronously through a JobRunr job so a slow mail server never blocks the
 * membership write-path transaction.
 *
 * @since 1.20.0
 */
public class InformRemovedGroupMember implements DomainEventSubscriber<MemberRemovedFromGroup> {

  private final EmailService emailService;
  private final JobScheduler jobScheduler;
  private final UserInformationService userInformationService;
  private final GroupService groupService;

  public InformRemovedGroupMember(EmailService emailService, JobScheduler jobScheduler,
      UserInformationService userInformationService, GroupService groupService) {
    this.emailService = requireNonNull(emailService, "emailService must not be null");
    this.jobScheduler = requireNonNull(jobScheduler, "jobScheduler must not be null");
    this.userInformationService = requireNonNull(userInformationService,
        "userInformationService must not be null");
    this.groupService = requireNonNull(groupService, "groupService must not be null");
  }

  @Override
  public Class<? extends DomainEvent> subscribedToEventType() {
    return MemberRemovedFromGroup.class;
  }

  @Override
  public void handleEvent(MemberRemovedFromGroup event) {
    jobScheduler.enqueue(() -> notifyRemovedMember(
        event.groupId(), event.userId()));
  }

  @Job(name = "Notify user %0 about their removal from group %1")
  public void notifyRemovedMember(String groupId, String userId)
      throws DirectiveExecutionException {
    UserInfo user = userInformationService.findById(userId).orElseThrow(
        () -> new DirectiveExecutionException(
            "User with id %s not found".formatted(userId)));
    // The group may already be dissolved when the last member is removed: the name lookup
    // fails for dissolved groups. In that case we can still notify using the group id as a
    // fallback label so the email keeps a useful reference.
    String groupName = groupService.findGroupById(groupId)
        .map(projection -> projection.groupName().value())
        .orElse(groupId);
    emailService.send(new Subject("You have been removed from a user group"),
        new Recipient(user.emailAddress(), user.fullName()),
        new Content(Messages.removedFromGroup(user.fullName(), groupName)));
  }
}