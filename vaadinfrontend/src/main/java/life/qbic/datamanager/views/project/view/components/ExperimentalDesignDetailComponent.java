package life.qbic.datamanager.views.project.view.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.security.PermitAll;
import life.qbic.datamanager.views.layouts.CardLayout;
import life.qbic.datamanager.views.project.experiment.ExperimentCreationDialog;
import life.qbic.datamanager.views.project.view.ProjectViewPage;
import life.qbic.projectmanagement.application.ExperimentInformationService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <b>ExperimentalDesignDetailComponent</b>
 * <p>
 * This component shows the {@link life.qbic.projectmanagement.domain.project.experiment.ExperimentalDesign} information associated with the selected {@link life.qbic.projectmanagement.domain.project.Project}
 * within the {@link life.qbic.datamanager.views.project.view.ProjectViewPage}
 *
 */
@SpringComponent
@UIScope
@Route(value = "projects/:projectId?/experiments", layout = ProjectViewPage.class)
@PermitAll
public class ExperimentalDesignDetailComponent extends Composite<CardLayout> {

  @Serial
  private static final long serialVersionUID = -2255999216830849632L;
  private static final String TITLE = "Experimental Design";
  private final Button createDesignButton = new Button("Add");
  private final ExperimentCreationDialog experimentCreationDialog;
  private final transient Handler handler;
  private final VirtualList<Experiment> experiments = new VirtualList<>();
  private final CardLayout experimentalDesignAddCard = new ExperimentalDesignAddCard();
  private final ComponentRenderer<Component, Experiment> experimentCardRenderer = new ComponentRenderer<>(
      ExperimentalDesignCard::new);

  public ExperimentalDesignDetailComponent(
      @Autowired ExperimentCreationDialog experimentCreationDialog,
      @Autowired ProjectInformationService projectInformationService,
      @Autowired ExperimentInformationService experimentInformationService) {

    Objects.requireNonNull(experimentCreationDialog);
    Objects.requireNonNull(projectInformationService);
    Objects.requireNonNull(experimentInformationService);
    this.experimentCreationDialog = experimentCreationDialog;
    VerticalLayout contentLayout = new VerticalLayout();
    contentLayout.add(experiments);
    contentLayout.add(experimentalDesignAddCard);
    getContent().addTitle(TITLE);
    getContent().addFields(contentLayout);
    this.handler = new Handler(projectInformationService, experimentInformationService);
  }

  public void setStyles(String... componentStyles) {
    getContent().addClassNames(componentStyles);
  }

  public void projectId(String parameter) {
    this.handler.setProjectId(ProjectId.parse(parameter));
  }


  /**
   * Component logic for the {@link ExperimentalDesignDetailComponent}
   *
   * @since 1.0.0
   */
  private final class Handler {

    private final ProjectInformationService projectInformationService;
    private final ExperimentInformationService experimentInformationService;
    private ProjectId projectId;

    public Handler(ProjectInformationService projectInformationService,
        ExperimentInformationService experimentInformationService) {
      this.projectInformationService = projectInformationService;
      this.experimentInformationService = experimentInformationService;
      openDialogueListener();
      experiments.setRenderer(experimentCardRenderer);
    }

    public void setProjectId(ProjectId projectId) {
      this.projectId = projectId;
      CallbackDataProvider<Experiment, Void> experimentsDataProvider = DataProvider.fromCallbacks(
          query -> loadExperimentsFromProjectId(projectId).stream()
              .skip(query.getOffset()).limit(query.getLimit()),
          query -> loadExperimentsFromProjectId(projectId).size());
      experiments.setDataProvider(experimentsDataProvider);
    }

    private List<Experiment> loadExperimentsFromProjectId(ProjectId projectId) {
      List<ExperimentId> projectExperimentIds = projectInformationService.loadProject(projectId)
          .experiments();
      List<Experiment> projectExperiments = new ArrayList<>();
      projectExperimentIds.forEach(experimentId -> projectExperiments.add(
          experimentInformationService.loadExperimentById(experimentId)));
      return projectExperiments;
    }

    private void openDialogueListener() {
      createDesignButton.addClickListener(clickEvent -> experimentCreationDialog.open(projectId));
      experimentalDesignAddCard.addClickListener(
          clickEvent -> experimentCreationDialog.open(projectId));
      experimentCreationDialog.addOpenedChangeListener(
          event -> {
            if (!event.isOpened()) {
              experiments.getDataProvider().refreshAll();
            }
          });
    }
  }

}
