package life.qbic.datamanager.views.navigation;

import static life.qbic.datamanager.views.projects.project.info.ProjectInformationMain.EXPERIMENT_ID_ROUTE_PARAMETER;
import static life.qbic.datamanager.views.projects.project.info.ProjectInformationMain.PROJECT_ID_ROUTE_PARAMETER;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.function.SerializableBiConsumer;
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

@SpringComponent
@UIScope
public class ProjectNavigationDrawer extends Div implements BeforeEnterObserver {

  private final Div projectSection = new Div();
  private final SideNavItem projectSectionHeader = new SideNavItem("");
  private final Div experimentSection = new Div();
  private final SideNavItem experimentSectionHeader = new SideNavItem("");
  private final transient ProjectInformationService projectInformationService;
  private final transient ExperimentInformationService experimentInformationService;
  private final Select<Project> projectSelect = new Select<>();

  public ProjectNavigationDrawer(@Autowired ProjectInformationService projectInformationService,
      @Autowired
      ExperimentInformationService experimentInformationService) {
    Objects.requireNonNull(projectInformationService);
    Objects.requireNonNull(experimentInformationService);
    this.addClassName("project-navigation-drawer");
    this.projectInformationService = projectInformationService;
    this.experimentInformationService = experimentInformationService;
    projectSection.addClassName("project-section");
    experimentSection.addClassName("experiment-section");
    add(projectSection, experimentSection);
  }

  @Override
  public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
    resetDrawers();
    Optional<String> projectIdOpt = beforeEnterEvent.getRouteParameters()
        .get(PROJECT_ID_ROUTE_PARAMETER);
    if (projectIdOpt.isEmpty()) {
      return;
    }
    ProjectId projectId = ProjectId.parse(projectIdOpt.get());
    initializeProjectDrawerSection(projectId);
    initializeExperimentDrawerSection(projectId);
    Optional<String> experimentIdOpt = beforeEnterEvent.getRouteParameters()
        .get(EXPERIMENT_ID_ROUTE_PARAMETER);
    if (experimentIdOpt.isEmpty()) {
      return;
    }
    ExperimentId parsedExperimentId = ExperimentId.parse(experimentIdOpt.get());
  }


  //ToDo Load Project Information in Combobox
  private void initializeProjectDrawerSection(ProjectId projectId) {
    projectSectionHeader.setLabel("PROJECT");
    projectSectionHeader.setPrefixComponent(VaadinIcon.BOOK.create());
    projectSectionHeader.addClassName("primary");
    initProjectSelect(projectId);
    String projectInformationPath = String.format(Projects.PROJECT_INFO, projectId.value());
    String projectAccessPath = String.format(Projects.ACCESS, projectId.value());
    SideNavItem projectInformationItem = new SideNavItem("PROJECT INFORMATION",
        projectInformationPath, VaadinIcon.DEINDENT.create());
    SideNavItem projectAccessItem = new SideNavItem("PROJECT ACCESS MANAGEMENT", projectAccessPath,
        VaadinIcon.USERS.create());
    projectSection.addComponentAsFirst(projectSectionHeader);
    projectSection.add(projectSelect, new Hr(), projectInformationItem,
        projectAccessItem);
  }


  //Todo add last modified projects
  private void initProjectSelect(ProjectId projectId) {
    projectSelect.removeAll();
    projectSelect.setEmptySelectionCaption("Go To Projects");
    Span routeToOverView = new Span("Go To Projects");
    routeToOverView.addClassName("overview-route");
    routeToOverView.addClickListener(
        spanClickEvent -> UI.getCurrent().navigate(ProjectOverviewPage.class));
    Optional<Project> projectOptional = projectInformationService.find(projectId);
    projectOptional.ifPresent(projectSelect::setValue);
    projectOptional.ifPresent(projectSelect::setItems);
    projectSelect.setItemLabelGenerator(
        project -> project.getProjectIntent().projectTitle().title());
    projectSelect.addComponentAsFirst(routeToOverView);
    projectSelect.addComponentAtIndex(1, new Hr());
    Span recentProjectsHeader = new Span("Recent Projects");
    recentProjectsHeader.addClassName("recent-projects-header");
    projectSelect.addComponentAtIndex(2, recentProjectsHeader);
    projectSelect.setRenderer(projectComponentRenderer());
  }

  private static ComponentRenderer<Div, Project> projectComponentRenderer() {
    return new ComponentRenderer<>(Div::new, styleProjectItem);
  }

  private static final SerializableBiConsumer<Div, Project> styleProjectItem = (div, project) -> {
    div.addClassName("project-item");
    Span projectCodeSpan = new Span(project.getProjectCode().value());
    projectCodeSpan.addClassName("project-item-code");
    Span projectTitle = new Span(project.getProjectIntent().projectTitle().title());
    projectTitle.addClassName("project-item-title");
    div.add(projectCodeSpan, projectTitle);
  };

  private void initializeExperimentDrawerSection(ProjectId projectId) {
    experimentSectionHeader.removeAll();
    experimentSectionHeader.setLabel("Experiments");
    experimentSectionHeader.setPrefixComponent(VaadinIcon.FLASK.create());
    experimentSectionHeader.addClassName("primary");
    List<Experiment> experiments = experimentInformationService.findAllForProject(projectId);
    experiments.forEach(
        experiment -> experimentSectionHeader.addItem(createExperimentItem(projectId, experiment)));
    experimentSection.addComponentAsFirst(experimentSectionHeader);
  }

  private SideNavItem createExperimentItem(ProjectId projectId, Experiment experiment) {
    String experimentPath = String.format(Projects.EXPERIMENT, projectId.value(),
        experiment.experimentId().value());
    return new SideNavItem(experiment.getName(), experimentPath);
  }

  private void resetDrawers() {
    projectSection.removeAll();
    experimentSection.removeAll();
  }

}
