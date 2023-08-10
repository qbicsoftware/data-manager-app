package life.qbic.datamanager.views.projects.project.info;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.projects.project.experiments.experiment.Tag;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.ExperimentInformationService;
import life.qbic.projectmanagement.application.PersonSearchService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.domain.project.PersonReference;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Species;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Specimen;
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

  private static final Logger log = logger(ProjectDetailsComponent.class);
  @Serial
  private static final long serialVersionUID = -5781313306040217724L;
  private final Div header = new Div();
  private final Span title = new Span();
  private final Span buttonBar = new Span();
  private final Div content = new Div();
  private final FormLayout formLayout = new FormLayout();
  private final Span projectTitle = new Span();
  private final Span projectObjective = new Span();
  private final Span experimentalDesign = new Span();
  private final Div species = new Div();
  private final Div specimens = new Div();
  private final Div analytes = new Div();
  private final Div projectManager = new Div();
  private final Div principalInvestigator = new Div();
  private final Div responsiblePerson = new Div();
  private final List<ComponentEventListener<ProjectEditEvent>> editListeners = new ArrayList<>();
  private Context context;
  private final transient ProjectInformationService projectInformationService;
  private final transient ExperimentInformationService experimentInformationService;
  private final transient PersonSearchService personSearchService;

  public ProjectDetailsComponent(@Autowired ProjectInformationService projectInformationService,
      @Autowired ExperimentInformationService experimentInformationService,
      @Autowired PersonSearchService personSearchService) {
    Objects.requireNonNull(projectInformationService);
    Objects.requireNonNull(experimentInformationService);
    Objects.requireNonNull(personSearchService);
    this.projectInformationService = projectInformationService;
    this.experimentInformationService = experimentInformationService;
    this.personSearchService = personSearchService;
    layoutComponent();
    addListenerForNewEditEvent();
    this.addClassName("project-details-component");
  }

  public void setContext(Context context) {
    context.projectId()
        .orElseThrow(() -> new ApplicationException("no project id in context " + context));
    this.context = context;
    loadProjectData(context.projectId().get());
  }

  private void layoutComponent() {
    this.add(header);
    title.setText("Project Information");
    header.addClassName("header");
    initButtonBar();
    header.add(title, buttonBar);
    title.addClassName("title");
    this.add(content);
    content.addClassName("details-content");
    initFormLayout();
  }

  private void initButtonBar() {
    Button editButton = new Button("Edit");
    editButton.addClickListener(event -> openProjectInformationDialog());
    buttonBar.add(editButton);
  }

  private void openProjectInformationDialog() {
    ProjectId projectId = context.projectId().orElseThrow();
    Optional<Project> project = projectInformationService.find(projectId);
    project.ifPresentOrElse(proj -> {
          ProjectInformationDialog projectInformationDialog = generateProjectInformationDialog(proj);
          addProjectInformationDialogListeners(projectId, projectInformationDialog);
          projectInformationDialog.open();
        }
        , () -> {
          throw new ApplicationException(
              "Project information could not be retrieved from service");
        });
  }

  private ProjectInformationDialog generateProjectInformationDialog(Project project) {
    PersonReference responsiblePerson = null;
    if (project.getResponsiblePerson().isPresent()) {
      responsiblePerson = project.getResponsiblePerson().get();
    }
    ProjectInformationDialog projectInformationDialog = ProjectInformationDialog.prefilled(
        personSearchService,
        project.getProjectIntent().projectTitle(), project.getProjectIntent().objective(),
        project.getProjectIntent().experimentalDesign(), project.getPrincipalInvestigator(),
        responsiblePerson, project.getProjectManager());
    projectInformationDialog.setConfirmButtonLabel("Save");
    return projectInformationDialog;
  }

  private void addProjectInformationDialogListeners(ProjectId projectId,
      ProjectInformationDialog projectInformationDialog) {
    projectInformationDialog.addCancelEventListener(
        experimentInformationDialogCancelEvent -> projectInformationDialog.close());
    projectInformationDialog.addConfirmEventListener(projectInformationDialogConfirmEvent -> {
      if (projectInformationDialog.isInputValid()) {
        ProjectInformationContent projectInformationContent = projectInformationDialogConfirmEvent.getSource()
            .content();
        updateProjectInformation(projectId, projectInformationContent);
        projectInformationDialog.close();
        fireEditEvent();
      }
    });
  }

  private void updateProjectInformation(ProjectId projectId,
      ProjectInformationContent projectInformationContent) {
    projectInformationService.updateTitle(projectId, projectInformationContent.projectTitle());
    projectInformationService.stateObjective(projectId,
        projectInformationContent.projectObjective());
    projectInformationService.describeExperimentalDesign(projectId,
        projectInformationContent.experimentalDesignDescription());
    projectInformationService.investigateProject(projectId,
        projectInformationContent.principalInvestigator());
    projectInformationService.manageProject(projectId,
        projectInformationContent.projectManager());
    projectInformationService.setResponsibility(projectId,
        projectInformationContent.responsiblePerson());
  }

  private void addListenerForNewEditEvent() {
    this.editListeners.add(event -> loadProjectData(event.projectId()));
  }

  private void fireEditEvent() {
    ProjectId projectId = context.projectId().orElseThrow();
    var editEvent = new ProjectEditEvent(this, projectId, true);
    editListeners.forEach(listener -> listener.onComponentEvent(editEvent));
  }

  private void initFormLayout() {
    species.addClassName("tag-collection");
    specimens.addClassName("tag-collection");
    analytes.addClassName("tag-collection");
    formLayout.addFormItem(projectTitle, "Project Title");
    formLayout.addFormItem(projectObjective, "Project Objective");
    formLayout.addFormItem(experimentalDesign, "Experimental Design");
    formLayout.addFormItem(species, "Species");
    formLayout.addFormItem(specimens, "Specimen");
    formLayout.addFormItem(analytes, "Analyte");
    formLayout.addFormItem(principalInvestigator, "Principal Investigator");
    formLayout.addFormItem(responsiblePerson, "Responsible Person");
    formLayout.addFormItem(projectManager, "Project Manager");
    // set form layout to only have one column (for any width)
    formLayout.setResponsiveSteps(new ResponsiveStep("0", 1));
    content.add(formLayout);
  }

  private void loadProjectData(ProjectId projectId) {
    projectInformationService.find(projectId).ifPresentOrElse(this::setProjectInformation, () -> {
      throw new ApplicationException("Project information could not be retrieved from service");
    });
  }

  private void setProjectInformation(Project project) {
    resetProjectInformation();
    projectTitle.setText(project.getProjectIntent().projectTitle().title());
    projectObjective.setText(project.getProjectIntent().objective().value());
    experimentalDesign.setText(project.getProjectIntent().experimentalDesign().value());
    principalInvestigator.add(generatePersonReference(project.getPrincipalInvestigator()));
    project.getResponsiblePerson().ifPresentOrElse(
        personReference -> responsiblePerson.add(generatePersonReference(personReference)),
        () -> responsiblePerson.add(createNoPersonAssignedSpan()));
    projectManager.add(generatePersonReference(project.getProjectManager()));
    setGroupedExperimentInformation(project.getId());
  }

  private void resetProjectInformation() {
    projectTitle.removeAll();
    projectObjective.removeAll();
    experimentalDesign.removeAll();
    species.removeAll();
    specimens.removeAll();
    analytes.removeAll();
    projectManager.removeAll();
    principalInvestigator.removeAll();
    responsiblePerson.removeAll();
  }

  private void setGroupedExperimentInformation(ProjectId projectId) {
    List<Experiment> experiments = experimentInformationService.findAllForProject(projectId);
    generateExperimentInformationTags(experiments);
  }

  private void generateExperimentInformationTags(List<Experiment> experiments) {
    TreeSet<String> speciesSet = new TreeSet<>();
    TreeSet<String> specimenSet = new TreeSet<>();
    TreeSet<String> analysisSet = new TreeSet<>();
    experiments.forEach(experiment -> {
      speciesSet.addAll(experiment.getSpecies().stream().map(Species::value).toList());
      specimenSet.addAll(experiment.getSpecimens().stream().map(Specimen::value).toList());
      analysisSet.addAll(experiment.getAnalytes().stream().map(Analyte::value).toList());
    });
    species.add(speciesSet.stream().map(Tag::new).collect(Collectors.toList()));
    specimens.add(specimenSet.stream().map(Tag::new).collect(Collectors.toList()));
    analytes.add(analysisSet.stream().map(Tag::new).collect(Collectors.toList()));
  }

  private Div generatePersonReference(PersonReference personReference) {
    Span nameSpan = new Span(personReference.fullName());
    Span emailSpan = new Span(personReference.emailAddress());
    Div personReferenceContainer = new Div(nameSpan, emailSpan);
    personReferenceContainer.addClassName("person-reference");
    emailSpan.addClassNames("email");
    nameSpan.addClassName("name");
    return personReferenceContainer;
  }

  private Span createNoPersonAssignedSpan() {
    Span noPersonAssignedSpan = new Span("None");
    noPersonAssignedSpan.addClassName("no-person-assigned");
    return noPersonAssignedSpan;
  }
}
