package life.qbic.datamanager.views.projects.project.info;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.security.UserPermissions;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.general.funding.FundingEntry;
import life.qbic.datamanager.views.projects.edit.EditProjectInformationDialog;
import life.qbic.datamanager.views.projects.edit.EditProjectInformationDialog.ProjectInformation;
import life.qbic.datamanager.views.projects.edit.EditProjectInformationDialog.ProjectUpdateEvent;
import life.qbic.datamanager.views.projects.project.info.InformationComponent.Entry;
import life.qbic.projectmanagement.application.ContactRepository;
import life.qbic.projectmanagement.application.ExperimentInformationService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.domain.model.OntologyTerm;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.project.Contact;
import life.qbic.projectmanagement.domain.model.project.Funding;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Project Details Component
 * <p>
 * Shows project details to the user.
 *
 * @since 1.0.0
 */
@UIScope
@SpringComponent
public class ProjectDetailsComponent extends PageArea {

  @Serial
  private static final long serialVersionUID = -5781313306040217724L;
  private final Div header = new Div();
  private final Span titleField = new Span();
  private final Span buttonBar = new Span();
  private final Div content = new Div();
  private final Span projectTitleField = new Span();
  private final Span projectObjectiveField = new Span();
  private final Div projectManagerField = new Div();
  private final Div principalInvestigatorField = new Div();
  private final Div responsiblePersonField = new Div();
  private final InformationComponent projectInformationSection = InformationComponent.create("", "");
  private final InformationComponent fundingInformationSection = InformationComponent.create(
      "Funding Information", "Information about project funding");
  private final InformationComponent collaboratorSection = InformationComponent.create(
      "Project Collaborators", "Important contacts for this project");
  private final transient ProjectInformationService projectInformationService;
  private final transient ExperimentInformationService experimentInformationService;
  private final transient ContactRepository contactRepository;
  private final UserPermissions userPermissions;
  private Context context;

  public ProjectDetailsComponent(@Autowired ProjectInformationService projectInformationService,
      @Autowired ExperimentInformationService experimentInformationService,
      @Autowired ContactRepository contactRepository,
      @Autowired UserPermissions userPermissions) {
    this.projectInformationService = requireNonNull(projectInformationService,
        "projectInformationService must not be null");
    this.experimentInformationService = requireNonNull(experimentInformationService,
        "experimentInformationService must not be null");
    this.contactRepository = requireNonNull(contactRepository,
        "contactRepository must not be null");
    this.userPermissions = requireNonNull(userPermissions, "userPermissions must not be null");
    layoutComponent();
    addListenerForNewEditEvent();
    addClassName("project-details-component");
  }

  private static List<Entry> extractProjectInfo(Project project, List<Experiment> experiments) {
    List<Entry> entries = new ArrayList<>();

    var projectCode = new Div();
    projectCode.setText(project.getProjectCode().value());
    entries.add(new Entry("Code", "The unique identifier of the project", projectCode));

    var projectTitle = new Div();
    projectTitle.setText(project.getProjectIntent().projectTitle().title());
    entries.add(new Entry("Title", "", projectTitle));

    var objective = new Div();
    objective.setText(project.getProjectIntent().objective().objective());
    entries.add(new Entry("Objective", "The objective of the project", objective));

    var species = new Div();
    species.addClassName("ontology-entry-collection");
    experiments.stream().flatMap(experiment -> experiment.getSpecies().stream()).forEach(ontologyClassDTO -> species.add(createOntologyEntryFrom(ontologyClassDTO)));
    entries.add(new Entry("Species", "", species));
    var specimen = new Div();
    specimen.addClassName("ontology-entry-collection");
    experiments.stream().flatMap(experiment -> experiment.getSpecimens().stream()).forEach(ontologyClassDTO -> specimen.add(createOntologyEntryFrom(ontologyClassDTO)));
    entries.add(new Entry("Specimen", "Tissue, cells or other matrix extracted from the "
        + "species", specimen));
    var analyte = new Div();
    analyte.addClassName("ontology-entry-collection");
    experiments.stream().flatMap(experiment -> experiment.getAnalytes().stream()).forEach(ontologyClassDTO -> analyte.add(createOntologyEntryFrom(ontologyClassDTO)));
    entries.add(new Entry("Analyte", "", analyte));
    return entries;
  }

  private static Span createOntologyEntryFrom(OntologyTerm ontologyTerm){
    String ontologyLinkName = ontologyTerm.getName().replace("_", ":");
    Span ontologyEntryLink = new Span(new Anchor(ontologyTerm.getClassIri(), ontologyLinkName));
    ontologyEntryLink.addClassName("ontology-link");
    Span ontologyEntry = new Span(new Span(ontologyTerm.getLabel()), ontologyEntryLink);
    ontologyEntry.addClassName("ontology-entry");
    return ontologyEntry;
  }

  private static List<Entry> extractFundingInfo(Project project) {
    List<Entry> entries = new ArrayList<>();

    project.funding().ifPresentOrElse(funding -> entries.addAll(fromFunding(funding)), () -> {
      var disclaimer = new Div();
      disclaimer.setText("No funding information provided.");
      entries.add(new Entry("Grant", "", disclaimer));
    });

    return entries;
  }

  private static List<Entry> fromFunding(Funding funding) {
    List<Entry> entries = new ArrayList<>();
    var grantLabel = "Grant";
    var info = new Div();
    info.setText(funding.grant());
    entries.add(new Entry(grantLabel, "", info));

    var grantIdLabel = "Grant ID";
    var grantId = new Div();
    grantId.setText(funding.grantId());
    entries.add(new Entry(grantIdLabel, "", grantId));

    return entries;
  }

  private static List<Entry> extractContactInfo(Project project) {
    List<Entry> entries = new ArrayList<>();

    var principalInvestigator = new Div();
    principalInvestigator.add(generateContactContainer(project.getPrincipalInvestigator()));
    entries.add(new Entry("Principal Investigator", "", principalInvestigator));

    var projectResponsible = new Div();
    project.getResponsiblePerson()
        .ifPresentOrElse(person -> projectResponsible.add(generateContactContainer(person)),
            () -> projectResponsible.add(createNoPersonAssignedSpan()));
    entries.add(new Entry("Project Responsible", "", projectResponsible));

    var projectManager = new Div();
    projectManager.add(generateContactContainer(project.getProjectManager()));
    entries.add(new Entry("Project Manager", "", projectManager));

    return entries;
  }

  private static Div generateContactContainer(Contact contact) {
    Span nameSpan = new Span(contact.fullName());
    Span emailSpan = new Span(contact.emailAddress());
    Div personContainer = new Div(nameSpan, emailSpan);
    personContainer.addClassName("person-contact-display");
    emailSpan.addClassNames("email");
    nameSpan.addClassName("name");
    return personContainer;
  }

  private static Span createNoPersonAssignedSpan() {
    Span noPersonAssignedSpan = new Span("-");
    noPersonAssignedSpan.addClassName("no-person-assigned");
    return noPersonAssignedSpan;
  }

  public void setContext(Context context) {
    if (context.projectId().isEmpty()) {
      throw new ApplicationException("no project id in context " + context);
    }
    this.context = context;
    loadProjectData(context.projectId().orElseThrow());
    showControls(userPermissions.editProject(context.projectId().orElseThrow()));

  }

  private void layoutComponent() {
    this.add(header);
    this.add(content);
    content.add(projectInformationSection, fundingInformationSection, collaboratorSection);
    content.addClassName("project-information-content");

    titleField.setText("Project Summary");
    header.addClassName("header");
    header.add(titleField, buttonBar);
    titleField.addClassName("title");
  }

  private Button editButton() {
    Button editButton = new Button("Edit");
    editButton.addClickListener(event -> openProjectInformationDialog());
    return editButton;
  }

  private void showControls(boolean enabled) {
    buttonBar.removeAll();
    if (enabled) {
      buttonBar.add(editButton());
    }
  }

  private void openProjectInformationDialog() {
    ProjectId projectId = context.projectId().orElseThrow();
    Optional<Project> project = projectInformationService.find(projectId);
    project.ifPresentOrElse(proj -> {
          EditProjectInformationDialog editProjectInformationDialog = generateEditProjectInformationDialog(
              proj);
          editProjectInformationDialog.addCancelListener(
              cancelEvent -> cancelEvent.getSource().close());
          editProjectInformationDialog.addProjectUpdateEventListener(this::onProjectUpdateEvent);
          editProjectInformationDialog.open();
        }
        , () -> {
          throw new ApplicationException(
              "Project information could not be retrieved from service");
        });
  }

  private EditProjectInformationDialog generateEditProjectInformationDialog(Project project) {
    EditProjectInformationDialog dialog = new EditProjectInformationDialog(contactRepository);
    ProjectInformation projectInformation = new ProjectInformation();
    projectInformation.setProjectTitle(project.getProjectIntent().projectTitle().title());
    projectInformation.setProjectObjective(project.getProjectIntent().objective().value());
    projectInformation.setPrincipalInvestigator(
        new life.qbic.datamanager.views.general.contact.Contact(
            project.getPrincipalInvestigator().fullName(),
            project.getPrincipalInvestigator().emailAddress()));
    project.getResponsiblePerson().ifPresent(
        it -> projectInformation.setResponsiblePerson(
            new life.qbic.datamanager.views.general.contact.Contact(it.fullName(),
                it.emailAddress()))
    );
    project.funding().ifPresent(funding -> projectInformation.setFundingEntry(
        new FundingEntry(funding.grant(), funding.grantId())));

    projectInformation.setProjectManager(
        new life.qbic.datamanager.views.general.contact.Contact(
            project.getProjectManager().fullName(),
            project.getProjectManager().emailAddress()));

    dialog.setProjectInformation(projectInformation);
    return dialog;
  }

  private void onProjectUpdateEvent(ProjectUpdateEvent projectUpdateEvent) {
    if (projectUpdateEvent.getOldValue().isEmpty() || !projectUpdateEvent.getOldValue()
        .orElseThrow()
        .equals(projectUpdateEvent.getValue())) {
      ProjectInformation projectInformation = projectUpdateEvent.getValue();
      updateProjectInformation(projectInformation);
      ProjectId projectId = context.projectId().orElseThrow();
      fireEvent(new ProjectEditEvent(this, projectId, projectUpdateEvent.isFromClient()));
    }
    projectUpdateEvent.getSource().close();
  }

  private void updateProjectInformation(ProjectInformation projectInformationContent) {
    ProjectId projectId = this.context.projectId().orElseThrow();
    projectInformationService.updateTitle(projectId, projectInformationContent.getProjectTitle());
    projectInformationService.stateObjective(projectId,
        projectInformationContent.getProjectObjective());
    projectInformationService.investigateProject(projectId,
        projectInformationContent.getPrincipalInvestigator().toDomainContact());
    projectInformationService.manageProject(projectId,
        projectInformationContent.getProjectManager().toDomainContact());

    projectInformationContent.getFundingEntry().ifPresentOrElse(
        funding -> projectInformationService.addFunding(projectId, funding.getLabel(),
            funding.getReferenceId()), () -> projectInformationService.removeFunding(projectId));

    projectInformationContent.getResponsiblePerson().ifPresentOrElse(contact ->
            projectInformationService.setResponsibility(projectId, contact.toDomainContact()),
        () -> projectInformationService.setResponsibility(projectId, null));
  }

  private void addListenerForNewEditEvent() {
    addListener(ProjectEditEvent.class, event -> loadProjectData(event.projectId()));
  }

  private void loadProjectData(ProjectId projectId) {
    projectInformationService.find(projectId)
        .ifPresentOrElse(this::loadProjectWithExperiments, () -> {
          throw new ApplicationException("Project information could not be retrieved from service");
        });
  }

  private void loadProjectWithExperiments(Project project) {
    var experiments = experimentInformationService.findAllForProject(project.getId());
    setProjectInformation(project, experiments);
  }

  private void setProjectInformation(Project project, List<Experiment> experiments) {
    resetProjectInformation();

    fillInformationSection(projectInformationSection,
        extractProjectInfo(project, experiments));
    fillInformationSection(fundingInformationSection,
        extractFundingInfo(project));
    fillInformationSection(collaboratorSection,
        extractContactInfo(project));
  }

  private void fillInformationSection(InformationComponent section,
      List<Entry> entries) {
    entries.forEach(section::add);
  }

  private void resetProjectInformation() {
    projectInformationSection.clearContent();
    fundingInformationSection.clearContent();
    collaboratorSection.clearContent();
    projectTitleField.removeAll();
    projectObjectiveField.removeAll();
    projectManagerField.removeAll();
    principalInvestigatorField.removeAll();
    responsiblePersonField.removeAll();
  }
}
