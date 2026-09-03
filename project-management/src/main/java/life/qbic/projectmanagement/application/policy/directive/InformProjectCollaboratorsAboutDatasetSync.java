package life.qbic.projectmanagement.application.policy.directive;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.ArrayList;
import java.util.List;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.domain.concepts.DomainEventSubscriber;
import life.qbic.identity.api.UserInformationService;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.AppContextProvider;
import life.qbic.projectmanagement.application.Messages;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.ProjectCollaborator;
import life.qbic.projectmanagement.application.communication.Content;
import life.qbic.projectmanagement.application.communication.EmailService;
import life.qbic.projectmanagement.application.communication.Recipient;
import life.qbic.projectmanagement.application.communication.Subject;
import life.qbic.projectmanagement.domain.model.associated_dataset.event.AssociatedDatasetsSyncedEvent;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.project.ProjectIntent;
import life.qbic.projectmanagement.domain.model.project.ProjectTitle;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * <b>Directive: Inform project collaborators about a dataset sync</b>
 *
 * <p>After a sync trigger updated one or more connected datasets, every
 * collaborator on that project is notified with a <em>single combined
 * email</em> listing all updated records (ADR-0005 N1). The actor who
 * triggered the sync is deliberately excluded so they do not receive a
 * self-notification (they see the sync results sidecar instead).</p>
 *
 * <p>The event is only emitted when at least one record actually changed;
 * no-op syncs and failures never reach this directive. The actual email
 * send runs inside a JobRunr background job so the domain-event handler
 * does not block on SMTP latency.</p>
 *
 * @since 1.13.0
 */
@Component
public class InformProjectCollaboratorsAboutDatasetSync
    implements DomainEventSubscriber<AssociatedDatasetsSyncedEvent> {

  private static final Logger log = logger(InformProjectCollaboratorsAboutDatasetSync.class);

  private final EmailService emailService;
  private final ProjectAccessService projectAccessService;
  private final UserInformationService userInformationService;
  private final ProjectInformationService projectInformationService;
  private final AppContextProvider appContextProvider;
  private final JobScheduler jobScheduler;

  public InformProjectCollaboratorsAboutDatasetSync(
      EmailService emailService,
      ProjectAccessService projectAccessService,
      UserInformationService userInformationService,
      ProjectInformationService projectInformationService,
      AppContextProvider appContextProvider,
      JobScheduler jobScheduler) {
    this.emailService = requireNonNull(emailService);
    this.projectAccessService = requireNonNull(projectAccessService);
    this.userInformationService = requireNonNull(userInformationService);
    this.projectInformationService = requireNonNull(projectInformationService);
    this.appContextProvider = requireNonNull(appContextProvider);
    this.jobScheduler = requireNonNull(jobScheduler);
  }

  @Override
  public Class<? extends DomainEvent> subscribedToEventType() {
    return AssociatedDatasetsSyncedEvent.class;
  }

  @Override
  @PreAuthorize(
      "hasPermission(#event.projectId(), 'life.qbic.projectmanagement.domain.model.project.Project', 'READ')")
  public void handleEvent(AssociatedDatasetsSyncedEvent event) {
    String projectTitle = projectInformationService.find(event.projectId())
        .map(Project::getProjectIntent)
        .map(ProjectIntent::projectTitle)
        .map(ProjectTitle::title)
        .orElseThrow(() -> new DirectiveExecutionException(
            "Project not found: " + event.projectId()));

    String projectUrl = appContextProvider.urlToProject(event.projectId().value());
    String actorId = event.actorUserId();

    // Flatten the updated records into pre-rendered lines so the JobRunr
    // job only carries plain strings (no domain types across serialization).
    List<String> updatedRecordLines = event.updatedRecords().stream()
        .map(record -> Messages.updatedRecordLine(
            record.title(), record.pid(), record.previousVersion(),
            record.newVersion(), record.accessStatusChanged()))
        .toList();

    List<RecipientInfo> recipients = resolveRecipientsExcludingActor(event.projectId(), actorId);
    if (recipients.isEmpty()) {
      log.info("No collaborators to notify for dataset sync on project %s".formatted(
          event.projectId()));
      return;
    }

    for (RecipientInfo recipient : recipients) {
      jobScheduler.enqueue(
          () -> notifyRecipient(recipient.email, recipient.fullName,
              recipient.fullName, projectTitle, updatedRecordLines, projectUrl));
    }
  }

  /**
   * Resolves all collaborators on the project, filtering out the actor
   * who triggered the sync. Silently skips users whose profile cannot be
   * resolved (e.g. a deleted user account).
   */
  private List<RecipientInfo> resolveRecipientsExcludingActor(ProjectId projectId, String actorId) {
    List<RecipientInfo> recipients = new ArrayList<>();
    List<String> userIds = projectAccessService.listCollaborators(projectId).stream()
        .map(ProjectCollaborator::userId)
        .toList();
    for (String userId : userIds) {
      if (userId.equals(actorId)) {
        continue;
      }
      userInformationService.findById(userId)
          .ifPresent(info -> recipients.add(new RecipientInfo(info.emailAddress(), info.fullName())));
    }
    return recipients;
  }

  @Job(name = "Notify collaborator about dataset sync on project %3")
  public void notifyRecipient(
      String emailAddress,
      String fullName,
      String addressee,
      String projectTitle,
      List<String> updatedRecordLines,
      String projectUrl) {
    var subject = new Subject("Connected datasets updated in project");
    var message = Messages.datasetsSyncedToProject(
        addressee, projectTitle, updatedRecordLines, projectUrl);
    emailService.send(subject,
        new Recipient(emailAddress, fullName),
        new Content(message));
  }

  private record RecipientInfo(String email, String fullName) {}
}