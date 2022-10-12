package life.qbic.datamanager.views.project.view.components;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
class ProjectDetailsHandler {

  private final ProjectDetailsComponent component;

  public ProjectDetailsHandler(ProjectDetailsComponent component) {
    this.component = component;
  }

  public void projectId(String projectId) {
    component.textField.setValue("Context: " + projectId);
  }

}
