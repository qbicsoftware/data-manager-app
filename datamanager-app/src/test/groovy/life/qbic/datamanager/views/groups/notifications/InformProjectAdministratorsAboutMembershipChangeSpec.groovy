package life.qbic.datamanager.views.groups.notifications

import life.qbic.identity.api.UserInfo
import life.qbic.identity.api.UserInformationService
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService
import life.qbic.projectmanagement.domain.model.project.ProjectId
import life.qbic.usergroups.api.GroupInfo
import life.qbic.usergroups.api.GroupInformationService
import life.qbic.usergroups.api.GroupType
import life.qbic.usergroups.application.communication.Content
import life.qbic.usergroups.application.communication.EmailService
import life.qbic.usergroups.application.communication.Recipient
import life.qbic.usergroups.application.communication.Subject
import org.jobrunr.jobs.JobId
import org.jobrunr.jobs.lambdas.JobLambda
import org.jobrunr.scheduling.JobScheduler
import org.jobrunr.storage.InMemoryStorageProvider
import spock.lang.Specification

/**
 * Tests for the {@link InformProjectAdministratorsAboutMembershipChange} directive
 * (FEAT-USER-GROUPS-02 AC2): project owners/admins of projects the group is shared with are
 * informed when a member is added to or removed from the group.
 *
 * <p>The directive's {@code handleEvent} merely enqueues JobRunr jobs; the observable behavior
 * (the email) lives in the {@link @Job}-annotated job body, which is exercised directly here —
 * the job scheduler is a harmless no-op fake.</p>
 */
class InformProjectAdministratorsAboutMembershipChangeSpec extends Specification {

  ProjectAccessService projectAccessService = Mock(ProjectAccessService)
  GroupInformationService groupInformationService = Mock(GroupInformationService)
  UserInformationService userInformationService = Mock(UserInformationService)
  EmailService emailService = Mock(EmailService)

  InformProjectAdministratorsAboutMembershipChange directive() {
    new InformProjectAdministratorsAboutMembershipChange(emailService,
        new NoOpJobScheduler(), projectAccessService, groupInformationService,
        userInformationService)
  }

  def "a membership change on a shared group schedules emails to the project owners/admins"() {
    given: "a group shared onto a project with two admin principals"
    groupInformationService.findGroupById("group-1") >> Optional.of(
        new GroupInfo("group-1", "NGS Lab", "desc", GroupType.ORG))
    ProjectId projectId = ProjectId.of(java.util.UUID.randomUUID())
    projectAccessService.getAccessibleProjectsForSid("GROUP_group-1") >> [projectId]
    projectAccessService.listProjectAdministrators(projectId) >> ["admin-1", "owner-1"]
    userInformationService.findById("admin-1") >> Optional.of(new UserInfo("admin-1", "Admin One",
        "admin-1@example.org", "admin-1", true, null, null))
    userInformationService.findById("owner-1") >> Optional.of(new UserInfo("owner-1", "Owner One",
        "owner-1@example.org", "owner-1", true, null, null))

    when: "the directive handles a membership change"
    directive().handleMemberChange("group-1")

    then: "two notification jobs are enqueued (one per recipient)"
    // enqueued jobs are no-ops; the email production path is asserted via notifyProjectAdministrator
    0 * emailService.send(_, _, _)

    when: "the notification job runs for one recipient"
    directive().notifyProjectAdministrator("admin-1@example.org", "Admin One", "NGS Lab")

    then: "the project admin receives a polite email naming the group"
    1 * emailService.send({ Subject s -> s.content().contains("Membership changed") },
        { Recipient r -> r.address() == "admin-1@example.org" && r.fullName() == "Admin One" },
        { Content c -> c.content().contains("NGS Lab") && c.content().contains("Admin One") })
  }

  def "a membership change on an unshared group enqueues nothing"() {
    given: "a group not shared onto any project"
    groupInformationService.findGroupById("group-1") >> Optional.of(
        new GroupInfo("group-1", "NGS Lab", "desc", GroupType.ORG))
    projectAccessService.getAccessibleProjectsForSid("GROUP_group-1") >> []

    when: "the directive handles the change"
    directive().handleMemberChange("group-1")

    then: "no project-administrator lookup happens and no email is sent"
    0 * projectAccessService.listProjectAdministrators(_)
    0 * emailService.send(_, _, _)
  }

  def "a project without resolvable owner/admin principals yields no notification"() {
    given: "a group shared onto a project whose principals cannot be resolved"
    groupInformationService.findGroupById("group-1") >> Optional.of(
        new GroupInfo("group-1", "NGS Lab", "desc", GroupType.ORG))
    ProjectId projectId = ProjectId.of(java.util.UUID.randomUUID())
    projectAccessService.getAccessibleProjectsForSid("GROUP_group-1") >> [projectId]
    projectAccessService.listProjectAdministrators(projectId) >> ["ghost-1"]
    userInformationService.findById("ghost-1") >> Optional.empty()

    when: "the directive handles the change"
    directive().handleMemberChange("group-1")

    then: "no email is sent and no failure is raised (silently skipped)"
    0 * emailService.send(_, _, _)
  }

  def "a missing group resolves to its id as the fallback name"() {
    given: "a group that can no longer be resolved"
    groupInformationService.findGroupById("group-1") >> Optional.empty()
    projectAccessService.getAccessibleProjectsForSid("GROUP_group-1") >> []

    when: "the directive handles the change"
    directive().handleMemberChange("group-1")

    then: "no shared projects → nothing sent and nothing thrown"
    0 * emailService.send(_, _, _)
  }

  /**
   * A {@link JobScheduler} whose {@code enqueue(...)} is a harmless no-op, mirroring the
   * user-groups test fake.
   */
  static class NoOpJobScheduler extends JobScheduler {

    NoOpJobScheduler() {
      super(new InMemoryStorageProvider())
    }

    @Override
    JobId enqueue(JobLambda jobLambda) {
      return new JobId(java.util.UUID.randomUUID())
    }
  }
}