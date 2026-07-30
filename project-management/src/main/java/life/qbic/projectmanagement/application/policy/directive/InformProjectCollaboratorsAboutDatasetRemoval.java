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
import life.qbic.projectmanagement.domain.model.associated_dataset.event.AssociatedDatasetRemovedEvent;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.project.ProjectIntent;
import life.qbic.projectmanagement.domain.model.project.ProjectTitle;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * <b>Directive: Inform project collaborators about dataset connection removal</b>
 *
 * <p>After a user removes a dataset connection from a project, every other
 * collaborator on that project is notified by email. The actor who performed
 * the removal is deliberately excluded so they do not receive a
 * self-notification.</p>
 *
 * <p>The actual email send runs inside a JobRunr background job so the
 * domain-event handler does not block on SMTP latency (ADR-0002 §12).</p>
 *
 * @since 1.12.0
 */
@Component
public class InformProjectCollaboratorsAboutDatasetRemoval
    implements DomainEventSubscriber<AssociatedDatasetRemovedEvent> {

  private static final Logger log = logger(InformProjectCollaboratorsAboutDatasetRemoval.class);

  private final EmailService emailService;
  private final ProjectAccessService projectAccessService;
  private final UserInformationService userInformationService;
  private final ProjectInformationService projectInformationService;
  private final AppContextProvider appContextProvider;
  private final JobScheduler jobScheduler;

  public InformProjectCollaboratorsAboutDatasetRemoval(
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
    return AssociatedDatasetRemovedEvent.class;
  }

  @Override
  @PreAuthorize(
      "hasPermission(#event.projectId(), 'life.qbic.projectmanagement.domain.model.project.Project', 'READ')")
  public void handleEvent(AssociatedDatasetRemovedEvent event) {
    String projectTitle = projectInformationService.find(event.projectId())
        .map(Project::getProjectIntent)
        .map(ProjectIntent::projectTitle)
        .map(ProjectTitle::title)
        .orElseThrow(() -> new DirectiveExecutionException(
            "Project not found: " + event.projectId()));

    String projectUrl = appContextProvider.urlToProject(event.projectId().value());
    String actorId = event.actorUserId();

    List<RecipientInfo> recipients = resolveRecipientsExcludingActor(event.projectId(), actorId);
    if (recipients.isEmpty()) {
      log.info("No collaborators to notify for dataset removal on project %s".formatted(
          event.projectId()));
      return;
    }

    for (RecipientInfo recipient : recipients) {
      jobScheduler.enqueue(
          () -> notifyRecipient(recipient.email, recipient.fullName,
              recipient.fullName, projectTitle, event.datasetTitle(), event.datasetPid(),
              projectUrl));
    }
  }

  /**
   * Resolves all collaborators on the project, filtering out the actor
   * who performed the removal. Silently skips users whose profile
   * cannot be resolved (e.g. a deleted user account).
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

  @Job(name = "Notify collaborator about dataset removal on project %3 for dataset %4")
  public void notifyRecipient(
      String emailAddress,
      String fullName,
      String addressee,
      String projectTitle,
      String datasetTitle,
      String datasetPid,
      String projectUrl) {
    var subject = new Subject("Dataset connection removed from project");
    var message = Messages.datasetRemovedFromProject(
        addressee, projectTitle, datasetTitle, datasetPid, projectUrl);
    emailService.send(subject,
        new Recipient(emailAddress, fullName),
        new Content(message));
  }

  private record RecipientInfo(String email, String fullName) {}
}
