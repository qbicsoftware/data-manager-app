package life.qbic.datamanager.views.projects.project.experiments;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.annotation.security.PermitAll;
import life.qbic.datamanager.views.layouts.CardLayout;
import life.qbic.datamanager.views.projects.project.ProjectViewPage;
import life.qbic.projectmanagement.application.ExperimentInformationService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <b>ExperimentListComponent</b>
 * <p>
 * This component lists the {@link life.qbic.projectmanagement.domain.project.experiment.Experiment}
 * associated with the selected {@link life.qbic.projectmanagement.domain.project.Project} within
 * the {@link ProjectViewPage}
 */
@SpringComponent
@UIScope
@PermitAll
public class ExperimentListComponent extends Composite<CardLayout> {

  @Serial
  private static final long serialVersionUID = -2255999216830849632L;
  private static final String TITLE = "Experimental Design";
  private final transient Handler handler;
  private final VirtualList<Experiment> experiments = new VirtualList<>();
  private final CardLayout experimentalDesignAddCard = new ExperimentalDesignAddCard();
  private final ComponentRenderer<Component, Experiment> experimentCardRenderer = new ComponentRenderer<>(
      ExperimentalDesignCard::new);

  public ExperimentListComponent(
      @Autowired ProjectInformationService projectInformationService,
      @Autowired ExperimentInformationService experimentInformationService) {
    Objects.requireNonNull(projectInformationService);
    Objects.requireNonNull(experimentInformationService);
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

  public void projectId(ProjectId projectId) {
    this.handler.setProjectId(projectId);
  }


  /**
   * Component logic for the {@link ExperimentListComponent}
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
      experiments.setRenderer(experimentCardRenderer);
    }

    public void setProjectId(ProjectId projectId) {
      this.projectId = projectId;
      projectInformationService.find(projectId)
          .ifPresent(this::setExperimentDataProviderFromProject);
    }

    private void setExperimentDataProviderFromProject(Project project) {
      CallbackDataProvider<Experiment, Void> experimentsDataProvider = DataProvider.fromCallbacks(
          query -> getExperimentsForProject(project).stream().skip(query.getOffset())
              .limit(query.getLimit()), query -> getExperimentsForProject(project).size());
      experiments.setDataProvider(experimentsDataProvider);
    }

    private Collection<Experiment> getExperimentsForProject(Project project) {
      List<Experiment> experimentList = new ArrayList<>();
      project.experiments().forEach(
          experimentId -> experimentInformationService.find(experimentId.value())
              .ifPresent(experimentList::add));
      return experimentList;
    }
  }

}
