package life.qbic.datamanager.views.projects.project;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoUtility.IconSize;
import java.io.Serial;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.views.AppRoutes.Projects;
import life.qbic.datamanager.views.layouts.PageComponent;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

/**
 * ProjectNavigationBarComponent
 * <p>
 * Allows the user to switch between the components shown in the {@link ProjectViewPage} by clicking
 * on the corresponding button within the Navigation Bar which routes the user to the respective
 * route defined in {@link life.qbic.datamanager.views.AppRoutes} for the component in question
 */
@SpringComponent
@UIScope
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ProjectNavigationBarComponent extends Composite<PageComponent> {

  @Serial
  private static final long serialVersionUID = 2246439877362853798L;
  private final transient Handler handler;
  private HorizontalLayout navigationBarLayout;
  private Button projectInformationButton;
  private Button experimentsButton;
  private Button samplesButton;
  private Button rawDataButton;
  private Button resultsButton;

  public ProjectNavigationBarComponent() {
    this.handler = new Handler();
    initNavigationBar();
  }

  private void styleNavLabelLayout(VerticalLayout layout) {
    layout.setAlignItems(Alignment.CENTER);
    layout.setPadding(false);
  }

  private void initNavigationBar() {

    navigationBarLayout = new HorizontalLayout();
    //Todo Generate Custom Component containing Button, statusIndicator and label
    VerticalLayout projectInformationLayout = new VerticalLayout();
    Icon projectInformationIcon = VaadinIcon.CLIPBOARD_CHECK.create();
    projectInformationIcon.setSize(IconSize.LARGE);
    projectInformationButton = new Button(projectInformationIcon);
    Label projectInformationLabel = new Label("Project Information");
    projectInformationLayout.add(projectInformationButton, projectInformationLabel);
    styleNavLabelLayout(projectInformationLayout);

    VerticalLayout experimentalDesignLayout = new VerticalLayout();
    Label experimentsLabel = new Label("Experiments");
    Icon experimentsIcon = VaadinIcon.SITEMAP.create();
    experimentsButton = new Button(experimentsIcon);
    experimentsIcon.setSize(IconSize.LARGE);
    experimentalDesignLayout.add(experimentsButton, experimentsLabel);
    styleNavLabelLayout(experimentalDesignLayout);

    VerticalLayout samplesLayout = new VerticalLayout();
    Icon sampleIcon = VaadinIcon.FILE_TABLE.create();
    sampleIcon.setSize(IconSize.LARGE);
    samplesButton = new Button(sampleIcon);
    Label samplesLabel = new Label("Samples");
    samplesLayout.add(samplesButton, samplesLabel);
    styleNavLabelLayout(samplesLayout);

    VerticalLayout rawDataLayout = new VerticalLayout();
    Icon rawDataIcon = VaadinIcon.CLOUD_DOWNLOAD.create();
    rawDataIcon.setSize(IconSize.LARGE);
    rawDataButton = new Button(rawDataIcon);
    Label rawDataLabel = new Label("Raw Data");
    rawDataLayout.add(rawDataButton, rawDataLabel);
    styleNavLabelLayout(rawDataLayout);

    VerticalLayout resultsLayout = new VerticalLayout();
    Icon resultsIcon = VaadinIcon.SEARCH.create();
    resultsIcon.setSize(IconSize.LARGE);
    resultsButton = new Button(resultsIcon);
    Label resultsLabel = new Label("Results");
    resultsLayout.add(resultsButton, resultsLabel);
    styleNavLabelLayout(resultsLayout);

    navigationBarLayout.add(projectInformationLayout, experimentalDesignLayout, samplesLayout,
        rawDataLayout, resultsLayout);
    navigationBarLayout.setWidthFull();
    navigationBarLayout.setJustifyContentMode(JustifyContentMode.EVENLY);
    navigationBarLayout.setAlignItems(Alignment.CENTER);
    getContent().addContent(navigationBarLayout);
    getContent().removeTitle();
    projectInformationButton.addClickListener(
        ((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> getUI().ifPresentOrElse(
            it -> it.navigate(
                String.format(Projects.PROJECT_INFO, handler.selectedProject.value())), () -> {
              throw new ApplicationException(
                  "Could not navigate to Project Information Page for "
                      + handler.selectedProject.value());
            })));
    experimentsButton.addClickListener(
        ((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> getUI().ifPresentOrElse(
            it -> it.navigate(String.format(Projects.EXPERIMENT, handler.selectedProject.value(),
                handler.experimentId)),
            () -> {
              throw new ApplicationException(
                  "Could not navigate to Experiment Information Page for "
                      + handler.selectedProject.value());
            })));
    samplesButton.addClickListener(
        ((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> getUI().ifPresentOrElse(
            it -> it.navigate(String.format(Projects.SAMPLES, handler.selectedProject.value())),
            () -> {
              throw new ApplicationException(
                  "Could not navigate to Sample Information Page for "
                      + handler.selectedProject.value());
            })));
  }

  public void projectId(ProjectId projectId) {
    this.handler.setProjectId(projectId);
  }

  public void experimentId(ExperimentId experimentId) {
    this.handler.setExperimentId(experimentId);
  }

  public void setStyles(String... componentStyles) {
    getContent().addClassNames(componentStyles);
  }

  private final class Handler {

    private ProjectId selectedProject;
    private ExperimentId experimentId;

    public void setProjectId(ProjectId projectId) {
      this.selectedProject = projectId;
    }

    public void setExperimentId(ExperimentId experimentId) {
      this.experimentId = experimentId;
    }
  }
}
