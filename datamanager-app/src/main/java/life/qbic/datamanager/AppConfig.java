package life.qbic.datamanager;

import java.util.Objects;
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
import life.qbic.identity.domain.repository.UserDataStorage;
import life.qbic.identity.domain.repository.UserRepository;
import life.qbic.usergroups.api.GroupInformationService;
import life.qbic.usergroups.api.GroupManagementService;
import life.qbic.usergroups.api.GroupSidProvider;
import life.qbic.usergroups.application.GroupService;
import life.qbic.usergroups.application.policy.MemberAccessPolicy;
import life.qbic.usergroups.application.policy.directive.InformAddedGroupMember;
import life.qbic.usergroups.application.policy.directive.InformRemovedGroupMember;
import life.qbic.usergroups.application.service.GroupInformationServiceImpl;
import life.qbic.usergroups.application.service.GroupManagementServiceImpl;
import life.qbic.usergroups.application.service.GroupSidProviderImpl;
import life.qbic.usergroups.domain.repository.GroupDataStorage;
import life.qbic.usergroups.domain.repository.GroupRepository;
import life.qbic.infrastructure.email.EmailServiceProvider;
import life.qbic.infrastructure.email.identity.IdentityEmailServiceProvider;
import life.qbic.infrastructure.email.project.ProjectManagementEmailServiceProvider;
import life.qbic.infrastructure.email.usergroups.UserGroupsEmailServiceProvider;
import life.qbic.projectmanagement.application.AppContextProvider;
import life.qbic.projectmanagement.application.OrganisationRepository;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.api.SampleCodeService;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService;
import life.qbic.projectmanagement.application.authorization.authorities.AuthorityService;
import life.qbic.projectmanagement.application.communication.broadcasting.MessageRouter;
import life.qbic.projectmanagement.application.concurrent.ElasticScheduler;
import life.qbic.projectmanagement.application.concurrent.VirtualThreadScheduler;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService;
import life.qbic.projectmanagement.application.measurement.MeasurementLookupService;
import life.qbic.projectmanagement.application.policy.AssociatedDatasetConnectedPolicy;
import life.qbic.projectmanagement.application.policy.AssociatedDatasetRemovedPolicy;
import life.qbic.projectmanagement.application.policy.AssociatedDatasetsSyncedPolicy;
import life.qbic.projectmanagement.application.policy.ExperimentCreatedPolicy;
import life.qbic.projectmanagement.application.policy.ExperimentUpdatedPolicy;
import life.qbic.projectmanagement.application.policy.MeasurementCreatedPolicy;
import life.qbic.projectmanagement.application.policy.MeasurementUpdatedPolicy;
import life.qbic.projectmanagement.application.policy.OfferAddedPolicy;
import life.qbic.projectmanagement.application.policy.ProjectAccessGrantedPolicy;
import life.qbic.projectmanagement.application.policy.ProjectChangedPolicy;
import life.qbic.projectmanagement.application.policy.ProjectRegisteredPolicy;
import life.qbic.projectmanagement.application.policy.QCAddedPolicy;
import life.qbic.projectmanagement.application.policy.SampleRegisteredPolicy;
import life.qbic.projectmanagement.application.policy.directive.CreateNewSampleStatisticsEntry;
import life.qbic.projectmanagement.application.policy.directive.InformProjectCollaboratorsAboutDatasetConnection;
import life.qbic.projectmanagement.application.policy.directive.InformProjectCollaboratorsAboutDatasetRemoval;
import life.qbic.projectmanagement.application.policy.directive.InformProjectCollaboratorsAboutDatasetSync;
import life.qbic.projectmanagement.application.policy.directive.InformUserAboutGrantedAccess;
import life.qbic.projectmanagement.application.policy.directive.UpdateProjectUponDeletionEvent;
import life.qbic.projectmanagement.application.policy.directive.UpdateProjectUponExperimentCreation;
import life.qbic.projectmanagement.application.policy.directive.UpdateProjectUponExperimentUpdate;
import life.qbic.projectmanagement.application.policy.directive.UpdateProjectUponMeasurementCreation;
import life.qbic.projectmanagement.application.policy.directive.UpdateProjectUponMeasurementUpdate;
import life.qbic.projectmanagement.application.policy.directive.UpdateProjectUponPurchaseCreation;
import life.qbic.projectmanagement.application.policy.directive.UpdateProjectUponQCCreation;
import life.qbic.projectmanagement.application.policy.directive.UpdateProjectUponSampleCreation;
import life.qbic.projectmanagement.application.policy.integration.UserActivated;
import life.qbic.projectmanagement.application.purchase.ProjectPurchaseService;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.application.sample.qualitycontrol.QualityControlService;
import life.qbic.projectmanagement.domain.repository.ProjectRepository;
import life.qbic.projectmanagement.infrastructure.organisations.CachedOrganisationRepository;
import life.qbic.projectmanagement.infrastructure.organisations.RorApi;
import life.qbic.projectmanagement.infrastructure.organisations.RorApi.RorApiV2;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
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
@ComponentScan({"life.qbic.identity.infrastructure", "life.qbic.usergroups.infrastructure",
    "life.qbic.datamanager.announcements"})
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
  @Qualifier("elasticScheduler")
  public Scheduler elasticScheduler() {
    return ElasticScheduler.elasticScheduler();
  }


  @Bean
  public IdentityService userRegistrationService(
      UserRepository userRepository
  ) {
    return new IdentityService(userRepository);
  }

  @Bean
  RorApi rorApi(
      @Value("${qbic.external-service.organisation-search.ror.client-id}") String clientId,
      @Value("${qbic.external-service.organisation-search.ror.organisation-api-endpoint}") String rorOrganisationEndpoint) {
    Objects.requireNonNull(rorOrganisationEndpoint);
    Objects.requireNonNull(clientId);
    return new RorApiV2(rorOrganisationEndpoint, clientId);
  }

  @Bean
  public OrganisationRepository organisationRepository(
      RorApi rorApi) {
    Objects.requireNonNull(rorApi);
    return new CachedOrganisationRepository(rorApi);
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
  public BasicUserInformationService userInformationService(UserRepository userRepository) {
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

  /**
   * Creates the group repository instance.
   *
   * @param groupDataStorage an implementation of the {@link GroupDataStorage} interface
   * @return a Singleton of the group repository
   * @since 1.0.0
   */
  @Bean
  public GroupRepository groupRepository(GroupDataStorage groupDataStorage) {
    return GroupRepository.getInstance(groupDataStorage);
  }

  @Bean
  public GroupService groupService(GroupRepository groupRepository,
      UserInformationService userInformationService) {
    return new GroupService(groupRepository, userInformationService);
  }

  @Bean
  public GroupInformationServiceImpl groupInformationService(GroupService groupService) {
    return new GroupInformationServiceImpl(groupService);
  }

  @Bean
  public GroupManagementServiceImpl groupManagementService(GroupService groupService) {
    return new GroupManagementServiceImpl(groupService);
  }

  @Bean
  public GroupSidProviderImpl groupSidProvider(GroupService groupService) {
    return new GroupSidProviderImpl(groupService);
  }

  /**
   * The user groups email provider, implementing the context's {@link EmailService} port with
   * the shared mail infrastructure.
   */
  @Bean
  public life.qbic.usergroups.application.communication.EmailService userGroupsEmailService(
      EmailServiceProvider emailServiceProvider) {
    return new UserGroupsEmailServiceProvider(emailServiceProvider);
  }

  /**
   * Registers the user-groups membership notification directives with the domain dispatcher: a
   * newly added member and a removed member each receive an email.
   */
  @Bean
  public MemberAccessPolicy memberAccessPolicy(
      life.qbic.usergroups.application.communication.EmailService emailService,
      JobScheduler jobScheduler, UserInformationService userInformationService,
      GroupService groupService) {
    var informAdded = new InformAddedGroupMember(emailService, jobScheduler,
        userInformationService, groupService);
    var informRemoved = new InformRemovedGroupMember(emailService, jobScheduler,
        userInformationService, groupService);
    return new MemberAccessPolicy(informAdded, informRemoved);
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
      SampleInformationService sampleInformationService,
      ExperimentInformationService experimentInformationService,
      ProjectInformationService projectInformationService, JobScheduler jobScheduler) {
    var updateProject = new UpdateProjectUponSampleCreation(sampleInformationService,
        experimentInformationService, projectInformationService, jobScheduler);
    return new SampleRegisteredPolicy(updateProject);
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
      ProjectInformationService projectInformationService, JobScheduler jobScheduler) {
    var updateProjectUponMeasurementUpdate = new UpdateProjectUponMeasurementUpdate(
        projectInformationService,
        jobScheduler);
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

  @Bean
  public AssociatedDatasetConnectedPolicy associatedDatasetConnectedPolicy(
      life.qbic.projectmanagement.application.communication.EmailService emailService,
      ProjectAccessService projectAccessService,
      UserInformationService userInformationService,
      ProjectInformationService projectInformationService,
      AppContextProvider appContextProvider,
      JobScheduler jobScheduler) {
    var informCollaborators = new InformProjectCollaboratorsAboutDatasetConnection(
        emailService, projectAccessService, userInformationService, projectInformationService,
        appContextProvider, jobScheduler);
    return new AssociatedDatasetConnectedPolicy(informCollaborators);
  }

  @Bean
  public AssociatedDatasetRemovedPolicy associatedDatasetRemovedPolicy(
      life.qbic.projectmanagement.application.communication.EmailService emailService,
      ProjectAccessService projectAccessService,
      UserInformationService userInformationService,
      ProjectInformationService projectInformationService,
      AppContextProvider appContextProvider,
      JobScheduler jobScheduler) {
    var informCollaborators = new InformProjectCollaboratorsAboutDatasetRemoval(
        emailService, projectAccessService, userInformationService,
        projectInformationService, appContextProvider, jobScheduler);
    return new AssociatedDatasetRemovedPolicy(informCollaborators);
  }

  @Bean
  public AssociatedDatasetsSyncedPolicy associatedDatasetsSyncedPolicy(
      life.qbic.projectmanagement.application.communication.EmailService emailService,
      ProjectAccessService projectAccessService,
      UserInformationService userInformationService,
      ProjectInformationService projectInformationService,
      AppContextProvider appContextProvider,
      JobScheduler jobScheduler) {
    var informCollaborators = new InformProjectCollaboratorsAboutDatasetSync(
        emailService, projectAccessService, userInformationService,
        projectInformationService, appContextProvider, jobScheduler);
    return new AssociatedDatasetsSyncedPolicy(informCollaborators);
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
  public EmailServiceProvider emailProvider(JavaMailSender mailSender) {
    return new EmailServiceProvider(mailSender);
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
