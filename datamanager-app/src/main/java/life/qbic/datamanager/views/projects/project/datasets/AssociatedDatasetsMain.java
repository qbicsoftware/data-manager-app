package life.qbic.datamanager.views.projects.project.datasets;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import life.qbic.datamanager.security.UserPermissions;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.projects.project.ProjectMainLayout;
import life.qbic.projectmanagement.application.associated_dataset.AssociatedDatasetService;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <b>Associated Datasets view</b>
 *
 * <p>The main view for the "Connect associated datasets with Data Manager
 * projects" feature (FEAT-DATSET-01). Provides:
 * <ul>
 *   <li>a "Connected Resources" section showing the datasets currently
 *       connected to the selected project, or an empty-state guide when
 *       no datasets are connected yet;</li>
 *   <li>a sliding connect-datasets sidebar that searches InvenioRDM
 *       instances, lets the user pick results to connect, and optionally
 *       associates them with a project experiment.</li>
 * </ul>
 *
 * <p>Accessible from the project navigation drawer (DATASETS link, placed
 * directly below SUMMARY).</p>
 *
 * @since 1.12.0
 */
@SpringComponent
@UIScope
@Route(value = "projects/:projectId?/datasets", layout = ProjectMainLayout.class)
@PermitAll
public class AssociatedDatasetsMain extends Main implements BeforeEnterObserver {

  public static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";

  private final transient AssociatedDatasetService associatedDatasetService;
  private final transient ExperimentInformationService experimentInformationService;
  private final transient UserPermissions userPermissions;

  private final ConnectedResourcesComponent connectedResourcesComponent;
  private ConnectDatasetSidebar connectDatasetSidebar;

  private Context context = new Context();

  @Autowired
  public AssociatedDatasetsMain(
      AssociatedDatasetService associatedDatasetService,
      ExperimentInformationService experimentInformationService,
      UserPermissions userPermissions) {
    this.associatedDatasetService = requireNonNull(associatedDatasetService,
        "associatedDatasetService must not be null");
    this.experimentInformationService = requireNonNull(experimentInformationService,
        "experimentInformationService must not be null");
    this.userPermissions = requireNonNull(userPermissions,
        "userPermissions must not be null");

    addClassName("project");
    connectedResourcesComponent = new ConnectedResourcesComponent(associatedDatasetService);
    connectedResourcesComponent.addConnectDatasetsClickListener(e -> openConnectSidebar());
    add(connectedResourcesComponent);
  }

  @Override
  public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
    String projectId = beforeEnterEvent.getRouteParameters().get(PROJECT_ID_ROUTE_PARAMETER)
        .orElseThrow();
    var parsedProjectId =
        life.qbic.projectmanagement.domain.model.project.ProjectId.parse(projectId);
    if (!userPermissions.readProject(parsedProjectId)) {
      beforeEnterEvent.rerouteToError(NotFoundException.class);
      return;
    }
    this.context = new Context().with(parsedProjectId);
    setContext();
  }

  private void setContext() {
    connectedResourcesComponent.setContext(context);
    if (connectDatasetSidebar != null) {
      connectDatasetSidebar.setContext(context);
    }
  }

  private void openConnectSidebar() {
    if (connectDatasetSidebar == null) {
      connectDatasetSidebar = new ConnectDatasetSidebar(
          associatedDatasetService,
          experimentInformationService,
          userPermissions);
      connectDatasetSidebar.setContext(context);
      connectDatasetSidebar.addDatasetsConnectedListener(
          e -> connectedResourcesComponent.refresh());
      add(connectDatasetSidebar);
    }
    connectDatasetSidebar.open();
  }
}
