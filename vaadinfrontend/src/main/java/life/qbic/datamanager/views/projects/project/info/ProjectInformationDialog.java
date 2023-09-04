package life.qbic.datamanager.views.projects.project.info;

import com.vaadin.flow.component.ComponentEventListener;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import life.qbic.datamanager.views.general.CancelEvent;
import life.qbic.datamanager.views.general.ConfirmEvent;
import life.qbic.datamanager.views.general.DialogWindow;
import life.qbic.datamanager.views.projects.create.DefineExperimentComponent;
import life.qbic.projectmanagement.application.PersonSearchService;
import life.qbic.projectmanagement.domain.project.ExperimentalDesignDescription;
import life.qbic.projectmanagement.domain.project.PersonReference;
import life.qbic.projectmanagement.domain.project.ProjectObjective;
import life.qbic.projectmanagement.domain.project.ProjectTitle;

/**
 * <b>ProjectInformationDialog</b>
 *
 * <p>Dialog to edit project information by providing the minimal required information
 * in the {@link DefineExperimentComponent}</p>
 *
 * @since 1.0.0
 */

public class ProjectInformationDialog extends
    DialogWindow { //FIXME not needed? use add dialog instead?

  @Serial
  private static final long serialVersionUID = 1735754381220518292L;
  private final DefineProjectComponent defineProjectComponent;
  private static final String TITLE = "Project Information";
  private final List<ComponentEventListener<CancelEvent<ProjectInformationDialog>>> cancelEventListeners = new ArrayList<>();
  private final List<ComponentEventListener<ConfirmEvent<ProjectInformationDialog>>> confirmEventListeners = new ArrayList<>();
  private final MODE mode;
  private final transient PersonSearchService personSearchService;

  public ProjectInformationDialog(PersonSearchService personSearchService) {
    this(personSearchService, false);
  }

  private ProjectInformationDialog(PersonSearchService personSearchService, boolean mode) {
    super();
    this.personSearchService = personSearchService;
    this.mode = mode ? MODE.EDIT : MODE.ADD;
    addClassName("project-information-dialog");
    defineProjectComponent = new DefineProjectComponent(personSearchService);
    layoutComponent();
    configureComponent();
  }

  private void layoutComponent() {
    setHeaderTitle(TITLE);
    setConfirmButtonLabel("Add");
    setCancelButtonLabel("Cancel");
    add(defineProjectComponent);
    final DialogFooter footer = getFooter();
    footer.add(this.cancelButton, this.confirmButton);
  }

  private void configureComponent() {
    configureCancelling();
    configureConfirmation();
  }

  /**
   * Creates a new dialog prefilled with project information.
   *
   * @param personSearchService           {@link PersonSearchService} service to provide the selectable {@link PersonReference}
   *                                                                  within the dialog
   * @param projectTitle                  {@link ProjectTitle} of the project to be edited
   * @param projectObjective              {@link ProjectObjective} of the project to be edited
   * @param experimentalDesignDescription {@link ExperimentalDesignDescription} of the project to be
   *                                      edited
   * @param principalInvestigator         {@link PersonReference} of the principal investigator of
   *                                      the project to be edited
   * @param responsiblePerson             {@link PersonReference} of the responsible person to be
   *                                      contacted of the project to be edited
   * @param projectManager                {@link PersonReference} of the project manager of the
   *                                      project to be edited
   * @return a new instance of the dialog
   */

  public static ProjectInformationDialog prefilled(
      PersonSearchService personSearchService,
      ProjectTitle projectTitle, ProjectObjective projectObjective,
      PersonReference principalInvestigator, PersonReference responsiblePerson,
      PersonReference projectManager) {
    ProjectInformationDialog projectInformationDialog = new ProjectInformationDialog(
        personSearchService, true);
    projectInformationDialog.setProjectInformation(projectTitle, projectObjective,
        principalInvestigator, responsiblePerson, projectManager);
    return projectInformationDialog;
  }

  private void setProjectInformation(ProjectTitle projectTitle,
      ProjectObjective projectObjective,
      PersonReference principalInvestigator, PersonReference responsiblePerson,
      PersonReference projectManager) {
    defineProjectComponent.setProjectInformation(projectTitle, projectObjective,
        principalInvestigator, responsiblePerson, projectManager);
  }

  /**
   * Checks if any fields within the {@link ProjectInformationContent} of this dialog contains an
   * invalid value
   *
   * @return boolean indicating if any field of this component contains an invalid value
   */
  public boolean isInputValid() {
    return defineProjectComponent.isInputValid();
  }

  private void configureConfirmation() {
    this.confirmButton.addClickListener(event -> fireConfirmEvent());
  }

  private void configureCancelling() {
    this.cancelButton.addClickListener(cancelListener -> fireCancelEvent());
  }

  private void fireConfirmEvent() {
    if (isInputValid()) {
      this.confirmEventListeners.forEach(
          listener -> listener.onComponentEvent(new ConfirmEvent<>(this, true)));
    }
  }

  private void fireCancelEvent() {
    this.cancelEventListeners.forEach(
        listener -> listener.onComponentEvent(new CancelEvent<>(this, true)));
  }

  /**
   * Adds a listener for {@link ConfirmEvent}s
   *
   * @param listener the listener to add
   */
  public void addConfirmEventListener(
      final ComponentEventListener<ConfirmEvent<ProjectInformationDialog>> listener) {
    this.confirmEventListeners.add(listener);
  }

  /**
   * Adds a listener for {@link CancelEvent}s
   *
   * @param listener the listener to add
   */
  public void addCancelEventListener(
      final ComponentEventListener<CancelEvent<ProjectInformationDialog>> listener) {
    this.cancelEventListeners.add(listener);
  }

  /**
   * Provides the content set in the {@link DefineProjectComponent} of this dialog
   *
   * @return {@link ProjectInformationContent} providing the information filled by the user within
   * this dialog
   */
  public ProjectInformationContent content() {
    return defineProjectComponent.getProjectInformationContent();
  }

  private enum MODE {
    ADD, EDIT
  }
}
