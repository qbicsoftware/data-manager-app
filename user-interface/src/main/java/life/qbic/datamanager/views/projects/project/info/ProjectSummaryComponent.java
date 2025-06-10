package life.qbic.datamanager.views.projects.project.info;

import static java.util.Objects.requireNonNull;
import static life.qbic.datamanager.views.MeasurementType.GENOMICS;
import static life.qbic.datamanager.views.MeasurementType.PROTEOMICS;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDate;
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
import java.util.function.Predicate;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.ByteBufferIteratorInputStream;
import life.qbic.application.commons.FileNameFormatter;
import life.qbic.datamanager.RequestCache;
import life.qbic.datamanager.RequestCache.CacheException;
import life.qbic.datamanager.files.export.download.DownloadStreamProvider;
import life.qbic.datamanager.security.UserPermissions;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.TagFactory;
import life.qbic.datamanager.views.account.UserAvatar.UserAvatarGroupItem;
import life.qbic.datamanager.views.general.CollapsibleDetails;
import life.qbic.datamanager.views.general.DateTimeRendering;
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
import life.qbic.datamanager.views.general.utils.Utility;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.datamanager.views.notifications.Toast;
import life.qbic.datamanager.views.projects.ProjectInformation;
import life.qbic.datamanager.views.projects.edit.EditContactsComponent;
import life.qbic.datamanager.views.projects.edit.ProjectDesignForm;
import life.qbic.datamanager.views.projects.project.experiments.experiment.ExperimentDetailsComponent;
import life.qbic.datamanager.views.strategy.scope.ReadScopeStrategy;
import life.qbic.datamanager.views.strategy.scope.UserScopeStrategy;
import life.qbic.datamanager.views.strategy.scope.WriteScopeStrategy;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.ProjectOverview;
import life.qbic.projectmanagement.application.ProjectOverview.UserInfo;
import life.qbic.projectmanagement.application.api.AsyncProjectService;
import life.qbic.projectmanagement.application.api.AsyncProjectService.AccessDeniedException;
import life.qbic.projectmanagement.application.api.AsyncProjectService.FundingDeletion;
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
import life.qbic.projectmanagement.application.api.AsyncProjectService.ProjectResponsibleDeletion;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ProjectResponsibleDeletionRequest;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ProjectResponsibleDeletionResponse;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ProjectUpdateRequest;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ProjectUpdateResponse;
import life.qbic.projectmanagement.application.api.AsyncProjectService.RequestFailedException;
import life.qbic.projectmanagement.application.api.AsyncProjectService.UnknownRequestException;
import life.qbic.projectmanagement.application.contact.PersonLookupService;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.project.Contact;
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
  private final transient ExperimentInformationService experimentInformationService;
  private final transient PersonLookupService personLookupService;
  private final transient UserPermissions userPermissions;
  private final transient MessageSourceNotificationFactory notificationFactory;
  private final Section headerSection;
  private final Section projectDesignSection;
  private final Section experimentInformationSection;
  private final Section fundingInformationSection;
  private final Section projectContactsSection;
  private final DownloadComponent downloadComponent;
  private final transient AsyncProjectService asyncProjectService;
  private final RequestCache requestCache;
  private final MessageSourceNotificationFactory messageSourceNotificationFactory;
  private final Map<String, Toast> pendingTaskToasts = new HashMap<>();
  private final ExperimentDetailsComponent experimentDetailsComponent;
  private Toast taskInProgressToast;
  private Context context;
  private transient List<? extends UserScopeStrategy> scopes;
  private DetailBox speciesDetailBox;
  private DetailBox specimenDetailBox;
  private DetailBox analyteDetailBox;

  @Autowired
  public ProjectSummaryComponent(ProjectInformationService projectInformationService,
      ExperimentInformationService experimentInformationService,
      PersonLookupService personLookupService,
      UserPermissions userPermissions,
      MessageSourceNotificationFactory notificationFactory,
      AsyncProjectService asyncProjectService,
      RequestCache requestCache,
      MessageSourceNotificationFactory messageSourceNotificationFactory,
      ExperimentDetailsComponent experimentDetailsComponent) {
    this.projectInformationService = Objects.requireNonNull(projectInformationService);
    this.personLookupService = requireNonNull(personLookupService,
        "person lookup service must not be null");
    this.headerSection = new SectionBuilder().build();
    this.projectDesignSection = new SectionBuilder().build();
    this.experimentInformationSection = new SectionBuilder().build();
    this.fundingInformationSection = new SectionBuilder().build();
    this.projectContactsSection = new SectionBuilder().build();
    this.userPermissions = Objects.requireNonNull(userPermissions);
    this.notificationFactory = Objects.requireNonNull(notificationFactory);
    this.experimentInformationService = experimentInformationService;
    this.asyncProjectService = Objects.requireNonNull(asyncProjectService);
    this.requestCache = Objects.requireNonNull(requestCache);
    this.downloadComponent = new DownloadComponent();

    addClassName("project-details-component");

    add(headerSection);
    add(projectDesignSection);
    add(experimentInformationSection);
    add(fundingInformationSection);
    add(projectContactsSection);
    add(downloadComponent);
    this.messageSourceNotificationFactory = messageSourceNotificationFactory;
    this.experimentDetailsComponent = experimentDetailsComponent;
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
        contact.emailAddress(), contact.oidc(), contact.oidcIssuer());
  }

  private static Button createButtonWithListener(String label,
      ComponentEventListener<ClickEvent<Button>> listener) {
    var button = new Button(label);
    button.addClickListener(listener);
    return button;
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
    var experiments = experimentInformationService.findAllForProject(projectId);
    setContent(projectOverview, fullProject, experiments);
    this.scopes = loadScope(userPermissions::editProject, projectId, projectDesignSection,
        fundingInformationSection, projectContactsSection);
    this.scopes.forEach(UserScopeStrategy::execute);
    // The header section only contains the RO-Crate action, which we want to enable always
    loadScope(id -> true, projectId, headerSection).forEach(UserScopeStrategy::execute);
    loadExperimentInfo(projectId.value());
  }

  private void loadExperimentInfo(String projectId) {
    asyncProjectService.getExperiments(projectId).collectList()
        .doOnSuccess(experiments -> {
          var species = new HashSet<OntologyTerm>();
          var specimen = new HashSet<OntologyTerm>();
          var analytes = new HashSet<OntologyTerm>();
          experiments.stream().forEach(experiment -> {
            species.addAll(experiment.species());
            specimen.addAll(experiment.specimen());
            analytes.addAll(experiment.analytes());
          });
          renderExperimentInfo(species, specimen, analytes);
        })
        .doOnError(AccessDeniedException.class, this::handleAccessDenied)
        .doOnError(RequestFailedException.class, this::handleRequestFailed)
        .doOnError(UnknownRequestException.class, this::handleUnknownRequest)
        .subscribe();
  }

  private void renderExperimentInfo(Set<OntologyTerm> species,
      Set<OntologyTerm> specimen,
      Set<OntologyTerm> analytes) {
    getUI().ifPresent(ui -> ui.access(() -> {
      speciesDetailBox.setContent(buildOntologyInfo(species));
      specimenDetailBox.setContent(buildOntologyInfo(specimen));
      analyteDetailBox.setContent(buildOntologyInfo(analytes));
    }));
  }

  private void reloadInformation(Context context) {
    var projectId = context.projectId()
        .orElseThrow(() -> new ApplicationException("No project id provided"));
    var projectOverview = projectInformationService.findOverview(projectId)
        .orElseThrow(() -> new ApplicationException("No project with given ID found"));
    var fullProject = projectInformationService.find(projectId)
        .orElseThrow(() -> new ApplicationException("No project found"));
    reloadProjectDesign(projectOverview, fullProject);
    reloadFundingInfoSection(fullProject);
    reloadProjectContactsSection(fullProject);
    // apply scope strategy again
    this.scopes.forEach(UserScopeStrategy::execute);
  }

  private void reloadProjectDesign(ProjectOverview projectOverview, Project project) {
    buildDesignSection(projectOverview, project);
  }

  private void reloadFundingInfoSection(Project project) {
    buildFundingInformationSection(project, convertToInfo(project));
  }

  private void reloadProjectContactsSection(Project project) {
    buildProjectContactsInfoSection(project);
  }

  private void setContent(ProjectOverview projectOverview, Project fullProject,
      List<Experiment> experiments) {
    Objects.requireNonNull(projectOverview);
    buildHeaderSection(projectOverview);
    buildDesignSection(projectOverview, fullProject);
    buildExperimentInformationSection();
    buildFundingInformationSection(fullProject, convertToInfo(fullProject));
    buildProjectContactsInfoSection(fullProject);
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

  private void buildProjectContactsInfoSection(Project project) {
    // set up the edit button, that opens the dialog for editing contacts
    var editButton = createButtonWithListener("Edit", listener -> {
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
          // we infer that the information has been removed for deletion
          // which might be risky and I will the future me: I told you so.
          responsible = null;
        }
        handleSubmission(project.getId().value(), manager, investigator, responsible);
      });
      dialog.open();
    });
    // create the section with header and content
    projectContactsSection.setHeader(
        new SectionHeader(new SectionTitle("Project Contacts"), new ActionBar(editButton)));
    var piBox = new DetailBox();
    var piBoxHeader = new Header(VaadinIcon.USER.create(), "Principal Investigator");
    piBox.setHeader(piBoxHeader);
    piBox.addClassName(FIXED_MEDIUM_WIDTH_CSS);
    var principalInvestigator = project.getPrincipalInvestigator();
    piBox.setContent(renderContactInfo(principalInvestigator));

    var pmBox = new DetailBox();
    var pmBoxHeader = new Header(VaadinIcon.USER.create(), "Project Manager");
    pmBox.setHeader(pmBoxHeader);
    pmBox.addClassName(FIXED_MEDIUM_WIDTH_CSS);
    var projectManager = project.getProjectManager();
    pmBox.setContent(renderContactInfo(projectManager));

    projectContactsSection.setContent(new SectionContent(piBox, pmBox));

    // If no responsible person has been defined, we do not want to show empty sections.
    if (project.getResponsiblePerson().isPresent()) {
      var prBox = new DetailBox();
      var prBoxHeader = new Header(VaadinIcon.USER.create(), "Project Responsible");
      prBox.setHeader(prBoxHeader);
      prBox.addClassName(FIXED_MEDIUM_WIDTH_CSS);
      var responsible = project.getResponsiblePerson().orElseThrow();
      prBox.setContent(renderContactInfo(responsible));
      projectContactsSection.content().add(prBox);
    }

    projectContactsSection.content().addClassNames("horizontal-list", "gap-medium",
        "wrapping-flex-container");
  }

  private Div renderContactInfo(Contact contact) {
    var contactInfo = new Div();
    contactInfo.addClassName("vertical-list");
    var name = new Span(contact.fullName());
    var email = new Anchor("mailto:" + contact.emailAddress(), contact.emailAddress());
    contactInfo.add(name, email);
    //Account for contacts without oidc or oidcissuer set
    if (contact.oidc() == null || contact.oidcIssuer() == null) {
      return contactInfo;
    }
    if (contact.oidcIssuer().isEmpty() || contact.oidc().isEmpty()) {
      return contactInfo;
    }
    var oidcType = Arrays.stream(OidcType.values())
        .filter(ot -> ot.getIssuer().equals(contact.oidcIssuer()))
        .findFirst();
    if (oidcType.isPresent()) {
      String oidcUrl = oidcType.get().getUrlFor(contact.oidc());
      Anchor oidcLink = new Anchor(oidcUrl, contact.oidc());
      oidcLink.setTarget(AnchorTarget.BLANK);
      OidcLogo oidcLogo = new OidcLogo(oidcType.get());
      Span oidcSpan = new Span(oidcLogo, oidcLink);
      oidcSpan.addClassNames("gap-02", "flex-align-items-center", "flex-horizontal");
      contactInfo.add(oidcSpan);
    }
    return contactInfo;
  }

  private void buildFundingInformationSection(Project fullProject,
      ProjectInformation projectInformation) {
    var editButton = createButtonWithListener("Edit", listener -> {
      var dialog = AppDialog.small();
      DialogHeader.with(dialog, "Edit funding information");
      DialogFooter.with(dialog, CANCEL_BUTTON_TEXT, SAVE_BUTTON_TEXT);

      var fundingField = FundingField.createHorizontal("Funding");
      var form = FundingInputForm.create(new BoundFundingField(fundingField,
          Validator.from(incompletePredicate(),
              "Please provide complete information for both, grant and grand ID.")));
      form.setContent(projectInformation.getFundingEntry().orElse(new FundingEntry("", "")));

      dialog.registerConfirmAction(() -> {
        dialog.close();
        var funding = convertFrom(form.getIfValid().orElseThrow());
        handleSubmission(fullProject.getId().value(), funding);
      });

      dialog.registerCancelAction(dialog::close);

      DialogBody.with(dialog, form, form);
      dialog.open();
    });

    fundingInformationSection.setHeader(
        new SectionHeader(new SectionTitle("Funding Information"),
            new ActionBar(editButton)));

    if (fullProject.funding().isEmpty()) {
      fundingInformationSection.setContent(
          new SectionContent(new Span("No funding information provided.")));
    } else {
      var grantIconLabel = new IconLabel(VaadinIcon.MONEY.create(), "Grant");
      var funding = fullProject.funding().orElseThrow();
      grantIconLabel.setInformation("%s (%s)".formatted(funding.grant(), funding.grantId()));
      grantIconLabel.setTooltipText(
          "Information about what grant served as funding for the project.");
      fundingInformationSection.setContent(new SectionContent(grantIconLabel));
    }

  }

  private void handleSubmission(String projectId, FundingInformation funding) {
    if (isEmpty(funding)) {
      submitRequest(new FundingInformationDeletionRequest(projectId));
      return;
    }
    submitRequest(new FundingInformationCreationRequest(projectId, funding));
  }

  private void buildExperimentInformationSection() {
    experimentInformationSection.setHeader(
        new SectionHeader(new SectionTitle("Experiment Information")));
    speciesDetailBox = new DetailBox();
    var speciesHeader = new Header(VaadinIcon.MALE.create(), "Species");
    speciesDetailBox.setHeader(speciesHeader);
    speciesDetailBox.setContent(new EmptyContent());
    speciesDetailBox.addClassNames(FIXED_MEDIUM_WIDTH_CSS);

    specimenDetailBox = new DetailBox();
    var specimenHeader = new Header(VaadinIcon.DROP.create(), "Specimen");
    specimenDetailBox.setHeader(specimenHeader);
    specimenDetailBox.setContent(new EmptyContent());
    specimenDetailBox.addClassName(FIXED_MEDIUM_WIDTH_CSS);

    analyteDetailBox = new DetailBox();
    var analyteHeader = new Header(VaadinIcon.CLUSTER.create(), "Analytes");
    analyteDetailBox.setHeader(analyteHeader);
    analyteDetailBox.setContent(new EmptyContent());
    analyteDetailBox.addClassName(FIXED_MEDIUM_WIDTH_CSS);

    var sectionContent = new SectionContent();
    sectionContent.add(speciesDetailBox);
    sectionContent.add(specimenDetailBox);
    sectionContent.add(analyteDetailBox);
    sectionContent.addClassNames("horizontal-list", "gap-medium", "wrapping-flex-container");
    experimentInformationSection.setContent(sectionContent);
  }

  private Div buildOntologyInfo(Set<OntologyTerm> terms) {
    var container = new Div();
    terms.stream().map(this::convert).forEach(container::add);
    container.addClassNames("vertical-list", "gap-small");
    return container;
  }

  private OntologyTermDisplay convert(OntologyTerm term) {
    return new OntologyTermDisplay(term.label(), term.oboId().toString(), term.id().toString());
  }


  private void buildDesignSection(ProjectOverview projectInformation, Project project) {
    var editButton = new Button("Edit");
    editButton.addClickListener(listener -> {
      var form = new ProjectDesignForm();
      form.setContent(convertToInfo(project));
      AppDialog dialog = createEditDesignDialog(project.getId().value(), form, form);
      dialog.registerCancelAction(dialog::close);
      dialog.registerConfirmAction(() -> {
        dialog.close();
        submitRequest(
            new ProjectUpdateRequest(project.getId().value(), form.getProjectDesign()));
      });
      dialog.open();
      add(dialog);
    });

    projectDesignSection.setHeader(
        new SectionHeader(new SectionTitle("Project Design"), new ActionBar(editButton)));
    var content = new SectionContent();

    // Set up the objective details
    var details = new Details();
    details.removeAll();
    var objectiveTitle = Heading.withIconAndText(VaadinIcon.MODAL_LIST.create(), "Objective");
    var objective = new SimpleParagraph(project.getProjectIntent().objective().objective());
    details.setSummary(objectiveTitle);
    details.add(objective);
    var collapsibleDetails = new CollapsibleDetails(details);
    collapsibleDetails.collapse();
    collapsibleDetails.addClassNames("background-color-grey", "padding-left-01", "padding-right-01",
        "line-height-01", "max-width-55rem", "text-justify", "box-corner-radius-small");

    content.add(
        Heading.withIconAndText(VaadinIcon.NOTEBOOK.create(), "Project ID and Title"));
    content.add(new SimpleParagraph("%s - %s".formatted(projectInformation.projectCode(),
        projectInformation.projectTitle())));
    content.add(collapsibleDetails);
    projectDesignSection.setContent(content);
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
    getUI().ifPresent(ui -> ui.access(() -> {
      requestCache.get(error.getRequestId()).ifPresentOrElse(request -> {
        // do sth with the cache
        var toast = notificationFactory.toast("project.updated.error.retry",
            new String[]{}, getLocale());
        toast.open();
      }, () -> {
        var toast = notificationFactory.toast("project.updated.error",
            new String[]{}, getLocale());
        toast.open();
      });
    }));
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

  private AppDialog createEditDesignDialog(String projectId, Component body, UserInput input) {
    var appDialog = AppDialog.medium();
    DialogHeader.with(appDialog, "Edit Project Design");
    DialogFooter.with(appDialog, CANCEL_BUTTON_TEXT, SAVE_BUTTON_TEXT);

    DialogBody.with(appDialog, body, input);
    return appDialog;
  }

  private void buildHeaderSection(ProjectOverview projectOverview) {
    Objects.requireNonNull(projectOverview);
    var header = new SectionHeader(
        new SectionTitle("%s - %s".formatted(projectOverview.projectCode(),
            projectOverview.projectTitle()), Size.LARGE));
    var crateExportBtn = new Button("Export Project Summary");

    crateExportBtn.addClickListener(event -> triggerRoCrateDownload());

    ActionBar actionBar = new ActionBar(crateExportBtn);
    header.setActionBar(actionBar);
    header.setSmallTrailingMargin();

    var sectionContent = new SectionContent();
    sectionContent.add(createAvatarGroup(projectOverview.collaboratorUserInfos()));
    sectionContent.add(createTags(projectOverview));

    header.setSectionNote(new SectionNote(
        "Last modified on %s".formatted(DateTimeRendering.simple(projectOverview.lastModified()))));
    headerSection.setHeader(header);
    headerSection.setContent(sectionContent);
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
    });

  }

  public AvatarGroup createAvatarGroup(Collection<UserInfo> userInfo) {
    AvatarGroup avatarGroup = new AvatarGroup();
    userInfo.forEach(user -> avatarGroup.add(new UserAvatarGroupItem(user.userName(),
        user.userId())));
    avatarGroup.setMaxItemsVisible(3);
    return avatarGroup;
  }

  private Div createTags(ProjectOverview projectOverview) {
    var tags = new Div();
    tags.addClassNames("tag-list", "gap-small");
    buildTags(projectOverview).forEach(tags::add);
    return tags;
  }

  private List<Tag> buildTags(ProjectOverview projectInformation) {
    var tags = new ArrayList<Tag>();
    if (projectInformation.ngsMeasurementCount() > 0) {
      tags.add(TagFactory.forMeasurement(GENOMICS));
    }
    if (projectInformation.pxpMeasurementCount() > 0) {
      tags.add(TagFactory.forMeasurement(PROTEOMICS));
    }
    return tags;
  }

  private static class EmptyContent extends Div {

    EmptyContent() {
      addClassNames("vertical-list", "gap-small");
      add("No information available");
    }
  }

}
