package life.qbic.datamanager.views.projects.project.info;

import static life.qbic.datamanager.views.MeasurementType.GENOMICS;
import static life.qbic.datamanager.views.MeasurementType.PROTEOMICS;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import edu.kit.datamanager.ro_crate.writer.RoCrateWriter;
import edu.kit.datamanager.ro_crate.writer.ZipWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.files.TempDirectory;
import life.qbic.datamanager.files.export.download.ByteArrayDownloadStreamProvider;
import life.qbic.datamanager.files.export.rocrate.ROCreateBuilder;
import life.qbic.datamanager.security.UserPermissions;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.TagFactory;
import life.qbic.datamanager.views.account.UserAvatar.UserAvatarGroupItem;
import life.qbic.datamanager.views.events.ProjectDesignUpdateEvent;
import life.qbic.datamanager.views.general.CollapsibleDetails;
import life.qbic.datamanager.views.general.DateTimeRendering;
import life.qbic.datamanager.views.general.DetailBox;
import life.qbic.datamanager.views.general.DetailBox.Header;
import life.qbic.datamanager.views.general.Heading;
import life.qbic.datamanager.views.general.IconLabel;
import life.qbic.datamanager.views.general.OntologyTermDisplay;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.general.Tag;
import life.qbic.datamanager.views.general.download.DownloadComponent;
import life.qbic.datamanager.views.general.funding.FundingEntry;
import life.qbic.datamanager.views.general.section.ActionBar;
import life.qbic.datamanager.views.general.section.Section;
import life.qbic.datamanager.views.general.section.Section.SectionBuilder;
import life.qbic.datamanager.views.general.section.SectionContent;
import life.qbic.datamanager.views.general.section.SectionHeader;
import life.qbic.datamanager.views.general.section.SectionNote;
import life.qbic.datamanager.views.general.section.SectionTitle;
import life.qbic.datamanager.views.general.section.SectionTitle.Size;
import life.qbic.datamanager.views.general.utils.Utility;
import life.qbic.datamanager.views.notifications.CancelConfirmationDialogFactory;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.datamanager.views.projects.ProjectInformation;
import life.qbic.datamanager.views.projects.edit.EditContactDialog;
import life.qbic.datamanager.views.projects.edit.EditFundingInformationDialog;
import life.qbic.datamanager.views.projects.edit.EditProjectDesignDialog;
import life.qbic.projectmanagement.application.api.AsyncProjectService;
import life.qbic.projectmanagement.application.api.AsyncProjectService.AccessDeniedException;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ProjectDesign;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ProjectUpdateRequest;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ProjectUpdateResponse;
import life.qbic.projectmanagement.application.api.AsyncProjectService.RequestFailedException;
import life.qbic.projectmanagement.application.api.AsyncProjectService.UnknownRequestException;
import life.qbic.datamanager.views.strategy.dialog.ClosingWithWarningStrategy;
import life.qbic.datamanager.views.strategy.dialog.ImmediateClosingStrategy;
import life.qbic.datamanager.views.strategy.scope.ReadScopeStrategy;
import life.qbic.datamanager.views.strategy.scope.UserScopeStrategy;
import life.qbic.datamanager.views.strategy.scope.WriteScopeStrategy;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.ProjectOverview;
import life.qbic.projectmanagement.application.ProjectOverview.UserInfo;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService;
import life.qbic.projectmanagement.domain.model.OntologyTerm;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.project.Contact;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;

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
  public static final String PROJECT_EDIT_CANCEL_CONFIRMATION_MESSAGE = "project.edit.cancel-confirmation.message";
  public static final String PROJECT_UPDATED_SUCCESS = "project.updated.success";
  private static final Logger log = logger(ProjectSummaryComponent.class);
  private final transient ProjectInformationService projectInformationService;
  private final transient ROCreateBuilder roCrateBuilder;
  private final transient TempDirectory tempDirectory;
  private final transient ExperimentInformationService experimentInformationService;
  private final transient UserPermissions userPermissions;
  private final transient MessageSourceNotificationFactory notificationFactory;
  private final transient CancelConfirmationDialogFactory cancelConfirmationDialogFactory;
  private final Section headerSection;
  private final Section projectDesignSection;
  private final Section experimentInformationSection;
  private final Section fundingInformationSection;
  private final Section projectContactsSection;
  private final DownloadComponent downloadComponent;
  private final transient AsyncProjectService asyncProjectService;
  private final MessageSourceNotificationFactory messageSourceNotificationFactory;
  private Context context;
  private EditProjectDesignDialog editProjectDesignDialog;
  private EditFundingInformationDialog editFundingInfoDialog;
  private EditContactDialog editContactsDialog;
  private transient List<? extends UserScopeStrategy> scopes;

  @Autowired
  public ProjectSummaryComponent(ProjectInformationService projectInformationService,
      ExperimentInformationService experimentInformationService,
      UserPermissions userPermissions,
      CancelConfirmationDialogFactory cancelConfirmationDialogFactory,
      ROCreateBuilder rOCreateBuilder, TempDirectory tempDirectory,
      MessageSourceNotificationFactory notificationFactory,
      AsyncProjectService asyncProjectService,
      MessageSourceNotificationFactory messageSourceNotificationFactory) {
    this.projectInformationService = Objects.requireNonNull(projectInformationService);
    this.headerSection = new SectionBuilder().build();
    this.projectDesignSection = new SectionBuilder().build();
    this.experimentInformationSection = new SectionBuilder().build();
    this.fundingInformationSection = new SectionBuilder().build();
    this.projectContactsSection = new SectionBuilder().build();
    this.tempDirectory = Objects.requireNonNull(tempDirectory);
    this.roCrateBuilder = Objects.requireNonNull(rOCreateBuilder);
    this.userPermissions = Objects.requireNonNull(userPermissions);
    this.notificationFactory = Objects.requireNonNull(notificationFactory);
    this.cancelConfirmationDialogFactory = Objects.requireNonNull(cancelConfirmationDialogFactory);
    this.experimentInformationService = experimentInformationService;
    this.asyncProjectService = Objects.requireNonNull(asyncProjectService);
    downloadComponent = new DownloadComponent();

    addClassName("project-details-component");

    add(headerSection);
    add(projectDesignSection);
    add(experimentInformationSection);
    add(fundingInformationSection);
    add(projectContactsSection);
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
        contact.emailAddress());
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
    buildExperimentInformationSection(experiments);
    buildFundingInformationSection(fullProject, convertToInfo(fullProject));
    buildProjectContactsInfoSection(fullProject);
  }

  private void buildProjectContactsInfoSection(Project project) {
    // set up the edit button, that opens the dialog for editing contacts
    var editButton = createButtonWithListener("Edit", listener -> {
      editContactsDialog = buildAndWireEditContacts(convertToInfo(project));
      editContactsDialog.open();
      editContactsDialog.addUpdateEventListener(event -> {
        updateContactInfo(context.projectId().orElseThrow(),
            event.content().orElseThrow());
        reloadInformation(context);
        editContactsDialog.close();
        var toast = notificationFactory.toast(PROJECT_UPDATED_SUCCESS,
            new String[]{project.getProjectCode().value()}, getLocale());
        toast.open();
      });
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

  private void updateContactInfo(ProjectId projectId, ProjectInformation projectInformation) {
    projectInformation.getResponsiblePerson().ifPresentOrElse(
        contact -> projectInformationService.setResponsibility(projectId,
            new Contact(contact.getFullName(), contact.getEmail())),
        () -> projectInformationService.removeResponsibility(projectId));

    projectInformationService.investigateProject(projectId,
        new Contact(projectInformation.getPrincipalInvestigator().getFullName(),
            projectInformation.getPrincipalInvestigator().getEmail()));

    projectInformationService.manageProject(projectId,
        new Contact(projectInformation.getProjectManager().getFullName(),
            projectInformation.getProjectManager().getEmail()));
  }

  private Div renderContactInfo(Contact contact) {
    var contactInfo = new Div();
    contactInfo.addClassName("vertical-list");
    var name = new Span(contact.fullName());
    var email = new Anchor("mailto:" + contact.emailAddress(), contact.emailAddress());
    contactInfo.add(name, email);
    return contactInfo;
  }

  private void buildFundingInformationSection(Project fullProject,
      ProjectInformation projectInformation) {
    var editButton = createButtonWithListener("Edit", listener -> {
      editFundingInfoDialog = buildAndWireEditFinanceInfo(projectInformation);
      editFundingInfoDialog.open();
      editFundingInfoDialog.addUpdateEventListener(event -> {
        var projectId = context.projectId().orElseThrow();
        // If the funding entry is "null", we need to remove the funding from the project
        // Otherwise we update it
        event.content().orElseThrow().getFundingEntry()
            .ifPresentOrElse(fundingEntry -> updateFundingInfo(projectId, fundingEntry),
                () -> removeFunding(projectId));
        reloadInformation(context);
        editFundingInfoDialog.close();
        var toast = notificationFactory.toast(PROJECT_UPDATED_SUCCESS,
            new String[]{fullProject.getProjectCode().value()}, getLocale());
        toast.open();
      });
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

  private EditContactDialog buildAndWireEditContacts(ProjectInformation projectInformation) {
    var dialog = new EditContactDialog(projectInformation,
        Utility.tryToLoadFromPrincipal().orElse(null));
    var defaultStrategy = new ImmediateClosingStrategy(dialog);
    var cancelDialog = cancelConfirmationDialogFactory.cancelConfirmationDialog(
        PROJECT_EDIT_CANCEL_CONFIRMATION_MESSAGE, getLocale());
    var withWarning = new ClosingWithWarningStrategy(dialog, cancelDialog);
    dialog.setDefaultCancelStrategy(defaultStrategy);
    dialog.setCancelWithoutSaveStrategy(withWarning);
    return dialog;
  }

  private EditFundingInformationDialog buildAndWireEditFinanceInfo(
      ProjectInformation projectInformation) {
    var dialog = new EditFundingInformationDialog(projectInformation);
    var defaultStrategy = new ImmediateClosingStrategy(dialog);
    var cancelDialog = cancelConfirmationDialogFactory.cancelConfirmationDialog(
        PROJECT_EDIT_CANCEL_CONFIRMATION_MESSAGE, getLocale());
    var withWarning = new ClosingWithWarningStrategy(dialog, cancelDialog);
    dialog.setDefaultCancelStrategy(defaultStrategy);
    dialog.setCancelWithoutSaveStrategy(withWarning);
    return dialog;
  }

  private void buildExperimentInformationSection(
      List<Experiment> experiments) {
    experimentInformationSection.setHeader(
        new SectionHeader(new SectionTitle("Experiment Information")));
    var speciesBox = new DetailBox();
    var speciesHeader = new Header(VaadinIcon.MALE.create(), "Species");
    speciesBox.setHeader(speciesHeader);
    speciesBox.setContent(buildSpeciesInfo(experiments));
    speciesBox.addClassNames(FIXED_MEDIUM_WIDTH_CSS);

    var specimenBox = new DetailBox();
    var specimenHeader = new Header(VaadinIcon.DROP.create(), "Specimen");
    specimenBox.setHeader(specimenHeader);
    specimenBox.setContent(buildSpecimenInfo(experiments));
    specimenBox.addClassName(FIXED_MEDIUM_WIDTH_CSS);

    var analyteBox = new DetailBox();
    var analyteHeader = new Header(VaadinIcon.CLUSTER.create(), "Analytes");
    analyteBox.setHeader(analyteHeader);
    analyteBox.setContent(buildAnalyteInfo(experiments));
    analyteBox.addClassName(FIXED_MEDIUM_WIDTH_CSS);

    var sectionContent = new SectionContent();
    sectionContent.add(speciesBox);
    sectionContent.add(specimenBox);
    sectionContent.add(analyteBox);
    sectionContent.addClassNames("horizontal-list", "gap-medium", "wrapping-flex-container");
    experimentInformationSection.setContent(sectionContent);
  }

  private Div buildSpeciesInfo(List<Experiment> experiments) {
    var ontologyTerms = extractSpecies(experiments);
    return buildOntologyInfo(ontologyTerms);
  }

  private Div buildSpecimenInfo(List<Experiment> experiments) {
    var ontologyTerms = extractSpecimen(experiments);
    return buildOntologyInfo(ontologyTerms);
  }

  private Div buildAnalyteInfo(List<Experiment> experiments) {
    var ontologyTerms = extractAnalyte(experiments);
    return buildOntologyInfo(ontologyTerms);
  }

  private Div buildOntologyInfo(List<OntologyTerm> terms) {
    var container = new Div();
    terms.stream().map(this::convert).forEach(container::add);
    container.addClassNames("vertical-list", "gap-small");
    return container;
  }

  private OntologyTermDisplay convert(OntologyTerm ontologyTerm) {
    return new OntologyTermDisplay(ontologyTerm.getLabel(), ontologyTerm.getOboId(),
        ontologyTerm.getClassIri());
  }

  private List<OntologyTerm> extractSpecies(List<Experiment> experiments) {
    return experiments.stream().flatMap(experiment -> experiment.getSpecies().stream()).distinct()
        .toList();
  }

  private List<OntologyTerm> extractSpecimen(List<Experiment> experiments) {
    return experiments.stream().flatMap(experiment -> experiment.getSpecimens().stream()).distinct()
        .toList();
  }

  private List<OntologyTerm> extractAnalyte(List<Experiment> experiments) {
    return experiments.stream().flatMap(experiment -> experiment.getAnalytes().stream()).distinct()
        .toList();
  }

  private void buildDesignSection(ProjectOverview projectInformation, Project project) {
    var editButton = new Button("Edit");
    editButton.addClickListener(listener -> {
      editProjectDesignDialog = buildAndWireEditProjectDesign(project);
      editProjectDesignDialog.open();
      editProjectDesignDialog.addUpdateEventListener(event -> {
        handleUpdateEvent(event);
        editProjectDesignDialog.close();
      });

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

  /*
  Handler for project design update events.

  Since the project id is not referenced in the event, we assume that
  the UI scope was not left and the current context references the project
  that is supposed to be updated.
   */
  private void handleUpdateEvent(ProjectDesignUpdateEvent event) {
    if (event.content().isEmpty()) {
      log.debug("No content to be updated");
      // Nothing to be done
      return;
    }
    var project = context.projectId().orElseThrow().value();
    var info = event.content().orElseThrow();
    // Build the request for the service call
    var request = new ProjectUpdateRequest(project,
        new ProjectDesign(info.getProjectTitle(), info.getProjectObjective()));

    asyncProjectService.update(request)
        .doOnError(UnknownRequestException.class, this::handleUnknownRequest)
        .doOnError(RequestFailedException.class, this::handleRequestFailed)
        .doOnError(AccessDeniedException.class, this::handleAccessDenied)
        .subscribe(this::handleSuccess);

  }


  /*
  Handler for successful project updates
   */
  private void handleSuccess(ProjectUpdateResponse response) {
    log.debug("Received project update response: " + response);
    getUI().ifPresent(ui -> ui.access(() -> {
      var toast = notificationFactory.toast(PROJECT_UPDATED_SUCCESS,
          new String[]{}, getLocale());
      toast.open();
      reloadInformation(context);
    }));
  }

  /*
  Handler for request failures. This covers failures the user can
  re-try it again, so the last request needs to be cached in the current user
  session and ui scope.
   */
  private void handleRequestFailed(RequestFailedException error) {
    log.error("request failed", error);
    getUI().ifPresent(ui -> ui.access(() -> {
      var toast = notificationFactory.toast("project.updated.error.retry",
          new String[]{}, getLocale());
      // Todo Implement retry with cached request
      toast.open();
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

  private void updateFundingInfo(ProjectId projectId, FundingEntry fundingEntry) {
    projectInformationService.setFunding(projectId, fundingEntry.getLabel(),
        fundingEntry.getReferenceId());
  }

  private void removeFunding(ProjectId projectId) {
    projectInformationService.removeFunding(projectId);
  }

  private EditProjectDesignDialog buildAndWireEditProjectDesign(Project project) {
    var projectInfo = convertToInfo(project);
    var dialog = new EditProjectDesignDialog(projectInfo);
    var defaultStrategy = new ImmediateClosingStrategy(dialog);
    var cancelDialog = cancelConfirmationDialogFactory.cancelConfirmationDialog(
        PROJECT_EDIT_CANCEL_CONFIRMATION_MESSAGE, getLocale());
    var withWarning = new ClosingWithWarningStrategy(dialog, cancelDialog);
    dialog.setDefaultStrategy(defaultStrategy);
    dialog.setWarningStrategy(withWarning);
    return dialog;
  }

  private void buildHeaderSection(ProjectOverview projectOverview) {
    Objects.requireNonNull(projectOverview);
    var header = new SectionHeader(
        new SectionTitle("%s - %s".formatted(projectOverview.projectCode(),
            projectOverview.projectTitle()), Size.LARGE));
    var crateExportBtn = new Button("Export Project Summary");
    crateExportBtn.addClickListener(event -> {
      try {
        triggerRoCrateDownload();
      } catch (IOException e) {
        throw new ApplicationException("An error occurred while exporting RO-Crate", e);
      }
    });
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

  private void triggerRoCrateDownload() throws IOException {
    ProjectId projectId = context.projectId().orElseThrow();
    Project project = projectInformationService.find(projectId).orElseThrow();
    var tempBuildDir = tempDirectory.createDirectory();
    var zippedRoCrateDir = tempDirectory.createDirectory();
    try {
      var roCrate = roCrateBuilder.projectSummary(project, tempBuildDir);
      var roCrateZipWriter = new RoCrateWriter(new ZipWriter());
      var zippedRoCrateFile = zippedRoCrateDir.resolve(
          "%s-project-summary-ro-crate.zip".formatted(project.getProjectCode().value()));
      roCrateZipWriter.save(roCrate, zippedRoCrateFile.toString());
      byte[] cachedContent = Files.readAllBytes(zippedRoCrateFile);
      downloadComponent.trigger(new ByteArrayDownloadStreamProvider() {
        @Override
        public byte[] getBytes() {
          return cachedContent;
        }

        @Override
        public String getFilename() {
          return zippedRoCrateFile.getFileName().toString();
        }
      });
    } catch (RuntimeException e) {
      throw new ApplicationException("Error exporting ro-crate.zip", e);
    } finally {
      deleteTempDir(tempBuildDir.toFile());
      deleteTempDir(zippedRoCrateDir.toFile());
    }
  }

  private boolean deleteTempDir(File dir) throws IOException {
    File[] files = dir.listFiles(); //null if not a directory
    // https://docs.oracle.com/javase/8/docs/api/java/io/File.html#listFiles--
    if (files != null) {
      for (File file : files) {
        if (!deleteTempDir(file)) {
          return false;
        }
      }
    }
    return dir.delete();
  }

  public AvatarGroup createAvatarGroup(Collection<UserInfo> userInfo) {
    AvatarGroup avatarGroup = new AvatarGroup();
    userInfo.forEach(user -> avatarGroup.add(new UserAvatarGroupItem(user.userName(),
        user.userId())));
    avatarGroup.setMaxItemsVisible(Integer.valueOf(3));
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
}
