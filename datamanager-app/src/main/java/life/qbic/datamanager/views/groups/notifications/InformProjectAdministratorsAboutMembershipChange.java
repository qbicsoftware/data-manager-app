package life.qbic.datamanager.views.groups.notifications;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.ArrayList;
import java.util.List;
import life.qbic.identity.api.UserInformationService;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.usergroups.api.GroupInformationService;
import life.qbic.usergroups.application.communication.Content;
import life.qbic.usergroups.application.communication.EmailService;
import life.qbic.usergroups.application.communication.Recipient;
import life.qbic.usergroups.application.communication.Subject;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.scheduling.JobScheduler;

/**
 * <b>Directive: inform project owners/admins about a membership change on a shared group</b>
 *
 * <p>When a member is added to or removed from a group that is shared onto projects
 * (FEAT-USER-GROUPS-02 AC2), the project owners and admins (project OWNER/ADMIN role holders) of
 * every project the group is shared with are informed by email. Membership changes alter the
 * effective access of the affected user on those projects, so the people governing the projects
 * need to know.</p>
 *
 * <p>The directive is deliberately a <b>composition-root</b> concern: it bridges the user-groups
 * membership events to the project-management ACL reverse lookup
 * ({@link ProjectAccessService#getAccessibleProjectsForSid(String)}) and the principal
 * OWNER/ADMIN enumeration ({@link ProjectAccessService#listProjectAdministrators(ProjectId)}),
 * which runs <b>without</b> a security context inside the JobRunr job. It therefore reuses the
 * <em>user-groups</em> {@link EmailService} port (wired to {@code UserGroupsEmailServiceProvider}
 * in the composition root).</p>
 *
 * <p>Because the in-process {@code DomainEventDispatcher} binds one subscriber instance to exactly
 * one event type, this class exposes a single entry point ({@link #handleMemberChange(String)})
 * and the composition-root policy registers it once per supported event type
 * ({@code MemberAddedToGroup} and {@code MemberRemovedFromGroup}). Groups that are shared on no
 * project, or projects with no resolvable owner/admin, are silently skipped.</p>
 *
 * @since 1.22.0
 */
public class InformProjectAdministratorsAboutMembershipChange {

  private static final Logger log =
      logger(InformProjectAdministratorsAboutMembershipChange.class);

  private static final String GROUP_SID_PREFIX = "GROUP_";

  private final EmailService emailService;
  private final JobScheduler jobScheduler;
  private final ProjectAccessService projectAccessService;
  private final GroupInformationService groupInformationService;
  private final UserInformationService userInformationService;

  public InformProjectAdministratorsAboutMembershipChange(
      EmailService emailService,
      JobScheduler jobScheduler,
      ProjectAccessService projectAccessService,
      GroupInformationService groupInformationService,
      UserInformationService userInformationService) {
    this.emailService = requireNonNull(emailService, "emailService must not be null");
    this.jobScheduler = requireNonNull(jobScheduler, "jobScheduler must not be null");
    this.projectAccessService = requireNonNull(projectAccessService,
        "projectAccessService must not be null");
    this.groupInformationService = requireNonNull(groupInformationService,
        "groupInformationService must not be null");
    this.userInformationService = requireNonNull(userInformationService,
        "userInformationService must not be null");
  }

  /**
   * Handles a membership change (member added to or removed from a group) and schedules the
   * project-administrator notification jobs. Called once per supported event type by the
   * policy.
   */
  public void handleMemberChange(String groupId) {
    String groupName = groupInformationService.findGroupById(groupId)
        .map(groupInfo -> groupInfo.name())
        .orElse(groupId);
    List<ProjectId> sharedProjects =
        projectAccessService.getAccessibleProjectsForSid(GROUP_SID_PREFIX + groupId);
    if (sharedProjects.isEmpty()) {
      return;
    }
    for (ProjectId projectId : sharedProjects) {
      List<String> administrators = projectAccessService.listProjectAdministrators(projectId);
      List<RecipientInfo> recipients = resolveRecipients(administrators);
      if (recipients.isEmpty()) {
        continue;
      }
      for (RecipientInfo recipient : recipients) {
        jobScheduler.enqueue(() -> notifyProjectAdministrator(
            recipient.email(), recipient.fullName(), groupName));
      }
    }
  }

  /**
   * Resolves the email addresses/full names of the given principals, silently skipping user ids
   * that cannot be resolved anymore (e.g. deleted user accounts).
   */
  private List<RecipientInfo> resolveRecipients(List<String> userIds) {
    List<RecipientInfo> recipients = new ArrayList<>();
    for (String userId : userIds) {
      userInformationService.findById(userId)
          .ifPresent(info -> recipients.add(new RecipientInfo(info.emailAddress(),
              info.fullName())));
    }
    return recipients;
  }

  @Job(name = "Inform project administrator about membership change on group %2")
  public void notifyProjectAdministrator(String emailAddress, String fullName, String groupName) {
    var subject = new Subject("Membership changed in a group shared with your project");
    var content = new Content(Messages.membershipChange(fullName, groupName));
    emailService.send(subject, new Recipient(emailAddress, fullName), content);
  }

  private record RecipientInfo(String email, String fullName) {

  }

  /**
   * Composition-root-local message templates for the project-administrator notification.
   */
  static final class Messages {

    private Messages() {
      // utility
    }

    static String membershipChange(String fullName, String groupName) {
      return String.format("""
          Dear %s,

          the membership of the user group "%s" that is shared with one of your projects has
          changed.

          As the owner or administrator of the project you are receiving this notice so you are
          aware of the change in effective access. No action is required unless this change was
          not intended.

          With kind regards,

          Your QBiC team
          """, fullName, groupName);
    }
  }
}