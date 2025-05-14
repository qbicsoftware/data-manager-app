package life.qbic.datamanager;

import life.qbic.broadcasting.Exchange;
import life.qbic.broadcasting.MessageBusSubmission;
import life.qbic.domain.concepts.SimpleEventStore;
import life.qbic.domain.concepts.TemporaryEventRepository;
import life.qbic.identity.api.UserInformationService;
import life.qbic.identity.api.UserPasswordService;
import life.qbic.identity.application.communication.EmailService;
import life.qbic.identity.application.communication.broadcasting.EventHub;
import life.qbic.identity.application.notification.NotificationService;
import life.qbic.identity.application.service.BasicUserInformationService;
import life.qbic.identity.application.user.IdentityService;
import life.qbic.identity.application.user.password.NewPassword;
import life.qbic.identity.application.user.password.NewPasswordInput;
import life.qbic.identity.application.user.password.PasswordResetInput;
import life.qbic.identity.application.user.password.PasswordResetRequest;
import life.qbic.identity.application.user.policy.EmailConfirmationLinkSupplier;
import life.qbic.identity.application.user.policy.UserRegisteredPolicy;
import life.qbic.identity.application.user.policy.directive.WhenUserActivatedSubmitIntegrationEvent;
import life.qbic.identity.application.user.policy.directive.WhenUserRegisteredSendConfirmationEmail;
import life.qbic.identity.application.user.policy.directive.WhenUserRegisteredSubmitIntegrationEvent;
import life.qbic.identity.domain.repository.UserDataStorage;
import life.qbic.identity.domain.repository.UserRepository;
import life.qbic.infrastructure.email.EmailServiceProvider;
import life.qbic.infrastructure.email.identity.IdentityEmailServiceProvider;
import life.qbic.infrastructure.email.project.ProjectManagementEmailServiceProvider;
import life.qbic.projectmanagement.application.AppContextProvider;
import life.qbic.projectmanagement.application.OrganisationRepository;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.api.SampleCodeService;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService;
import life.qbic.projectmanagement.application.authorization.authorities.AuthorityService;
import life.qbic.projectmanagement.application.batch.BatchRegistrationService;
import life.qbic.projectmanagement.application.communication.broadcasting.MessageRouter;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService;
import life.qbic.projectmanagement.application.measurement.MeasurementLookupService;
import life.qbic.projectmanagement.application.policy.BatchRegisteredPolicy;
import life.qbic.projectmanagement.application.policy.ExperimentCreatedPolicy;
import life.qbic.projectmanagement.application.policy.ExperimentUpdatedPolicy;
import life.qbic.projectmanagement.application.policy.MeasurementCreatedPolicy;
import life.qbic.projectmanagement.application.policy.MeasurementUpdatedPolicy;
import life.qbic.projectmanagement.application.policy.OfferAddedPolicy;
import life.qbic.projectmanagement.application.policy.ProjectAccessGrantedPolicy;
import life.qbic.projectmanagement.application.policy.ProjectChangedPolicy;
import life.qbic.projectmanagement.application.policy.ProjectRegisteredPolicy;
import life.qbic.projectmanagement.application.policy.QCAddedPolicy;
import life.qbic.projectmanagement.application.policy.SampleDeletedPolicy;
import life.qbic.projectmanagement.application.policy.SampleRegisteredPolicy;
import life.qbic.projectmanagement.application.policy.directive.AddSampleToBatch;
import life.qbic.projectmanagement.application.policy.directive.CreateNewSampleStatisticsEntry;
import life.qbic.projectmanagement.application.policy.directive.DeleteSampleFromBatch;
import life.qbic.projectmanagement.application.policy.directive.InformUserAboutGrantedAccess;
import life.qbic.projectmanagement.application.policy.directive.InformUsersAboutBatchRegistration;
import life.qbic.projectmanagement.application.policy.directive.UpdateProjectUponBatchCreation;
import life.qbic.projectmanagement.application.policy.directive.UpdateProjectUponBatchUpdate;
import life.qbic.projectmanagement.application.policy.directive.UpdateProjectUponDeletionEvent;
import life.qbic.projectmanagement.application.policy.directive.UpdateProjectUponExperimentCreation;
import life.qbic.projectmanagement.application.policy.directive.UpdateProjectUponExperimentUpdate;
import life.qbic.projectmanagement.application.policy.directive.UpdateProjectUponMeasurementCreation;
import life.qbic.projectmanagement.application.policy.directive.UpdateProjectUponMeasurementUpdate;
import life.qbic.projectmanagement.application.policy.directive.UpdateProjectUponPurchaseCreation;
import life.qbic.projectmanagement.application.policy.directive.UpdateProjectUponQCCreation;
import life.qbic.projectmanagement.application.policy.directive.UpdateProjectUponSampleCreation;
import life.qbic.projectmanagement.application.policy.integration.BatchUpdatedPolicy;
import life.qbic.projectmanagement.application.policy.integration.UserActivated;
import life.qbic.projectmanagement.application.purchase.ProjectPurchaseService;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.application.sample.qualitycontrol.QualityControlService;
import life.qbic.projectmanagement.domain.repository.ProjectRepository;
import life.qbic.projectmanagement.infrastructure.CachedOrganisationRepository;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import reactor.core.scheduler.Scheduler;

/**
 * <b>App bean configuration class</b>
 *
 * <p>Not all components can be generated on the fly by Spring, some we have to call explicitly via
 * factory methods.
 *
 * @since 1.0.0
 */
@Configuration
@ComponentScan({"life.qbic.identity.infrastructure", "life.qbic.datamanager.announcements"})
public class AppConfig {
  /*
  Wiring up identity application core and policies

  Section starts below
  */
  @Bean
  public Scheduler reactiveScheduler() {
    return VirtualThreadScheduler.getScheduler();
  }


  @Bean
  public IdentityService userRegistrationService(
      UserRepository userRepository
  ) {
    return new IdentityService(userRepository);
  }

  @Bean
  public OrganisationRepository organisationRepository() {
    return new CachedOrganisationRepository();
  }


  @Bean
  public NewPasswordInput newPasswordInput(IdentityService identityService) {
    return new NewPassword(identityService);
  }

  @Bean
  public PasswordResetInput passwordResetInput(IdentityService identityService) {
    return new PasswordResetRequest(identityService);
  }

  @Bean
  public UserInformationService userInformationService(UserRepository userRepository) {
    return new BasicUserInformationService(userRepository);
  }

  @Bean
  public UserPasswordService userPasswordService(UserRepository userRepository) {
    return new BasicUserInformationService(userRepository);
  }

  @Bean
  public WhenUserRegisteredSendConfirmationEmail whenUserRegisteredSendConfirmationEmail(
      EmailService emailService, JobScheduler jobScheduler, UserRepository userRepository,
      EmailConfirmationLinkSupplier emailConfirmationLinkSupplier) {

    return new WhenUserRegisteredSendConfirmationEmail(emailService, jobScheduler, userRepository,
        emailConfirmationLinkSupplier);
  }

  @Bean
  public WhenUserRegisteredSubmitIntegrationEvent whenUserRegisteredSubmitIntegrationEvent(
      JobScheduler jobScheduler, EventHub eventHub) {
    return new WhenUserRegisteredSubmitIntegrationEvent(eventHub, jobScheduler);
  }

  @Bean
  public WhenUserActivatedSubmitIntegrationEvent whenUserActivatedSubmitIntegrationEvent(
      JobScheduler jobScheduler, EventHub eventHub) {
    return new WhenUserActivatedSubmitIntegrationEvent(eventHub, jobScheduler);
  }

  @Bean
  public UserRegisteredPolicy userRegisteredPolicy(
      WhenUserRegisteredSendConfirmationEmail whenUserRegisteredSendConfirmationEmail,
      WhenUserRegisteredSubmitIntegrationEvent whenUserRegisteredSubmitIntegrationEvent,
      WhenUserActivatedSubmitIntegrationEvent whenUserActivatedSubmitIntegrationEvent) {
    return new UserRegisteredPolicy(whenUserRegisteredSendConfirmationEmail,
        whenUserRegisteredSubmitIntegrationEvent, whenUserActivatedSubmitIntegrationEvent);
  }

  /**
   * Creates the user repository instance.
   *
   * @param userDataStorage an implementation of the {@link UserDataStorage} interface
   * @return a Singleton of the user repository
   * @since 1.0.0
   */
  @Bean
  public UserRepository userRepository(UserDataStorage userDataStorage) {
    return UserRepository.getInstance(userDataStorage);
  }
  /*
  Section ends

  Wiring up identity application core and policies
   */

  /*
  Wiring up project management application core and policies

  Section starts below
  */
  @Bean
  public BatchRegisteredPolicy batchRegisteredPolicy(
      life.qbic.projectmanagement.application.communication.EmailService emailService,
      ProjectAccessService accessService, ProjectInformationService projectInformationService,
      UserInformationService userInformationService, AppContextProvider appContextProvider,
      JobScheduler jobScheduler) {
    var informUsers = new InformUsersAboutBatchRegistration(emailService, accessService,
        userInformationService, appContextProvider, jobScheduler);
    var updateProject = new UpdateProjectUponBatchCreation(projectInformationService, jobScheduler);
    return new BatchRegisteredPolicy(informUsers, updateProject);
  }

  @Bean
  public BatchUpdatedPolicy batchUpdatedPolicy(
      ProjectInformationService projectInformationService, JobScheduler jobScheduler) {
    var updateProject = new UpdateProjectUponBatchUpdate(projectInformationService, jobScheduler);
    return new BatchUpdatedPolicy(updateProject);
  }

  @Bean
  public ProjectAccessGrantedPolicy projectAccessGrantedPolicy(
      life.qbic.projectmanagement.application.communication.EmailService emailService,
      JobScheduler jobScheduler, UserInformationService userInformationService,
      AppContextProvider appContextProvider,
      ProjectInformationService projectInformationService) {
    var informUserAboutGrantedAccess = new InformUserAboutGrantedAccess(emailService,
        jobScheduler,
        userInformationService,
        projectInformationService,
        appContextProvider);
    return new ProjectAccessGrantedPolicy(informUserAboutGrantedAccess);
  }

  @Bean
  public ProjectRegisteredPolicy projectRegisteredPolicy(SampleCodeService sampleCodeService,
      JobScheduler jobScheduler, ProjectRepository projectRepository) {
    var createNewSampleStatisticsEntry = new CreateNewSampleStatisticsEntry(sampleCodeService,
        jobScheduler,
        projectRepository);
    return new ProjectRegisteredPolicy(createNewSampleStatisticsEntry);
  }

  @Bean
  public SampleRegisteredPolicy sampleRegisteredPolicy(
      BatchRegistrationService batchRegistrationService,
      SampleInformationService sampleInformationService,
      ExperimentInformationService experimentInformationService,
      ProjectInformationService projectInformationService, JobScheduler jobScheduler) {
    var addSampleToBatch = new AddSampleToBatch(batchRegistrationService, jobScheduler);
    var updateProject = new UpdateProjectUponSampleCreation(sampleInformationService,
        experimentInformationService, projectInformationService, jobScheduler);
    return new SampleRegisteredPolicy(addSampleToBatch, updateProject);
  }

  @Bean
  public SampleDeletedPolicy sampleDeletedPolicy(BatchRegistrationService batchRegistrationService,
      JobScheduler jobScheduler) {
    var deleteSampleFromBatch = new DeleteSampleFromBatch(batchRegistrationService, jobScheduler);
    return new SampleDeletedPolicy(deleteSampleFromBatch);
  }

  @Bean
  public ProjectChangedPolicy projectChangedPolicy(ProjectInformationService projectInformationService,
      JobScheduler jobScheduler) {
    var updateProject = new UpdateProjectUponDeletionEvent(projectInformationService, jobScheduler);
    return new ProjectChangedPolicy(updateProject);
  }

  @Bean
  public UserActivated userEmailConfirmedIntegration(JobScheduler jobScheduler,
      AuthorityService authorityService, MessageRouter messageRouter) {
    UserActivated userActivated = new UserActivated(jobScheduler, authorityService);
    messageRouter.register(userActivated);
    return userActivated;
  }

  @Bean
  public MeasurementCreatedPolicy measurementCreatedPolicy(
      MeasurementLookupService measurementLookupService,
      ProjectInformationService projectInformationService, JobScheduler jobScheduler) {
    var updateProjectUponMeasurementCreation = new UpdateProjectUponMeasurementCreation(
        measurementLookupService, projectInformationService, jobScheduler);
    return new MeasurementCreatedPolicy(updateProjectUponMeasurementCreation);
  }

  @Bean
  public MeasurementUpdatedPolicy measurementUpdatedPolicy(
      MeasurementLookupService measurementLookupService,
      ProjectInformationService projectInformationService, JobScheduler jobScheduler) {
    var updateProjectUponMeasurementUpdate = new UpdateProjectUponMeasurementUpdate(
        measurementLookupService, projectInformationService, jobScheduler);
    return new MeasurementUpdatedPolicy(updateProjectUponMeasurementUpdate);
  }

  @Bean
  public ExperimentCreatedPolicy experimentCreatedPolicy(
      ProjectInformationService projectInformationService,
      ExperimentInformationService experimentInformationService, JobScheduler jobScheduler) {
    var updateProjectUponExperimentCreation = new UpdateProjectUponExperimentCreation(
        projectInformationService, experimentInformationService, jobScheduler);
    return new ExperimentCreatedPolicy(updateProjectUponExperimentCreation);
  }

  @Bean
  public ExperimentUpdatedPolicy experimentUpdatedPolicy(
      ProjectInformationService projectInformationService,
      ExperimentInformationService experimentInformationService, JobScheduler jobScheduler) {
    var updateProjectUponExperimentUpdate = new UpdateProjectUponExperimentUpdate(
        projectInformationService, experimentInformationService, jobScheduler);
    return new ExperimentUpdatedPolicy(updateProjectUponExperimentUpdate);
  }

  @Bean
  public QCAddedPolicy qcAddedPolicy(
      QualityControlService qualityControlService,
      ProjectInformationService projectInformationService, JobScheduler jobScheduler) {
    var updateProjectUponQCChange = new UpdateProjectUponQCCreation(
        qualityControlService, projectInformationService, jobScheduler);
    return new QCAddedPolicy(updateProjectUponQCChange);
  }

  @Bean
  public OfferAddedPolicy offerAddedPolicy(
      ProjectPurchaseService projectPurchaseService,
      ProjectInformationService projectInformationService, JobScheduler jobScheduler) {
    var updateProjectUponOfferChange = new UpdateProjectUponPurchaseCreation(
        projectPurchaseService, projectInformationService, jobScheduler);
    return new OfferAddedPolicy(updateProjectUponOfferChange);
  }

  /*
  Section ends

  Wiring up project management application core and policies
  */

  /*
  Infrastructure wiring and setup

  Section starts below
   */
  @Bean
  public EmailService identityEmailService(EmailServiceProvider emailServiceProvider) {
    return new IdentityEmailServiceProvider(emailServiceProvider);
  }

  @Bean
  public EmailServiceProvider emailProvider(@Value("${spring.mail.host}") String host,
      @Value("${spring.mail.port}") int port, @Value("${spring.mail.username}") String mailUserName,
      @Value("${spring.mail.password}") String mailUserPassword) {
    var mailServerConfiguration = new life.qbic.infrastructure.email.MailServerConfiguration(
        host, port,
        mailUserName, mailUserPassword);
    return new EmailServiceProvider(mailServerConfiguration);
  }

  @Bean
  public life.qbic.projectmanagement.application.communication.EmailService projectEmailService(
      EmailServiceProvider emailServiceProvider) {
    return new ProjectManagementEmailServiceProvider(emailServiceProvider);
  }

  @Bean
  public SimpleEventStore eventStore() {
    return SimpleEventStore.instance(new TemporaryEventRepository());
  }

  @Bean
  public NotificationService notificationService(MessageBusSubmission messageBusInterface) {
    return new NotificationService(messageBusInterface);
  }

  @Bean
  public MessageBusSubmission messageBusInterface() {
    return Exchange.instance();
  }

   /*
   Section ends

   Infrastructure wiring and setup
   */
}
