package life.qbic.datamanager;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.List;
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
import life.qbic.identity.application.user.registration.EmailAddressConfirmation;
import life.qbic.identity.application.user.registration.RegisterUserInput;
import life.qbic.identity.application.user.registration.Registration;
import life.qbic.identity.domain.repository.UserDataStorage;
import life.qbic.identity.domain.repository.UserRepository;
import life.qbic.infrastructure.email.EmailServiceProvider;
import life.qbic.infrastructure.email.identity.IdentityEmailServiceProvider;
import life.qbic.infrastructure.email.project.ProjectManagementEmailServiceProvider;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.AppContextProvider;
import life.qbic.projectmanagement.application.api.SampleCodeService;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService;
import life.qbic.projectmanagement.application.authorization.authorities.AuthorityService;
import life.qbic.projectmanagement.application.batch.BatchRegistrationService;
import life.qbic.projectmanagement.application.communication.broadcasting.MessageRouter;
import life.qbic.projectmanagement.application.policy.BatchRegisteredPolicy;
import life.qbic.projectmanagement.application.policy.ProjectAccessGrantedPolicy;
import life.qbic.projectmanagement.application.policy.ProjectRegisteredPolicy;
import life.qbic.projectmanagement.application.policy.SampleRegisteredPolicy;
import life.qbic.projectmanagement.application.policy.directive.AddSampleToBatch;
import life.qbic.projectmanagement.application.policy.directive.CreateNewSampleStatisticsEntry;
import life.qbic.projectmanagement.application.policy.directive.InformUserAboutGrantedAccess;
import life.qbic.projectmanagement.application.policy.directive.InformUsersAboutBatchRegistration;
import life.qbic.projectmanagement.application.policy.integration.UserRegistered;
import life.qbic.projectmanagement.domain.model.experiment.repository.ExperimentalDesignVocabularyRepository;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Species;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Specimen;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectCode;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;
import life.qbic.projectmanagement.domain.repository.ProjectRepository;
import life.qbic.projectmanagement.infrastructure.project.QbicProjectDataRepo;
import life.qbic.projectmanagement.infrastructure.sample.QbicSampleDataRepo;
import life.qbic.projectmanagement.infrastructure.sample.openbis.OpenbisConnector;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

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

  private static final Logger log = logger(AppConfig.class);
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
  public UserRegisteredPolicy userRegisteredPolicy(EmailService emailService,
      JobScheduler jobScheduler, UserRepository userRepository,
      EmailConfirmationLinkSupplier emailConfirmationLinkSupplier, EventHub eventHub) {
    return new UserRegisteredPolicy(emailService, jobScheduler, userRepository,
        emailConfirmationLinkSupplier, eventHub);
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
  public UserRegistered userRegisteredIntegration(
      JobScheduler jobScheduler, AuthorityService authorityService,
      MessageRouter messageRouter) {
    UserRegistered userRegistered = new UserRegistered(jobScheduler, authorityService);
    messageRouter.register(userRegistered);
    return userRegistered;
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


  @Bean(name = "experimentalDesignVocabularyRespository")
  public ExperimentalDesignVocabularyRepository experimentalDesignVocabularyRepository(
      @Value("${openbis.user.name}") String userName,
      @Value("${openbis.user.password}") String password,
      @Value("${openbis.datasource.url}") String url) {
    try {
      return new OpenbisConnector(userName, password, url);
    } catch (RuntimeException runtimeException) {
      log.error(runtimeException.getMessage(), runtimeException);
      return new ExperimentalDesignVocabularyRepository() {
        @Override
        public List<Specimen> retrieveSpecimens() {
          return List.of(Specimen.create("specimen 1"), Specimen.create("Specimen 2"),
              Specimen.create("Specimen 3"));
        }

        @Override
        public List<Analyte> retrieveAnalytes() {
          return List.of(Analyte.create("Analyte 1"), Analyte.create("Analyte 2"),
              Analyte.create("Analyte 3"));
        }

        @Override
        public List<Species> retrieveSpecies() {
          return List.of(Species.create("Species 1"), Species.create("Species 2"),
              Species.create("Species 3"));
        }
      };
    }
  }

  @Bean(name = "qbicProjectDataRepo")
  @Primary
  public QbicProjectDataRepo qbicProjectDataRepo(
      @Value("${openbis.user.name}") String userName,
      @Value("${openbis.user.password}") String password,
      @Value("${openbis.datasource.url}") String url) {
    try {
      return (QbicProjectDataRepo) new OpenbisConnector(userName, password, url);
    } catch (RuntimeException runtimeException) {
      log.error(runtimeException.getMessage(), runtimeException);
      return new QbicProjectDataRepo() {
        @Override
        public void add(Project project) {

        }

        @Override
        public void delete(ProjectCode projectCode) {

        }

        @Override
        public boolean projectExists(ProjectCode projectCode) {
          return false;
        }
      };
    }
  }

  @Bean(name = "qbicSampleDataRepo")
  public QbicSampleDataRepo qbicSampleDataRepo(
      @Value("${openbis.user.name}") String userName,
      @Value("${openbis.user.password}") String password,
      @Value("${openbis.datasource.url}") String url) {
    try {
      return new OpenbisConnector(userName, password, url);
    } catch (RuntimeException runtimeException) {
      log.error(runtimeException.getMessage(), runtimeException);
      return new QbicSampleDataRepo() {
        @Override
        public void addSamplesToProject(Project project, List<Sample> samples) {

        }

        @Override
        public void delete(SampleCode sampleCode) {

        }

        @Override
        public boolean sampleExists(SampleCode sampleCode) {
          return false;
        }
      };
    }
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
