package life.qbic.datamanager.views.project.view.components;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import life.qbic.datamanager.views.AppRoutes.Projects;
import life.qbic.datamanager.views.layouts.CardLayout;
import life.qbic.datamanager.views.project.view.ProjectViewPage;

/**
 * ProjectNavigationBarComponent
 * <p>
 * Allows the user to switch between the components shown in the {@link ProjectViewPage} by clicking
 * on the corresponding button within the Navigation Bar which routes the user to the respective
 * route defined in {@link life.qbic.datamanager.views.AppRoutes} for the component in question
 */
@SpringComponent
@UIScope
public class ProjectNavigationBarComponent extends Composite<CardLayout> {

  @Serial
  private static final long serialVersionUID = 2246439877362853798L;
  private final transient Handler handler;
  private HorizontalLayout navigationBarLayout;
  private Button projectInformationButton;
  private Button experimentalDesignButton;
  private Button samplesButton;
  private Button rawDataButton;
  private Button resultsButton;

  public ProjectNavigationBarComponent() {
    this.handler = new Handler();
    initNavigationBar();
  }

  private void initNavigationBar() {

    navigationBarLayout = new HorizontalLayout();
    //Todo Generate Custom Component containing Button, statusIndicator and label
    VerticalLayout projectInformationLayout = new VerticalLayout();
    projectInformationButton = new Button(VaadinIcon.CLIPBOARD_CHECK.create());
    Label projectInformationLabel = new Label("Project Information");
    projectInformationLayout.add(projectInformationButton, projectInformationLabel);
    projectInformationLayout.setAlignItems(Alignment.CENTER);

    VerticalLayout experimentalDesignLayout = new VerticalLayout();
    Label experimentalDesignLabel = new Label("Experimental Design");
    experimentalDesignButton = new Button(VaadinIcon.SITEMAP.create());
    experimentalDesignLayout.add(experimentalDesignButton, experimentalDesignLabel);
    experimentalDesignLayout.setAlignItems(Alignment.CENTER);

    VerticalLayout samplesLayout = new VerticalLayout();
    samplesButton = new Button(VaadinIcon.FILE_TABLE.create());
    Label samplesLabel = new Label("Samples");
    samplesLayout.add(samplesButton, samplesLabel);
    samplesLayout.setAlignItems(Alignment.CENTER);

    VerticalLayout rawDataLayout = new VerticalLayout();
    rawDataButton = new Button(VaadinIcon.CLOUD_DOWNLOAD.create());
    Label rawDataLabel = new Label("Raw Data");
    rawDataLayout.add(rawDataButton, rawDataLabel);
    rawDataLayout.setAlignItems(Alignment.CENTER);

    VerticalLayout resultsLayout = new VerticalLayout();
    resultsButton = new Button(VaadinIcon.SEARCH.create());
    Label resultsLabel = new Label("Results");
    resultsLayout.add(resultsButton, resultsLabel);
    resultsLayout.setAlignItems(Alignment.CENTER);

    navigationBarLayout.add(projectInformationLayout, experimentalDesignLayout, samplesLayout,
        rawDataLayout, resultsLayout);
    navigationBarLayout.setWidthFull();
    navigationBarLayout.setHeightFull();
    navigationBarLayout.setJustifyContentMode(JustifyContentMode.EVENLY);
    navigationBarLayout.setAlignItems(Alignment.CENTER);
    getContent().addFields(navigationBarLayout);

    experimentalDesignButton.addClickListener(
        ((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> getUI().get()
            .navigate(String.format(Projects.EXPERIMENTS, handler.selectedProject))));
    projectInformationButton.addClickListener(
        ((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> getUI().get()
            .navigate(String.format(Projects.PROJECT_INFO, handler.selectedProject))));
    projectInformationButton.addClickListener(
        ((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> getUI().get()
            .navigate(String.format(Projects.SAMPLES, handler.selectedProject))));
  }

  public void projectId(String projectId) {
    this.handler.setProjectId(projectId);
  }

  public void setStyles(String... componentStyles) {
    getContent().addClassNames(componentStyles);
  }

  private final class Handler {

    private String selectedProject;

    public void setProjectId(String projectId) {
      this.selectedProject = projectId;
    }

    public String selectedProjectId() {
      return this.selectedProject;
    }
  }
}
