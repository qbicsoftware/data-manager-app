package life.qbic.datamanager.views.projects.project.info;

import static java.util.Objects.requireNonNull;
import static life.qbic.datamanager.views.MeasurementType.GENOMICS;
import static life.qbic.datamanager.views.MeasurementType.IMMUNOPEPTIDOMICS;
import static life.qbic.datamanager.views.MeasurementType.PROTEOMICS;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.InputStream;
import java.io.Serial;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.ByteBufferIteratorInputStream;
import life.qbic.application.commons.FileNameFormatter;
import life.qbic.application.commons.time.DateTimeFormat;
import life.qbic.datamanager.RequestCache;
import life.qbic.datamanager.RequestCache.CacheException;
import life.qbic.datamanager.files.export.download.DownloadStreamProvider;
import life.qbic.datamanager.security.UserPermissions;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.TagFactory;
import life.qbic.datamanager.views.account.UserAvatar.UserAvatarGroupItem;
import life.qbic.datamanager.views.general.CollapsibleDetails;
import life.qbic.datamanager.views.general.DetailBox;
import life.qbic.datamanager.views.general.DetailBox.Header;
import life.qbic.datamanager.views.general.Heading;
import life.qbic.datamanager.views.general.IconLabel;
import life.qbic.datamanager.views.general.OntologyTermDisplay;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.general.Tag;
import life.qbic.datamanager.views.general.dialog.AppDialog;
import life.qbic.datamanager.views.general.dialog.DialogBody;
import life.qbic.datamanager.views.general.dialog.DialogFooter;
import life.qbic.datamanager.views.general.dialog.DialogHeader;
import life.qbic.datamanager.views.general.dialog.InputValidation;
import life.qbic.datamanager.views.general.dialog.UserInput;
import life.qbic.datamanager.views.general.download.DownloadComponent;
import life.qbic.datamanager.views.general.funding.BoundFundingField;
import life.qbic.datamanager.views.general.funding.FundingEntry;
import life.qbic.datamanager.views.general.funding.FundingField;
import life.qbic.datamanager.views.general.funding.FundingInputForm;
import life.qbic.datamanager.views.general.oidc.OidcLogo;
import life.qbic.datamanager.views.general.oidc.OidcType;
import life.qbic.datamanager.views.general.section.ActionBar;
import life.qbic.datamanager.views.general.section.Section;
import life.qbic.datamanager.views.general.section.Section.SectionBuilder;
import life.qbic.datamanager.views.general.section.SectionContent;
import life.qbic.datamanager.views.general.section.SectionHeader;
import life.qbic.datamanager.views.general.section.SectionNote;
import life.qbic.datamanager.views.general.section.SectionTitle;
import life.qbic.datamanager.views.general.section.SectionTitle.Size;
import life.qbic.datamanager.views.general.utils.Restorable.Snapshot;
import life.qbic.datamanager.views.general.utils.Utility;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.datamanager.views.notifications.Toast;
import life.qbic.datamanager.views.projects.ProjectInformation;
import life.qbic.datamanager.views.projects.edit.EditContactsComponent;
import life.qbic.datamanager.views.projects.edit.ProjectDesignForm;
import life.qbic.datamanager.views.strategy.scope.ReadScopeStrategy;
import life.qbic.datamanager.views.strategy.scope.UserScopeStrategy;
import life.qbic.datamanager.views.strategy.scope.WriteScopeStrategy;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.ProjectOverview;
import life.qbic.projectmanagement.application.ProjectOverview.UserInfo;
import life.qbic.projectmanagement.application.api.AsyncProjectService;
import life.qbic.projectmanagement.application.api.AsyncProjectService.AccessDeniedException;
import life.qbic.projectmanagement.application.api.AsyncProjectService.FundingInformation;
import life.qbic.projectmanagement.application.api.AsyncProjectService.FundingInformationCreationRequest;
import life.qbic.projectmanagement.application.api.AsyncProjectService.FundingInformationCreationResponse;
import life.qbic.projectmanagement.application.api.AsyncProjectService.FundingInformationDeletionRequest;
import life.qbic.projectmanagement.application.api.AsyncProjectService.FundingInformationDeletionResponse;
import life.qbic.projectmanagement.application.api.AsyncProjectService.OntologyTerm;
import life.qbic.projectmanagement.application.api.AsyncProjectService.PrincipalInvestigator;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ProjectContact;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ProjectDeletionRequest;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ProjectDeletionResponse;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ProjectManager;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ProjectResponsible;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ProjectResponsibleDeletionRequest;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ProjectResponsibleDeletionResponse;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ProjectUpdateRequest;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ProjectUpdateResponse;
import life.qbic.projectmanagement.application.api.AsyncProjectService.RequestFailedException;
import life.qbic.projectmanagement.application.api.AsyncProjectService.UnknownRequestException;
import life.qbic.projectmanagement.application.api.ProjectQueryService;
import life.qbic.projectmanagement.application.associated_dataset.AssociatedDatasetService;
import life.qbic.projectmanagement.application.contact.PersonLookupService;
import life.qbic.projectmanagement.application.query.ExperimentProgress;
import life.qbic.projectmanagement.domain.model.project.Contact;
import life.qbic.projectmanagement.domain.model.project.Contact.OidcInformation;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectCode;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;

/**
 * <b>Project Summary Component</b>
 *
 * <p>Gives the user an overview about general project information, such as:</p>
 *
 * <ul>
 *   <li>title</li>
 *   <li>objective</li>
 *   <li>shared with other users</li>
 *   <li>funding</li>
 *   <li>contacts</li>
 * </ul>
 *
 * @since <version tag>
 */
@UIScope
@SpringComponent
public class ProjectSummaryComponent extends PageArea {

  public static final String FIXED_MEDIUM_WIDTH_CSS = "fixed-medium-width";
  public static final String PROJECT_UPDATED_SUCCESS = "project.updated.success";
  public static final String CANCEL_BUTTON_TEXT = "Cancel";
  public static final String SAVE_BUTTON_TEXT = "Save";
  private static final Logger log = logger(ProjectSummaryComponent.class);
  private final transient ProjectInformationService projectInformationService;
  private final transient PersonLookupService personLookupService;
  private final transient UserPermissions userPermissions;
  private final transient MessageSourceNotificationFactory notificationFactory;
  private final Section headerSection;
  private final DownloadComponent downloadComponent;
  private final ProjectProgressSection projectProgressSection;
  private final ProjectExperimentsSection projectExperimentsSection;
  private final Div identityMeta = new Div();
  private transient String identityLastModified = "";
  private final transient AsyncProjectService asyncProjectService;
  private final RequestCache requestCache;
  private final MessageSourceNotificationFactory messageSourceNotificationFactory;
  private final Map<String, Toast> pendingTaskToasts = new HashMap<>();
  private Toast taskInProgressToast;
  private Context context;
  private transient List<? extends UserScopeStrategy> scopes;
  private final Div projectMetadataBand = new Div();

  @Autowired
  public ProjectSummaryComponent(ProjectInformationService projectInformationService,
      PersonLookupService personLookupService,
      UserPermissions userPermissions,
      MessageSourceNotificationFactory notificationFactory,
      AsyncProjectService asyncProjectService,
      ProjectQueryService projectQueryService,
      AssociatedDatasetService associatedDatasetService,
      RequestCache requestCache,
      MessageSourceNotificationFactory messageSourceNotificationFactory) {
    this.projectInformationService = Objects.requireNonNull(projectInformationService);
    this.personLookupService = requireNonNull(personLookupService,
        "person lookup service must not be null");
    this.headerSection = new SectionBuilder().build();
    this.userPermissions = Objects.requireNonNull(userPermissions);
    this.notificationFactory = Objects.requireNonNull(notificationFactory);
    this.asyncProjectService = Objects.requireNonNull(asyncProjectService);
    this.requestCache = Objects.requireNonNull(requestCache);
    this.downloadComponent = new DownloadComponent();
    this.projectProgressSection = new ProjectProgressSection(
        Objects.requireNonNull(projectQueryService), projectInformationService,
        Objects.requireNonNull(associatedDatasetService));
    this.projectExperimentsSection = new ProjectExperimentsSection(
        Objects.requireNonNull(projectQueryService));
    this.projectExperimentsSection.addAddExperimentListener(
        event -> fireEvent(new AddExperimentRequestedEvent(this, event.isFromClient())));
    this.projectProgressSection.addProgressLoadedListener(
        event -> updateIdentityMeta(event.progress()));

    addClassName("project-details-component");

    add(headerSection);
    add(projectProgressSection);
    add(projectExperimentsSection);
    add(projectMetadataBand);
    add(downloadComponent);
    this.messageSourceNotificationFactory = messageSourceNotificationFactory;
  }

  private static ProjectInformation convertToInfo(Project project) {
    var info = new ProjectInformation();

    // General Info
    info.setProjectTitle(project.getProjectIntent().projectTitle().title());
    info.setProjectObjective(project.getProjectIntent().objective().objective());
    info.setProjectId(project.getProjectCode().value());

    // Funding
    project.funding().ifPresent(
        funding -> info.setFundingEntry(new FundingEntry(funding.grant(), funding.grantId())));

    // Contacts
    project.getResponsiblePerson().ifPresent(contact -> info.setResponsiblePerson(
        convert(contact)));
    info.setPrincipalInvestigator(convert(project.getPrincipalInvestigator()));
    info.setProjectManager(convert(project.getProjectManager()));

    return info;
  }

  private static life.qbic.datamanager.views.general.contact.Contact convert(Contact contact) {
    return new life.qbic.datamanager.views.general.contact.Contact(contact.fullName(),
        contact.emailAddress(), contact.oidcInformation().map(OidcInformation::oidcId).orElse(null),
        contact.oidcInformation().map(OidcInformation::oidcIssuer).orElse(null));
  }


  private static List<? extends UserScopeStrategy> loadScope(Predicate<ProjectId> hasWriteScope,
      ProjectId id,
      Section... sections) {
    if (hasWriteScope.test(id)) {
      return loadWriteScope(sections);
    } else {
      return loadReadScope(sections);
    }
  }

  private static List<? extends UserScopeStrategy> loadReadScope(Section[] sections) {
    return Arrays.stream(sections).map(ReadScopeStrategy::new).toList();
  }

  private static List<? extends UserScopeStrategy> loadWriteScope(Section[] sections) {
    return Arrays.stream(sections).map(WriteScopeStrategy::new).toList();
  }

  private static SerializablePredicate<FundingEntry> incompletePredicate() {
    return (FundingEntry entry) ->
        !((entry.getLabel().isBlank() && !entry.getReferenceId().isBlank()) ||
            (!entry.getLabel().isBlank() && entry.getReferenceId().isBlank()));
  }

  private static boolean isEmpty(FundingInformation funding) {
    if (funding == null) {
      return true;
    }
    return funding.grant().isBlank() && funding.grantId().isBlank();
  }

  private static boolean isEmpty(ProjectContact contact) {
    if (contact == null) {
      return true;
    }
    return contact.email().isBlank() && contact.fullName().isBlank();
  }

  private static FundingInformation convertFrom(FundingEntry entry) {
    if (entry == null) {
      return new FundingInformation("", "");
    }
    return new FundingInformation(entry.getLabel(), entry.getReferenceId());
  }

  public void setContext(Context context) {
    this.context = Objects.requireNonNull(context);
    var projectId = context.projectId()
        .orElseThrow(() -> new ApplicationException("No project id provided"));
    var projectOverview = projectInformationService.findOverview(projectId)
        .orElseThrow(() -> new ApplicationException("No project with given ID found"));
    var fullProject = projectInformationService.find(projectId)
        .orElseThrow(() -> new ApplicationException("No project found"));
    setContent(projectOverview, fullProject);
    this.scopes = List.of();
    // The header section only contains the RO-Crate action, which we want to enable always
    loadScope(id -> true, projectId, headerSection).forEach(UserScopeStrategy::execute);
    projectProgressSection.setProjectId(projectId.value());
    projectExperimentsSection.setProjectId(projectId.value());
  }

  /**
   * Delegates the "add experiment" action of the experiments section to the owning page, which runs
   * the creation workflow.
   */
  public Registration addAddExperimentListener(
      ComponentEventListener<AddExperimentRequestedEvent> listener) {
    return addListener(AddExperimentRequestedEvent.class, listener);
  }

  /**
   * Fired when the user asks to create a new experiment, from either the identity band action or the
   * experiments section. The owning page runs the creation workflow.
   */
  public static class AddExperimentRequestedEvent extends
      ComponentEvent<ProjectSummaryComponent> {

    @Serial
    private static final long serialVersionUID = 1L;

    public AddExperimentRequestedEvent(ProjectSummaryComponent source, boolean fromClient) {
      super(source, fromClient);
    }
  }


  private void reloadInformation(Context context) {
    var projectId = context.projectId()
        .orElseThrow(() -> new ApplicationException("No project id provided"));
    var projectOverview = projectInformationService.findOverview(projectId)
        .orElseThrow(() -> new ApplicationException("No project with given ID found"));
    var fullProject = projectInformationService.find(projectId)
        .orElseThrow(() -> new ApplicationException("No project found"));
    buildMetadataBand(projectOverview, fullProject);
    // apply scope strategy again
    this.scopes.forEach(UserScopeStrategy::execute);
  }


  private void setContent(ProjectOverview projectOverview, Project fullProject) {
    Objects.requireNonNull(projectOverview);
    buildHeaderSection(projectOverview, fullProject);
    buildMetadataBand(projectOverview, fullProject);
  }

  private Optional<ProjectContact> toProjectContacts(
      life.qbic.datamanager.views.general.contact.Contact contact) {
    if (contact == null) {
      return Optional.empty();
    }
    return Optional.of(new ProjectContact(contact.fullName(), contact.email(), contact.oidc(),
        contact.oidcIssuer()));
  }

  private void handleSubmission(String projectId, ProjectContact manager,
      ProjectContact investigator, ProjectContact responsible) {
    if (responsible != null) {
      submitMultiple(
          List.of(
              new ProjectUpdateRequest(projectId, new ProjectManager(manager)),
              new ProjectUpdateRequest(projectId,
                  new PrincipalInvestigator(investigator)),
              new ProjectUpdateRequest(projectId,
                  new ProjectResponsible(responsible))));
    } else {
      submitRequest(new ProjectResponsibleDeletionRequest(projectId));
      submitMultiple(
          List.of(
              new ProjectUpdateRequest(projectId, new ProjectManager(manager)),
              new ProjectUpdateRequest(projectId,
                  new PrincipalInvestigator(investigator))));
    }
  }

  /**
   * The contacts row of the identity band: "Contacts", one chip per role, and "Edit contacts".
   * Editing stays in a dialog until the project settings surface exists.
   */
  private Div contactsRow(Project project) {
    Div row = new Div();
    row.addClassName("project-contacts");
    Span label = new Span("Contacts");
    label.addClassName("project-contacts-label");
    row.add(label);
    project.getResponsiblePerson().ifPresent(contact -> row.add(contactChip("Resp.", contact)));
    row.add(contactChip("PM", project.getProjectManager()));
    row.add(contactChip("PI", project.getPrincipalInvestigator()));
    Button edit = new Button("Edit contacts");
    edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    edit.addClassName("project-contacts-edit");
    edit.addClickListener(event -> showEditContactsDialog(project));
    row.add(edit);
    return row;
  }

  private Span contactChip(String role, Contact contact) {
    Span chip = new Span();
    chip.addClassName("project-contact");
    Span roleChip = new Span(role);
    roleChip.addClassName("project-contact-role");
    Span name = new Span(contact.fullName());
    name.addClassName("project-contact-name");
    chip.add(roleChip, name);
    var mail = new Anchor("mailto:" + contact.emailAddress());
    mail.add(new Icon(VaadinIcon.ENVELOPE));
    mail.addClassName("project-contact-action");
    chip.add(mail);
    return chip;
  }

  private void showEditContactsDialog(Project project) {
    var editContacts = new EditContactsComponent(convertToInfo(project),
        Utility.tryToLoadFromPrincipal().orElse(null), personLookupService);
    AppDialog dialog = AppDialog.medium();
    DialogHeader.with(dialog, "Edit Contacts");
    DialogFooter.with(dialog, CANCEL_BUTTON_TEXT, SAVE_BUTTON_TEXT);
    DialogBody.with(dialog, editContacts, editContacts);

    dialog.registerCancelAction(dialog::close);
    dialog.registerConfirmAction(() -> {
      dialog.close();
      var manager = toProjectContacts(
          editContacts.getIfValidManager().orElseThrow()).orElseThrow();
      var responsible = toProjectContacts(
          editContacts.getIfValidProjectResponsible().orElseThrow()).orElseThrow();
      var investigator = toProjectContacts(
          editContacts.getIfValidInvestigator().orElseThrow()).orElseThrow();
      if (isEmpty(responsible)) {
        responsible = null;
      }
      handleSubmission(project.getId().value(), manager, investigator, responsible);
    });
    dialog.open();
  }

  private Div renderContactInfo(Contact contact) {
    var contactInfo = new Div();
    contactInfo.addClassNames("flex-vertical", "width-full");
    var name = new Span(contact.fullName());
    var email = new Anchor("mailto:" + contact.emailAddress(), contact.emailAddress());
    contactInfo.add(name, email);
    //Account for contacts without oidc or oidcissuer set
    if (contact.oidcInformation().isEmpty()) {
      return contactInfo;
    }
    OidcInformation oidcInformation = contact.oidcInformation().orElseThrow();
    var optionalOidcType = Arrays.stream(OidcType.values())
        .filter(ot -> ot.getIssuer().equals(oidcInformation.oidcIssuer()))
        .findFirst();
    if (optionalOidcType.isPresent()) {
      OidcType oidcType = optionalOidcType.orElseThrow();
      String oidcUrl = oidcType.getUrlFor(oidcInformation.oidcId());
      Anchor oidcLink = new Anchor(oidcUrl, oidcInformation.oidcId());
      oidcLink.setTarget(AnchorTarget.BLANK);
      OidcLogo oidcLogo = new OidcLogo(oidcType);
      oidcLogo.addClassName("icon-size-m");
      Span oidcSpan = new Span(oidcLogo, oidcLink);
      oidcSpan.addClassNames("gap-02", "flex-align-items-center", "flex-horizontal");
      contactInfo.add(oidcSpan);
    }
    return contactInfo;
  }


  private void handleSubmission(String projectId, FundingInformation funding) {
    if (isEmpty(funding)) {
      submitRequest(new FundingInformationDeletionRequest(projectId));
      return;
    }
    submitRequest(new FundingInformationCreationRequest(projectId, funding));
  }


  /**
   * Submits multiple {@link ProjectUpdateRequest} to the service.
   *
   * @param requests a {@link Collection} of {@link ProjectUpdateRequest}.
   * @since 1.10.0
   */
  private void submitMultiple(Collection<ProjectUpdateRequest> requests) {
    var requestId = UUID.randomUUID().toString();
    var publishers = requests.stream().map(asyncProjectService::update).toList();
    Flux.merge(publishers)
        .doOnError(UnknownRequestException.class, this::handleUnknownRequest)
        .doOnError(RequestFailedException.class, this::handleRequestFailed)
        .doOnError(AccessDeniedException.class, this::handleAccessDenied)
        .doOnSubscribe(
            subscription -> notifyUser(requestId))
        .doOnCancel(() -> log.debug("Cancelled project update request"))
        .doOnTerminate(() -> cleanPendingTask(requestId))
        .doOnComplete(this::handleSuccess)
        .subscribe(this::removeFromCache);
  }

  private void submitRequest(FundingInformationCreationRequest request) {
    asyncProjectService.create(request)
        .doOnError(UnknownRequestException.class, this::handleUnknownRequest)
        .doOnError(RequestFailedException.class, this::handleRequestFailed)
        .doOnError(AccessDeniedException.class, this::handleAccessDenied)
        .doOnSubscribe(subscription -> notifyUser(request.requestId()))
        .doOnCancel(() -> log.debug("Cancelled project delete request"))
        .doOnTerminate(() -> cleanPendingTask(request.requestId()))
        .doOnSuccess(this::handleSuccess)
        .subscribe();
  }

  private void submitRequest(ProjectDeletionRequest request) {
    asyncProjectService.delete(request)
        .doOnError(UnknownRequestException.class, this::handleUnknownRequest)
        .doOnError(RequestFailedException.class, this::handleRequestFailed)
        .doOnError(AccessDeniedException.class, this::handleAccessDenied)
        .doOnSubscribe(subscription -> notifyUser(request.requestId()))
        .doOnCancel(() -> log.debug("Cancelled project delete request"))
        .doOnTerminate(() -> cleanPendingTask(request.requestId()))
        .doOnSuccess(this::handleSuccess)
        .subscribe();
  }


  private void submitRequest(FundingInformationDeletionRequest request) {
    asyncProjectService.delete(request)
        .doOnError(UnknownRequestException.class, this::handleUnknownRequest)
        .doOnError(RequestFailedException.class, this::handleRequestFailed)
        .doOnError(AccessDeniedException.class, this::handleAccessDenied)
        .doOnSubscribe(subscription -> notifyUser(request.requestId()))
        .doOnCancel(() -> log.debug("Cancelled project delete request"))
        .doOnTerminate(() -> cleanPendingTask(request.requestId()))
        .doOnSuccess(this::handleSuccess)
        .subscribe();
  }

  private void submitRequest(ProjectResponsibleDeletionRequest request) {
    asyncProjectService.delete(request)
        .doOnError(UnknownRequestException.class, this::handleUnknownRequest)
        .doOnError(RequestFailedException.class, this::handleRequestFailed)
        .doOnError(AccessDeniedException.class, this::handleAccessDenied)
        .doOnSubscribe(
            subscription -> notifyUser(request.requestId()))
        .doOnCancel(() -> log.debug("Cancelled project update request: " + request))
        .doOnTerminate(() -> cleanPendingTask(request.requestId()))
        .doOnSuccess(this::handleSuccess)
        .subscribe();
  }

  private void submitRequest(ProjectUpdateRequest request) {
    requestCache.store(request);

    asyncProjectService.update(request)
        .doOnError(UnknownRequestException.class, this::handleUnknownRequest)
        .doOnError(RequestFailedException.class, this::handleRequestFailed)
        .doOnError(AccessDeniedException.class, this::handleAccessDenied)
        .doOnSubscribe(
            subscription -> notifyUser(request.requestId()))
        .doOnCancel(() -> log.debug("Cancelled project update request: " + request))
        .doOnTerminate(() -> cleanPendingTask(request.requestId()))
        .doOnSuccess(this::handleSuccess)
        .subscribe(this::removeFromCache);
  }

  private void cleanPendingTask(String requestId) {
    getUI().ifPresent(ui -> ui.access(() -> {
      closePendingTaskToast(requestId);
      removePendingTaskToast(requestId);
    }));
  }

  private void notifyUser(String requestId) {
    getUI().ifPresent(ui -> ui.access(() -> {
      taskInProgressToast = messageSourceNotificationFactory.pendingTaskToast("task.in-progress",
          new Object[]{"Updating project"}, getLocale());
      taskInProgressToast.open();
      addPendingTaskToast(requestId, taskInProgressToast);
    }));
  }

  private void closePendingTaskToast(String requestId) {
    if (pendingTaskToasts.containsKey(requestId)) {
      pendingTaskToasts.get(requestId).close();
    }
  }

  private void removePendingTaskToast(String requestId) {
    pendingTaskToasts.remove(requestId);
  }

  private void addPendingTaskToast(String requestId, Toast toast) {
    if (pendingTaskToasts.containsKey(requestId)) {
      return;
    }
    pendingTaskToasts.put(requestId, toast);
  }

  private void removeFromCache(ProjectUpdateResponse response) {
    getUI().ifPresent(ui -> ui.access(() -> {
      try {
        requestCache.remove(response.requestId());
      } catch (CacheException e) {
        log.error(e.getMessage());
      }
    }));
  }

  private void handleSuccess() {
    getUI().ifPresent(ui -> ui.access(() -> {
      var toast = notificationFactory.toast(PROJECT_UPDATED_SUCCESS,
          new String[]{}, getLocale());
      toast.open();
      ui.push();
      reloadInformation(context);
    }));
  }

  private void handleSuccess(ProjectDeletionResponse response) {
    getUI().ifPresent(ui -> ui.access(() -> {
      var toast = notificationFactory.toast(PROJECT_UPDATED_SUCCESS,
          new String[]{}, getLocale());
      toast.open();
      reloadInformation(context);
    }));
  }

  private void handleSuccess(ProjectResponsibleDeletionResponse response) {
    getUI().ifPresent(ui -> ui.access(() -> {
      var toast = notificationFactory.toast(PROJECT_UPDATED_SUCCESS,
          new String[]{}, getLocale());
      toast.open();
      reloadInformation(context);
    }));
  }

  private void handleSuccess(FundingInformationCreationResponse response) {
    getUI().ifPresent(ui -> ui.access(() -> {
      var toast = notificationFactory.toast(PROJECT_UPDATED_SUCCESS,
          new String[]{}, getLocale());
      toast.open();
      reloadInformation(context);
    }));
  }

  private void handleSuccess(FundingInformationDeletionResponse response) {
    getUI().ifPresent(ui -> ui.access(() -> {
      var toast = notificationFactory.toast(PROJECT_UPDATED_SUCCESS,
          new String[]{}, getLocale());
      toast.open();
      reloadInformation(context);
    }));
  }

  /*
  Handler for successful project updates
   */
  private void handleSuccess(ProjectUpdateResponse response) {
    handleSuccess();
  }

  /*
  Handler for request failures. This covers failures the user can
  re-try it again, so the last request needs to be cached in the current user
  session and ui scope.
   */
  private void handleRequestFailed(RequestFailedException error) {
    log.error("request failed", error);
    getUI().ifPresent(
        ui -> ui.access(() -> requestCache.get(error.getRequestId()).ifPresentOrElse(request -> {
          // do sth with the cache
          var toast = notificationFactory.toast("project.updated.error.retry",
              new String[]{}, getLocale());
          toast.open();
        }, () -> {
          var toast = notificationFactory.toast("project.updated.error",
              new String[]{}, getLocale());
          toast.open();
        })));
  }

  /*
  Handler for unknown requests. This happens when the wrong service method
  has been called, so be sure to check the requests the service API can handle.
   */
  private void handleUnknownRequest(UnknownRequestException error) {
    log.error("unknown request", error);
    getUI().ifPresent(ui -> ui.access(() -> {
      var toast = notificationFactory.toast("project.updated.error",
          new String[]{}, getLocale());
      toast.open();
    }));
  }

  /*
  Handler for access denied exceptions. This usually happens, when the service method called
  requires rights the current logged-in user does not have.
   */
  private void handleAccessDenied(AccessDeniedException e) {
    log.error("access denied", e);
    getUI().ifPresent(ui -> ui.access(() -> {
      var toast = notificationFactory.toast("access.denied", new String[]{}, getLocale());
      toast.open();
    }));
  }

  /**
   * The "Project details" band: the project's storable facts, read-only, with one link to the
   * place that edits them. The prototype keeps the summary read-only and routes the edit to
   * Settings; that surface does not exist yet, so the link opens a dialog hosting the same two
   * forms (design and funding) the summary used to inline.
   */
  private void buildMetadataBand(ProjectOverview projectOverview, Project project) {
    projectMetadataBand.removeAll();
    projectMetadataBand.addClassName("project-metadata");

    Div head = new Div();
    head.addClassName("project-metadata-head");
    Span title = new Span("Project details");
    title.addClassName("project-metadata-title");
    Button edit = new Button("Edit project details");
    edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    edit.addClassName("project-metadata-edit");
    edit.addClickListener(event -> showEditProjectDetailsDialog(project));
    head.add(title, edit);

    Div facts = new Div();
    facts.addClassName("project-metadata-facts");
    facts.add(fact("Project ID", projectOverview.projectCode()));
    facts.add(fact("Funding", project.funding()
        .map(funding -> "%s (%s)".formatted(funding.grant(), funding.grantId()))
        .orElse("No funding information provided.")));
    facts.add(fact("Primary data type", primaryDataTypes(projectOverview)));

    projectMetadataBand.add(head, facts);
  }

  private static Div fact(String label, String value) {
    Div fact = new Div();
    fact.addClassName("project-metadata-fact");
    Span labelSpan = new Span(label);
    labelSpan.addClassName("project-metadata-fact-label");
    Span valueSpan = new Span(value);
    valueSpan.addClassName("project-metadata-fact-value");
    fact.add(labelSpan, valueSpan);
    return fact;
  }

  private static String primaryDataTypes(ProjectOverview overview) {
    List<String> types = new ArrayList<>();
    if (overview.ngsMeasurementCount() > 0) {
      types.add("Genomics");
    }
    if (overview.pxpMeasurementCount() > 0) {
      types.add("Proteomics");
    }
    if (overview.ipMeasurementCount() > 0) {
      types.add("Immunopeptidomics");
    }
    return types.isEmpty() ? "No primary data yet" : String.join(" \u00b7 ", types);
  }

  /**
   * Edits the things the metadata band states: the project design (title, objective, design) and
   * its funding.
   */
  private void showEditProjectDetailsDialog(Project project) {
    var designForm = new ProjectDesignForm();
    designForm.setContent(convertToInfo(project));

    var fundingField = FundingField.createHorizontal("Funding");
    var fundingForm = FundingInputForm.create(new BoundFundingField(fundingField,
        Validator.from(incompletePredicate(),
            "Please provide complete information for both, grant and grand ID.")));
    fundingForm.setContent(
        convertToInfo(project).getFundingEntry().orElse(new FundingEntry("", "")));

    var body = new Div(designForm, fundingForm);
    body.addClassName("project-metadata-edit-body");

    UserInput input = new UserInput() {
      @Override
      public InputValidation validate() {
        return designForm.validate().and(fundingForm.validate());
      }

      @Override
      public boolean hasChanges() {
        return designForm.hasChanges() || fundingForm.hasChanges();
      }
    };

    AppDialog dialog = AppDialog.medium();
    DialogHeader.with(dialog, "Edit project details");
    DialogFooter.with(dialog, CANCEL_BUTTON_TEXT, SAVE_BUTTON_TEXT);
    DialogBody.with(dialog, body, input);
    dialog.registerCancelAction(dialog::close);
    dialog.registerConfirmAction(() -> {
      dialog.close();
      submitRequest(
          new ProjectUpdateRequest(project.getId().value(), designForm.getProjectDesign()));
      handleSubmission(project.getId().value(),
          convertFrom(fundingForm.getIfValid().orElseThrow()));
    });
    dialog.open();
  }


  private void buildHeaderSection(ProjectOverview projectOverview, Project project) {
    Objects.requireNonNull(projectOverview);

    // Identity band: <title> <code>, the way the prototype names the project in its band.
    SectionTitle title = new SectionTitle();
    title.setSize(Size.LARGE);
    Span titleText = new Span(projectOverview.projectTitle());
    Span code = new Span(projectOverview.projectCode());
    code.addClassName("project-identity-code");
    title.add(titleText, code);

    Button crateExportBtn = new Button("Export summary");
    crateExportBtn.addClickListener(event -> triggerRoCrateDownload());
    Button addExperimentBtn = new Button("Add experiment");
    addExperimentBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    addExperimentBtn.addClickListener(click -> fireEvent(
        new AddExperimentRequestedEvent(this, click.isFromClient())));

    SectionHeader header = new SectionHeader(title,
        new ActionBar(addExperimentBtn, crateExportBtn));
    header.setSmallTrailingMargin();

    identityMeta.addClassName("project-identity-meta");
    identityLastModified = "Last modified on %s".formatted(DateTimeFormat.asJavaFormatter(
            DateTimeFormat.SIMPLE_DATE_SHORT, ZoneId.systemDefault())
        .format(projectOverview.lastModified()));
    renderIdentityMeta(0, 0);

    var sectionContent = new SectionContent();
    sectionContent.add(identityMeta);
    sectionContent.add(contactsRow(project));
    sectionContent.add(objectiveBlock(project));
    headerSection.setHeader(header);
    headerSection.setContent(sectionContent);
  }

  private void updateIdentityMeta(List<ExperimentProgress> progress) {
    int attention = (int) progress.stream()
        .filter(entry -> entry.measurementsWithoutPrimaryData() > 0)
        .count();
    renderIdentityMeta(progress.size(), attention);
  }

  private void renderIdentityMeta(int experiments, int attention) {
    identityMeta.removeAll();
    identityMeta.add(new Span(identityLastModified));
    identityMeta.add(separator());
    identityMeta.add(new Span(experiments == 1 ? "1 experiment" : experiments + " experiments"));
    if (attention > 0) {
      identityMeta.add(separator());
      identityMeta.add(attentionChip(attention));
    }
  }

  /**
   * The attention rollup. Attention can hit several experiments at once, so the chip is a
   * project-level count that brings the reader to the per-experiment rows rather than jumping
   * into one arbitrary experiment.
   */
  private Div attentionChip(int attention) {
    Div chip = new Div();
    chip.addClassName("project-identity-attention");
    chip.addClassName("project-identity-attention--action");
    chip.getElement().setAttribute("title", "Show which experiments need attention");
    chip.add(new Span(attention == 1
        ? "1 experiment needs attention"
        : attention + " experiments need attention"));
    chip.add(VaadinIcon.ANGLE_RIGHT.create());
    Utility.activateOnClickAndKeys(chip, "button", this::scrollToAttention);
    return chip;
  }

  private void scrollToAttention() {
    getUI().ifPresent(ui -> ui.getPage().executeJs(
        "document.querySelector('.project-progress-attention')"
            + "?.scrollIntoView({behavior: 'smooth', block: 'center'})"));
  }

  private static Span separator() {
    Span separator = new Span("\u00b7");
    separator.addClassName("project-identity-separator");
    return separator;
  }

  /**
   * The objective under the identity block: expanded by default, clamped to a few lines with a
   * "Show more" toggle, exactly as the prototype presents it (never a collapsed grey bar).
   */
  private static Div objectiveBlock(Project project) {
    Div block = new Div();
    block.addClassName("project-objective");
    Span text = new Span(project.getProjectIntent().objective().objective());
    text.addClassName("project-objective-text");
    Button toggle = new Button("Show more");
    toggle.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    toggle.addClassName("project-objective-toggle");
    toggle.addClickListener(click -> {
      boolean expanded = text.hasClassName("is-expanded");
      text.getClassNames().set("is-expanded", !expanded);
      toggle.setText(expanded ? "Show more" : "Show less");
    });
    block.add(text, toggle);
    return block;
  }

  private InputStream forSummary(ProjectId projectId) {
    var byteBufferIterator = asyncProjectService.roCrateSummary(projectId.value())
        .timeout(Duration.ofSeconds(10)).toIterable()
        .iterator();
    return new ByteBufferIteratorInputStream(byteBufferIterator);
  }

  private void triggerRoCrateDownload() {
    var projectCode = projectInformationService.find(context.projectId().orElseThrow())
        .map(Project::getProjectCode)
        .map(ProjectCode::value)
        .orElse("");
    downloadComponent.trigger(new DownloadStreamProvider() {
      @Override
      public String getFilename() {
        return FileNameFormatter.formatWithTimestampedSimple(LocalDate.now(), projectCode,
            "project summary", "zip");
      }

      @Override
      public InputStream getStream() {
        return forSummary(context.projectId().orElseThrow());
      }

      @Override
      public String getContentType() {
        return "application/zip";
      }

      @Override
      public Optional<Long> contentLength() {
        return Optional.empty();
      }


    });

  }

  public AvatarGroup createAvatarGroup(Collection<UserInfo> userInfo) {
    AvatarGroup avatarGroup = new AvatarGroup();
    userInfo.forEach(user -> avatarGroup.add(new UserAvatarGroupItem(user.userName(),
        user.userId())));
    avatarGroup.setMaxItemsVisible(3);
    return avatarGroup;
  }


}
