package life.qbic.datamanager.views.projects.project.settings;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.RequestCache;
import life.qbic.datamanager.RequestCache.CacheException;
import life.qbic.datamanager.security.UserPermissions;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.general.dialog.AlertDialog;
import life.qbic.datamanager.views.general.dialog.UserInput;
import life.qbic.datamanager.views.general.funding.BoundFundingField;
import life.qbic.datamanager.views.general.funding.FundingEntry;
import life.qbic.datamanager.views.general.funding.FundingField;
import life.qbic.datamanager.views.general.funding.FundingInputForm;
import life.qbic.datamanager.views.general.utils.Utility;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.datamanager.views.notifications.Toast;
import life.qbic.datamanager.views.projects.ProjectInformation;
import life.qbic.datamanager.views.projects.edit.EditContactsComponent;
import life.qbic.datamanager.views.projects.edit.ProjectDesignForm;
import life.qbic.datamanager.views.projects.project.ProjectMainLayout;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.api.AsyncProjectService;
import life.qbic.projectmanagement.application.api.AsyncProjectService.AccessDeniedException;
import life.qbic.projectmanagement.application.api.AsyncProjectService.FundingInformation;
import life.qbic.projectmanagement.application.api.AsyncProjectService.FundingInformationCreationRequest;
import life.qbic.projectmanagement.application.api.AsyncProjectService.FundingInformationCreationResponse;
import life.qbic.projectmanagement.application.api.AsyncProjectService.FundingInformationDeletionRequest;
import life.qbic.projectmanagement.application.api.AsyncProjectService.FundingInformationDeletionResponse;
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
import life.qbic.projectmanagement.application.contact.PersonLookupService;
import life.qbic.projectmanagement.domain.model.project.Contact;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;

/**
 * <b>Project settings</b>
 *
 * <p>Where a project's metadata is <em>changed</em>, as opposed to the project page, which only
 * shows it. This is the deliberate answer to "who edits contacts, and how?": a dedicated page
 * with page-local sub-navigation, not a dialog. Editing is rare and never urgent, so it does not
 * belong beside the title competing with the content, and keeping every editor in one place
 * teaches the rule once instead of scattering one-off dialogs per band.</p>
 *
 * @since 1.19.0
 */
@SpringComponent
@UIScope
@Route(value = "projects/:projectId?/settings", layout = ProjectMainLayout.class)
@PermitAll
public class ProjectSettingsMain extends Main implements BeforeEnterObserver {

  public static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";

  private static final Logger log = logger(ProjectSettingsMain.class);
  private static final String CANCEL_BUTTON_TEXT = "Cancel";
  private static final String SAVE_BUTTON_TEXT = "Save";
  private static final String SECTION_DESCRIPTION = "Description";
  private static final String SECTION_FUNDING = "Funding";
  private static final String SECTION_CONTACTS = "Contacts";
  private static final String SECTION_DELETION = "Project deletion";
  private static final List<String> SECTIONS = List.of(SECTION_DESCRIPTION, SECTION_FUNDING,
      SECTION_CONTACTS, SECTION_DELETION);

  private final transient ProjectInformationService projectInformationService;
  private final transient PersonLookupService personLookupService;
  private final transient AsyncProjectService asyncProjectService;
  private final transient UserPermissions userPermissions;
  private final transient MessageSourceNotificationFactory notificationFactory;
  private final transient RequestCache requestCache;

  private final Div area = new Div();
  private final Map<String, Toast> pendingTaskToasts = new HashMap<>();
  private Toast taskInProgressToast;
  private Context context;
  private String section = SECTION_DESCRIPTION;
  private boolean editing;

  public ProjectSettingsMain(@Autowired ProjectInformationService projectInformationService,
      @Autowired PersonLookupService personLookupService,
      @Autowired AsyncProjectService asyncProjectService,
      @Autowired UserPermissions userPermissions,
      @Autowired MessageSourceNotificationFactory notificationFactory,
      @Autowired RequestCache requestCache) {
    this.projectInformationService = requireNonNull(projectInformationService);
    this.personLookupService = requireNonNull(personLookupService);
    this.asyncProjectService = requireNonNull(asyncProjectService);
    this.userPermissions = requireNonNull(userPermissions);
    this.notificationFactory = requireNonNull(notificationFactory);
    this.requestCache = requireNonNull(requestCache);
    addClassName("project");
    area.addClassName("page-area");
    area.addClassName("project-settings");
    add(area);
  }

  @Override
  public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
    String projectIdValue = beforeEnterEvent.getRouteParameters()
        .get(PROJECT_ID_ROUTE_PARAMETER).orElseThrow();
    if (!ProjectId.isValid(projectIdValue)) {
      throw new ApplicationException("invalid project id " + projectIdValue);
    }
    ProjectId projectId = ProjectId.parse(projectIdValue);
    if (!userPermissions.readProject(projectId)) {
      beforeEnterEvent.rerouteToError(NotFoundException.class);
      return;
    }
    this.context = new Context().with(projectId);
    render();
  }

  private void render() {
    area.removeAll();
    Project project = project();
    area.add(pageHead(project));
    area.add(pageLayout(project));
  }

  private ProjectId projectId() {
    return context.projectId()
        .orElseThrow(() -> new ApplicationException("No project id provided"));
  }

  private Project project() {
    return projectInformationService.find(projectId())
        .orElseThrow(() -> new ApplicationException("No project found"));
  }


  // ── Page scaffolding ────────────────────────────────────────────────────

  private Div pageHead(Project project) {
    Div head = new Div();
    head.addClassName("project-settings-pagehead");
    H1 title = new H1("Project settings");
    title.addClassName("project-settings-title");
    Paragraph subtitle = new Paragraph("Metadata for "
        + project.getProjectIntent().projectTitle().title()
        + ". These values are shown read-only on the project page.");
    subtitle.addClassName("project-settings-subtitle");
    head.add(title, subtitle);
    return head;
  }

  private Div pageLayout(Project project) {
    Div layout = new Div();
    layout.addClassName("project-settings-layout");
    layout.add(sectionNav(), sectionContent(project));
    return layout;
  }

  /** Page-local sub-navigation: sections of this page, not app-wide destinations. */
  private Div sectionNav() {
    Div nav = new Div();
    nav.addClassName("project-settings-nav");
    for (String candidate : SECTIONS) {
      Button item = new Button(candidate);
      item.addClassName("project-settings-navitem");
      if (candidate.equals(section)) {
        item.getElement().setAttribute("aria-current", "true");
      }
      item.addClickListener(event -> {
        section = candidate;
        editing = false;
        render();
      });
      nav.add(item);
    }
    return nav;
  }

  private Div sectionContent(Project project) {
    Div content = new Div();
    content.addClassName("project-settings-content");
    switch (section) {
      case SECTION_FUNDING -> fundingSection(content, project);
      case SECTION_CONTACTS -> contactsSection(content, project);
      case SECTION_DELETION -> deletionSection(content, project);
      default -> descriptionSection(content, project);
    }
    return content;
  }

  private Div sectionHeading(String title, String subtitle) {
    Div head = new Div();
    head.addClassName("project-settings-sectionhead");
    H2 h2 = new H2(title);
    h2.addClassName("project-settings-sectiontitle");
    Paragraph p = new Paragraph(subtitle);
    p.addClassName("project-settings-subtitle");
    head.add(h2, p);
    return head;
  }

  /**
   * A card's heading with the action scoped to what it edits: "Edit" swaps the read-only facts for
   * the form in place — no dialog, and no separate "edit mode" page state to get lost in.
   */
  private Div cardHead(String title, UserInput input, Runnable onSave) {
    Div head = new Div();
    head.addClassName("project-settings-cardhead");
    Span span = new Span(title);
    span.addClassName("project-settings-cardtitle");
    Div actions = new Div();
    actions.addClassName("project-settings-cardactions");
    if (editing) {
      Button save = new Button(SAVE_BUTTON_TEXT);
      save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
      save.addClickListener(event -> {
        if (!input.validate().hasPassed()) {
          return;
        }
        onSave.run();
        editing = false;
      });
      Button cancel = new Button(CANCEL_BUTTON_TEXT);
      cancel.addClickListener(event -> {
        editing = false;
        render();
      });
      actions.add(save, cancel);
    } else {
      Button edit = new Button("Edit");
      edit.addClickListener(event -> {
        editing = true;
        render();
      });
      actions.add(edit);
    }
    head.add(span, actions);
    return head;
  }

  private static Div settingField(String label, String value) {
    Div field = new Div();
    field.addClassName("project-settings-fact");
    Span labelSpan = new Span(label);
    labelSpan.addClassName("project-settings-fact-label");
    Paragraph valueParagraph = new Paragraph(value);
    valueParagraph.addClassName("project-settings-fact-value");
    field.add(labelSpan, valueParagraph);
    return field;
  }

  // ── Description ─────────────────────────────────────────────────────────

  private void descriptionSection(Div content, Project project) {
    content.add(sectionHeading("Description",
        "The objective and design summary shown at the top of the project page."));

    ProjectDesignForm form = new ProjectDesignForm();
    form.setContent(convertToInfo(project));

    Div card = new Div();
    card.addClassName("project-settings-card");
    card.add(cardHead("Project description", form, () -> submitRequest(
        new ProjectUpdateRequest(project.getId().value(), form.getProjectDesign()))));
    if (editing) {
      form.addClassName("project-settings-form");
      card.add(form);
    } else {
      Div facts = new Div();
      facts.addClassName("project-settings-facts");
      facts.add(settingField("Project title",
          project.getProjectIntent().projectTitle().title()));
      facts.add(settingField("Objective",
          project.getProjectIntent().objective().objective()));
      card.add(facts);
    }
    content.add(card);
  }

  // ── Funding ─────────────────────────────────────────────────────────────

  private void fundingSection(Div content, Project project) {
    content.add(sectionHeading("Funding",
        "Grant or funding reference recorded for this project."));

    var fundingField = FundingField.createHorizontal("Funding");
    var form = FundingInputForm.create(new BoundFundingField(fundingField,
        Validator.from(incompletePredicate(),
            "Please provide complete information for both, grant and grand ID.")));
    form.setContent(convertToInfo(project).getFundingEntry().orElse(new FundingEntry("", "")));

    Div card = new Div();
    card.addClassName("project-settings-card");
    card.add(cardHead("Funding information", form, () -> submitRequest(
        project.getId().value(), convertFrom(form.getIfValid().orElseThrow()))));
    if (editing) {
      form.addClassName("project-settings-form");
      card.add(form);
    } else {
      Div facts = new Div();
      facts.addClassName("project-settings-facts");
      facts.add(settingField("Funding", project.funding()
          .map(funding -> "%s (%s)".formatted(funding.grant(), funding.grantId()))
          .orElse("No funding information provided.")));
      card.add(facts);
    }
    content.add(card);
  }

  // ── Contacts ────────────────────────────────────────────────────────────

  private void contactsSection(Div content, Project project) {
    content.add(sectionHeading("Contacts",
        "The people accountable for this project. Shown read-only on the project page."));

    EditContactsComponent editor = new EditContactsComponent(convertToInfo(project),
        Utility.tryToLoadFromPrincipal().orElse(null), personLookupService);

    Div card = new Div();
    card.addClassName("project-settings-card");
    card.add(cardHead("Contact persons", editor, () -> {
      var manager = toProjectContact(editor.getIfValidManager().orElseThrow()).orElseThrow();
      var investigator = toProjectContact(
          editor.getIfValidInvestigator().orElseThrow()).orElseThrow();
      var responsible = toProjectContact(
          editor.getIfValidProjectResponsible().orElseThrow()).orElse(null);
      if (isEmpty(responsible)) {
        responsible = null;
      }
      submitContacts(project.getId().value(), manager, investigator, responsible);
    }));
    if (editing) {
      card.add(editor);
    } else {
      Div facts = new Div();
      facts.addClassName("project-settings-facts");
      project.getResponsiblePerson().ifPresent(
          contact -> facts.add(settingField("Project responsible", contact.fullName())));
      facts.add(settingField("Project manager", project.getProjectManager().fullName()));
      facts.add(settingField("Principal investigator",
          project.getPrincipalInvestigator().fullName()));
      card.add(facts);
    }
    content.add(card);

    Paragraph note = new Paragraph(
        "An ORCID iD is required for the FAIR badge on the project page. Contacts without one are "
            + "listed without an ORCID link.");
    note.addClassName("project-settings-note");
    content.add(note);
  }

  // ── Project deletion ────────────────────────────────────────────────────

  private void deletionSection(Div content, Project project) {
    content.add(sectionHeading("Project deletion",
        "Deleting a project removes its experiments, samples and measurement metadata."));

    Div card = new Div();
    card.addClassName("project-settings-card");
    card.addClassName("project-settings-card--danger");
    Span title = new Span("Delete this project");
    title.addClassName("project-settings-cardtitle");
    Paragraph text = new Paragraph(
        "This cannot be undone. Registered data in the repository is not deleted.");
    text.addClassName("project-settings-text");
    Div actions = new Div();
    actions.addClassName("project-settings-cardactions");
    Button delete = new Button("Delete project");
    delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
    delete.addClickListener(event -> AlertDialog.danger(this, "Delete this project?",
            "This cannot be undone. Registered data in the repository is not deleted.",
            "Delete project", CANCEL_BUTTON_TEXT,
            () -> submitRequest(new ProjectDeletionRequest(project.getId().value())))
        .open());
    actions.add(delete);
    card.add(title, text, actions);
    content.add(card);
  }

  // ── Submission plumbing ─────────────────────────────────────────────────

  private void submitContacts(String projectId, ProjectContact manager,
      ProjectContact investigator, ProjectContact responsible) {
    if (responsible != null) {
      submitMultiple(List.of(
          new ProjectUpdateRequest(projectId, new ProjectManager(manager)),
          new ProjectUpdateRequest(projectId, new PrincipalInvestigator(investigator)),
          new ProjectUpdateRequest(projectId, new ProjectResponsible(responsible))));
    } else {
      submitRequest(new ProjectResponsibleDeletionRequest(projectId));
      submitMultiple(List.of(
          new ProjectUpdateRequest(projectId, new ProjectManager(manager)),
          new ProjectUpdateRequest(projectId, new PrincipalInvestigator(investigator))));
    }
  }

  private void submitRequest(String projectId, FundingInformation funding) {
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
   * @since 1.19.0
   */
  private void submitMultiple(Collection<ProjectUpdateRequest> requests) {
    var requestId = UUID.randomUUID().toString();
    var publishers = requests.stream().map(asyncProjectService::update).toList();
    Flux.merge(publishers)
        .doOnError(UnknownRequestException.class, this::handleUnknownRequest)
        .doOnError(RequestFailedException.class, this::handleRequestFailed)
        .doOnError(AccessDeniedException.class, this::handleAccessDenied)
        .doOnSubscribe(subscription -> notifyUser(requestId))
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
        .doOnCancel(() -> log.debug("Cancelled funding create request"))
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
        .doOnCancel(() -> log.debug("Cancelled funding delete request"))
        .doOnTerminate(() -> cleanPendingTask(request.requestId()))
        .doOnSuccess(this::handleSuccess)
        .subscribe();
  }

  private void submitRequest(ProjectResponsibleDeletionRequest request) {
    asyncProjectService.delete(request)
        .doOnError(UnknownRequestException.class, this::handleUnknownRequest)
        .doOnError(RequestFailedException.class, this::handleRequestFailed)
        .doOnError(AccessDeniedException.class, this::handleAccessDenied)
        .doOnSubscribe(subscription -> notifyUser(request.requestId()))
        .doOnCancel(() -> log.debug("Cancelled responsible delete request"))
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

  private void submitRequest(ProjectUpdateRequest request) {
    requestCache.store(request);

    asyncProjectService.update(request)
        .doOnError(UnknownRequestException.class, this::handleUnknownRequest)
        .doOnError(RequestFailedException.class, this::handleRequestFailed)
        .doOnError(AccessDeniedException.class, this::handleAccessDenied)
        .doOnSubscribe(subscription -> notifyUser(request.requestId()))
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
      taskInProgressToast = notificationFactory.pendingTaskToast("task.in-progress",
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
      notificationFactory.toast("project.updated.success", new String[]{}, getLocale()).open();
      render();
      ui.push();
    }));
  }

  private void handleSuccess(ProjectDeletionResponse response) {
    handleSuccess();
  }

  private void handleSuccess(ProjectResponsibleDeletionResponse response) {
    handleSuccess();
  }

  private void handleSuccess(FundingInformationCreationResponse response) {
    handleSuccess();
  }

  private void handleSuccess(FundingInformationDeletionResponse response) {
    handleSuccess();
  }

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

  // ── Adapters ────────────────────────────────────────────────────────────

  private static ProjectInformation convertToInfo(Project project) {
    var info = new ProjectInformation();
    info.setProjectTitle(project.getProjectIntent().projectTitle().title());
    info.setProjectObjective(project.getProjectIntent().objective().objective());
    info.setProjectId(project.getProjectCode().value());
    project.funding().ifPresent(
        funding -> info.setFundingEntry(new FundingEntry(funding.grant(), funding.grantId())));
    project.getResponsiblePerson().ifPresent(
        contact -> info.setResponsiblePerson(convert(contact)));
    info.setPrincipalInvestigator(convert(project.getPrincipalInvestigator()));
    info.setProjectManager(convert(project.getProjectManager()));
    return info;
  }

  private static life.qbic.datamanager.views.general.contact.Contact convert(Contact contact) {
    return new life.qbic.datamanager.views.general.contact.Contact(contact.fullName(),
        contact.emailAddress(),
        contact.oidcInformation().map(Contact.OidcInformation::oidcId).orElse(null),
        contact.oidcInformation().map(Contact.OidcInformation::oidcIssuer).orElse(null));
  }

  private static Optional<ProjectContact> toProjectContact(
      life.qbic.datamanager.views.general.contact.Contact contact) {
    if (contact == null) {
      return Optional.empty();
    }
    return Optional.of(new ProjectContact(contact.fullName(), contact.email(), contact.oidc(),
        contact.oidcIssuer()));
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
}
