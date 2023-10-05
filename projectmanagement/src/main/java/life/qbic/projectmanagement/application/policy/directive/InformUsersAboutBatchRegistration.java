package life.qbic.projectmanagement.application.policy.directive;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import life.qbic.authentication.domain.user.concept.User;
import life.qbic.authentication.domain.user.concept.UserId;
import life.qbic.authentication.domain.user.repository.UserRepository;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.domain.concepts.DomainEventSubscriber;
import life.qbic.domain.concepts.communication.CommunicationService;
import life.qbic.domain.concepts.communication.Content;
import life.qbic.domain.concepts.communication.Recipient;
import life.qbic.domain.concepts.communication.Subject;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.Messages;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.sample.Sample;
import life.qbic.projectmanagement.domain.project.sample.event.SampleBatchRegistered;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.stereotype.Component;

/**
 * <b>Directive: Send email to project users about new sample batch</b>
 * <p>
 * After a sample batch has been registered, we need to inform users about
 * the newly registered samples.
 *
 * @since 1.0.0
 */
@Component
public class InformUsersAboutBatchRegistration implements DomainEventSubscriber<SampleBatchRegistered> {

  private static final Logger log = logger(InformUsersAboutBatchRegistration.class);
  private final CommunicationService communicationService;
  private final ProjectAccessService accessService;
  private final UserRepository userRepository;
  private final JobScheduler jobScheduler;

  public InformUsersAboutBatchRegistration(CommunicationService communicationService,
      ProjectAccessService accessService, UserRepository userRepository, JobScheduler jobScheduler) {
    this.accessService = accessService;
    this.userRepository = userRepository;
    this.communicationService = communicationService;
    this.jobScheduler = jobScheduler;
  }

  @Override
  public Class<? extends DomainEvent> subscribedToEventType() {
    return SampleBatchRegistered.class;
  }

  @Override
  public void handleEvent(SampleBatchRegistered event) {
    jobScheduler.enqueue(() -> notifyUsersAboutSamples(event.project(), event.samples()));
  }

  @Job(name = "Notify_Users_About_Batch")
  /**
   * Sends an email with attached spreadsheet of newly registered samples to the users of a project.
   * The email contains an explanation about sample identifiers and the spreadsheet the known metadata of the samples
   * @param project - the project the sample batch was added to
   * @param sampleBatch - the samples that were registered
   */
  public void notifyUsersAboutSamples(Project project, Collection<Sample> sampleBatch) {
    List<User> usersWithAccess = getUsersWithAccess(project.getId());
    String attachmentContent = prepareSpreadsheetContent(sampleBatch);
    for(User user : usersWithAccess) {
      if(user.isActive()) {
        notifyUser(user, project, attachmentContent);
      }
    }
  }

  private List<User> getUsersWithAccess(ProjectId projectId) {
    List<User> users = new ArrayList<>();
    List<UserId> userIds = projectAccess.listUsers(projectId);
    for(UserId id : userIds) {
      userRepository.findById(id).ifPresent(user -> users.add(user));
    }
    return users;
  }

  private String prepareSpreadsheetContent(Collection<Sample> samples) {
    StringBuilder builder = new StringBuilder();
    List<String> header = Arrays.asList("Label", "Sample Code", "Replicate ID", "Origin",
        "Analysis Type", "Comment");
    builder.append(String.join("\t", header));
    builder.append("\n");
    for(Sample sample : samples) {
      List<String> row = new ArrayList<>();
      row.add(sample.label());
      row.add(sample.sampleCode().code());
      row.add(sample.biologicalReplicateId().toString());
      row.add(sample.sampleOrigin().toString());
      row.add(sample.analysisMethod().description());
      row.add(sample.comment().orElse(""));
      builder.append(String.join("\t", row));
      builder.append("\n");
    }
    return builder.toString();
  }

  private void notifyUser(User user, Project project, String attachmentContent) {
    String subject = "New samples added to project";
    String projectUri = project.getId().toString();
    String projectTitle = project.getProjectIntent().projectTitle().title();

    var message = Messages.samplesAddedToProject(user.fullName().get(), projectTitle, projectUri);

    communicationService.send(new Subject(subject),
        new Recipient(user.emailAddress().get(), user.fullName().get()), new Content(message),
        attachmentContent, "sample_sheet.tsv");
  }

}
