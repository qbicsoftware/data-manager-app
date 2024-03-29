package life.qbic.datamanager;

import life.qbic.broadcasting.Exchange;
import life.qbic.broadcasting.MessageBusSubmission;
import life.qbic.domain.concepts.SimpleEventStore;
import life.qbic.domain.concepts.TemporaryEventRepository;
import life.qbic.identity.api.UserInformationService;
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
import life.qbic.identity.application.user.registration.EmailAddressConfirmation;
import life.qbic.identity.application.user.registration.RegisterUserInput;
import life.qbic.identity.application.user.registration.Registration;
import life.qbic.identity.domain.repository.UserDataStorage;
import life.qbic.identity.domain.repository.UserRepository;
import life.qbic.infrastructure.email.EmailServiceProvider;
import life.qbic.infrastructure.email.identity.IdentityEmailServiceProvider;
import life.qbic.infrastructure.email.project.ProjectManagementEmailServiceProvider;
import life.qbic.projectmanagement.application.AppContextProvider;
import life.qbic.projectmanagement.application.api.SampleCodeService;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService;
import life.qbic.projectmanagement.application.authorization.authorities.AuthorityService;
import life.qbic.projectmanagement.application.batch.BatchRegistrationService;
import life.qbic.projectmanagement.application.communication.broadcasting.MessageRouter;
import life.qbic.projectmanagement.application.policy.BatchRegisteredPolicy;
import life.qbic.projectmanagement.application.policy.ProjectAccessGrantedPolicy;
import life.qbic.projectmanagement.application.policy.ProjectRegisteredPolicy;
import life.qbic.projectmanagement.application.policy.SampleDeletedPolicy;
import life.qbic.projectmanagement.application.policy.SampleRegisteredPolicy;
import life.qbic.projectmanagement.application.policy.directive.AddSampleToBatch;
import life.qbic.projectmanagement.application.policy.directive.CreateNewSampleStatisticsEntry;
import life.qbic.projectmanagement.application.policy.directive.DeleteSampleFromBatch;
import life.qbic.projectmanagement.application.policy.directive.InformUserAboutGrantedAccess;
import life.qbic.projectmanagement.application.policy.directive.InformUsersAboutBatchRegistration;
import life.qbic.projectmanagement.application.policy.integration.UserActivated;
import life.qbic.projectmanagement.domain.repository.ProjectRepository;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * <b>App bean configuration class</b>
 *
 * <p>Not all components can be generated on the fly by Spring, some we have to call explicitly via
 * factory methods.
 *
 * @since 1.0.0
 */
@Configuration
@ComponentScan({"life.qbic.identity.infrastructure"})
public class AppConfig {

  /*
  Wiring up identity application core and policies

  Section starts below
  */
  @Bean
  public EmailAddressConfirmation confirmEmailInput(
      IdentityService identityService) {
    return new EmailAddressConfirmation(identityService);
  }

  @Bean
  public IdentityService userRegistrationService(
      UserRepository userRepository
  ) {
    return new IdentityService(userRepository);
  }

  @Bean
  public NewPasswordInput newPasswordInput(IdentityService identityService) {
    return new NewPassword(identityService);
  }

  @Bean
  public PasswordResetInput passwordResetInput(IdentityService identityService) {
    return new PasswordResetRequest(identityService);
  }

  /**
   * Creates the registration use case.
   *
   * @param identityService the user registration services used by this use case
   * @return the use case input
   * @since 1.0.0
   */
  @Bean
  public RegisterUserInput registerUserInput(IdentityService identityService) {
    return new Registration(identityService);
  }

  @Bean
  public UserInformationService userInformationService(UserRepository userRepository) {
    return new BasicUserInformationService(userRepository);
  }

  @Bean
  public WhenUserRegisteredSendConfirmationEmail whenUserRegisteredSendConfirmationEmail(
      EmailService emailService, JobScheduler jobScheduler, UserRepository userRepository,
      EmailConfirmationLinkSupplier emailConfirmationLinkSupplier
  ) {

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
      ProjectAccessService accessService,
      UserInformationService userInformationService, AppContextProvider appContextProvider,
      JobScheduler jobScheduler) {
    var informUsers = new InformUsersAboutBatchRegistration(emailService, accessService,
        userInformationService, appContextProvider, jobScheduler);
    return new BatchRegisteredPolicy(informUsers);
  }

  @Bean
  public ProjectAccessGrantedPolicy projectAccessGrantedPolicy(
      life.qbic.projectmanagement.application.communication.EmailService emailService,
      JobScheduler jobScheduler, UserInformationService userInformationService,
      AppContextProvider appContextProvider) {
    var informUserAboutGrantedAccess = new InformUserAboutGrantedAccess(emailService,
        jobScheduler,
        userInformationService, appContextProvider);
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
      BatchRegistrationService batchRegistrationService, JobScheduler jobScheduler) {
    var addSampleToBatch = new AddSampleToBatch(batchRegistrationService, jobScheduler);
    return new SampleRegisteredPolicy(addSampleToBatch);
  }

  @Bean
  public SampleDeletedPolicy sampleDeletedPolicy(BatchRegistrationService batchRegistrationService,
      JobScheduler jobScheduler) {
    var deleteSampleFromBatch = new DeleteSampleFromBatch(batchRegistrationService, jobScheduler);
    return new SampleDeletedPolicy(deleteSampleFromBatch);
  }

  @Bean
  public UserActivated userEmailConfirmedIntegration(JobScheduler jobScheduler,
      AuthorityService authorityService, MessageRouter messageRouter) {
    UserActivated userActivated = new UserActivated(jobScheduler, authorityService);
    messageRouter.register(userActivated);
    return userActivated;
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
