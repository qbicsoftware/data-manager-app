package life.qbic.datamanager;

import life.qbic.authentication.application.notification.NotificationService;
import life.qbic.authentication.application.service.SimpleUserInformationService;
import life.qbic.authentication.application.user.password.NewPassword;
import life.qbic.authentication.application.user.password.NewPasswordInput;
import life.qbic.authentication.application.user.password.PasswordResetInput;
import life.qbic.authentication.application.user.password.PasswordResetRequest;
import life.qbic.authentication.application.user.registration.EmailAddressConfirmation;
import life.qbic.authentication.application.user.registration.RegisterUserInput;
import life.qbic.authentication.application.user.registration.Registration;
import life.qbic.authentication.application.user.registration.UserRegistrationService;
import life.qbic.authentication.domain.user.repository.UserDataStorage;
import life.qbic.authentication.domain.user.repository.UserRepository;
import life.qbic.projectmanagement.application.AppContextProvider;
import life.qbic.projectmanagement.application.policy.ProjectAccessGrantedPolicy;
import life.qbic.projectmanagement.application.policy.directive.InformUserAboutGrantedAccess;
import life.qbic.broadcasting.Exchange;
import life.qbic.broadcasting.MessageBusSubmission;
import life.qbic.domain.concepts.SimpleEventStore;
import life.qbic.domain.concepts.TemporaryEventRepository;
import life.qbic.domain.concepts.communication.CommunicationService;
import life.qbic.newshandler.usermanagement.email.EmailCommunicationService;
import life.qbic.newshandler.usermanagement.email.MailServerConfiguration;
import life.qbic.projectmanagement.application.api.SampleCodeService;
import life.qbic.projectmanagement.application.batch.BatchRegistrationService;
import life.qbic.projectmanagement.application.policy.ProjectRegisteredPolicy;
import life.qbic.projectmanagement.application.policy.SampleRegisteredPolicy;
import life.qbic.projectmanagement.application.policy.directive.AddSampleToBatch;
import life.qbic.projectmanagement.application.policy.directive.CreateNewSampleStatisticsEntry;
import life.qbic.projectmanagement.domain.project.repository.ProjectRepository;
import life.qbic.user.api.UserInformationService;
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
@ComponentScan({"life.qbic.authentication.persistence"})
public class AppConfig {

  /**
   * Creates the registration use case.
   *
   * @param userRegistrationService the user registration services used by this use case
   * @return the use case input
   * @since 1.0.0
   */
  @Bean
  public RegisterUserInput registerUserInput(UserRegistrationService userRegistrationService) {
    return new Registration(userRegistrationService);
  }

  @Bean
  public EmailAddressConfirmation confirmEmailInput(
      UserRegistrationService userRegistrationService) {
    return new EmailAddressConfirmation(userRegistrationService);
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

  @Bean
  public UserRegistrationService userRegistrationService(
      UserRepository userRepository
  ) {
    return new UserRegistrationService(userRepository);
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

  @Bean
  public PasswordResetInput passwordResetInput(UserRegistrationService userRegistrationService) {
    return new PasswordResetRequest(userRegistrationService);
  }

  @Bean
  public NewPasswordInput newPasswordInput(UserRegistrationService userRegistrationService) {
    return new NewPassword(userRegistrationService);
  }

  @Bean
  public SampleRegisteredPolicy sampleRegisteredPolicy(
      BatchRegistrationService batchRegistrationService, JobScheduler jobScheduler) {
    var addSampleToBatch = new AddSampleToBatch(batchRegistrationService, jobScheduler);
    return new SampleRegisteredPolicy(addSampleToBatch);
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
  public UserInformationService userInformationService(UserRepository userRepository) {
    return new SimpleUserInformationService(userRepository);
  }

  @Bean
  public ProjectAccessGrantedPolicy projectAccessGrantedPolicy(CommunicationService communicationService,
      JobScheduler jobScheduler, UserInformationService userInformationService,
      AppContextProvider appContextProvider) {
    var informUserAboutGrantedAccess = new InformUserAboutGrantedAccess(communicationService, jobScheduler,
        userInformationService, appContextProvider);
    return new ProjectAccessGrantedPolicy(informUserAboutGrantedAccess);
  }

  @Bean
  public CommunicationService communicationService(@Value("${spring.mail.host}") String host,
      @Value("${spring.mail.port}") int port, @Value("${spring.mail.username}") String mailUserName,
      @Value("${spring.mail.password}") String mailUserPassword) {
    MailServerConfiguration mailServerConfiguration = new MailServerConfiguration(host, port,
        mailUserName, mailUserPassword);
    return new EmailCommunicationService(mailServerConfiguration);
  }
}
