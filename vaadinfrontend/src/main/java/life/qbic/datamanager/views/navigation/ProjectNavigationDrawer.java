package life.qbic.datamanager.views.navigation;

import static life.qbic.datamanager.views.projects.project.info.ProjectInformationMain.EXPERIMENT_ID_ROUTE_PARAMETER;
import static life.qbic.datamanager.views.projects.project.info.ProjectInformationMain.PROJECT_ID_ROUTE_PARAMETER;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import life.qbic.datamanager.views.AppRoutes.Projects;
import life.qbic.datamanager.views.projects.overview.ProjectOverviewPage;
import life.qbic.projectmanagement.application.ExperimentInformationService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <class short description - One Line!>
 * <p>
 * <More detailed description - When to use, what it solves, etc.>
 *
 * @since <version tag>
 */

@Tag("NavigationDrawer")
@SpringComponent
@UIScope
public class ProjectNavigationDrawer extends Div implements BeforeEnterObserver {

  private final Div projectDrawerSection = new Div();
  private final SideNavItem projectDrawerTitle = new SideNavItem("");
  private final SideNavItem experimentsDrawerSection = new SideNavItem("");
  private final transient ProjectInformationService projectInformationService;
  private final transient ExperimentInformationService experimentInformationService;
  private final Select<Project> projectSelect = new Select<>();

  public ProjectNavigationDrawer(@Autowired ProjectInformationService projectInformationService,
      @Autowired
      ExperimentInformationService experimentInformationService) {
    Objects.requireNonNull(projectInformationService);
    Objects.requireNonNull(experimentInformationService);
    this.projectInformationService = projectInformationService;
    this.experimentInformationService = experimentInformationService;
    projectDrawerSection.addClassName("project-section");
    experimentsDrawerSection.addClassName("experiment-section");
    addDrawers();
  }

  @Override
  public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
    removeDrawers();
    Optional<String> projectIdOpt = beforeEnterEvent.getRouteParameters()
        .get(PROJECT_ID_ROUTE_PARAMETER);
    if (projectIdOpt.isEmpty()) {
      return;
    }
    ProjectId projectId = ProjectId.parse(projectIdOpt.get());
    initializeProjectDrawerSection(projectId);
    initializeExperimentDrawerSection(projectId);
    addDrawers();
    Optional<String> experimentIdOpt = beforeEnterEvent.getRouteParameters()
        .get(EXPERIMENT_ID_ROUTE_PARAMETER);
    if (experimentIdOpt.isEmpty()) {
      return;
    }
    ExperimentId parsedExperimentId = ExperimentId.parse(experimentIdOpt.get());
  }


  //ToDo Load Project Information in Combobox
  private void initializeProjectDrawerSection(ProjectId projectId) {
    initProjectSelect(projectId);
    projectDrawerTitle.setLabel("PROJECT");
    projectDrawerTitle.setPrefixComponent(VaadinIcon.BOOK.create());
    String projectInformationPath = String.format(Projects.PROJECT_INFO, projectId.value());
    String projectAccessPath = String.format(Projects.ACCESS, projectId.value());
    SideNavItem projectInformationItem = new SideNavItem("PROJECT INFORMATION",
        projectInformationPath, VaadinIcon.DEINDENT.create());
    SideNavItem projectAccessItem = new SideNavItem("PROJECT ACCESS MANAGEMENT", projectAccessPath,
        VaadinIcon.USERS.create());
    projectDrawerSection.add(projectDrawerTitle, projectSelect, projectInformationItem,
        projectAccessItem);
  }

  private void initProjectSelect(ProjectId projectId) {
    projectSelect.clear();
    Span routeToOverView = new Span("Select your project");
    projectSelect.setEmptySelectionAllowed(true);
    routeToOverView.addClickListener(
        spanClickEvent -> UI.getCurrent().navigate(ProjectOverviewPage.class));
    projectSelect.addComponentAsFirst(new Div(routeToOverView));
    projectSelect.addComponentAtIndex(1, new Hr());
    projectSelect.setValue(projectInformationService.find(projectId).orElse(null));
  }

  private void initializeExperimentDrawerSection(ProjectId projectId) {
    experimentsDrawerSection.setLabel("Experiments");
    experimentsDrawerSection.setPrefixComponent(VaadinIcon.FLASK.create());
    List<Experiment> experiments = experimentInformationService.findAllForProject(projectId);
    experiments.forEach(experiment -> {
      String experimentPath = String.format(Projects.EXPERIMENT, projectId.value(),
          experiment.experimentId().value());
      SideNavItem experimentItem = new SideNavItem(experiment.getName(), experimentPath,
          VaadinIcon.FLASK.create());
      experimentsDrawerSection.addItem(experimentItem);
    });
  }

  private void removeDrawers() {
    projectDrawerSection.removeAll();
    experimentsDrawerSection.removeAll();
  }

  private void addDrawers() {
    add(projectDrawerSection, experimentsDrawerSection);
  }
}
