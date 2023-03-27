package life.qbic.datamanager.views.project.view.sample;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;
import life.qbic.datamanager.views.layouts.CardLayout;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.SampleInformationService;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;
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
public class SampleOverviewComponent extends Composite<CardLayout> implements Serializable {

  @Serial
  private static final long serialVersionUID = 2893730975944372088L;
  private final transient SampleOverviewComponentHandler sampleOverviewComponentHandler;
  private final VirtualList<Experiment> samples = new VirtualList<>();

  public SampleOverviewComponent(@Autowired ProjectInformationService projectInformationService,
      @Autowired SampleInformationService sampleInformationService) {
    Objects.requireNonNull(projectInformationService);
    Objects.requireNonNull(sampleInformationService);
    this.sampleOverviewComponentHandler = new SampleOverviewComponentHandler(
        projectInformationService, sampleInformationService);
  }

  public void projectId(String parameter) {
    this.sampleOverviewComponentHandler.setProjectId(ProjectId.parse(parameter));
  }

  public void setStyles(String... componentStyles) {
    getContent().addClassNames(componentStyles);
  }

  private final class SampleOverviewComponentHandler {

    private final ProjectInformationService projectInformationService;
    private final SampleInformationService sampleInformationService;
    private ProjectId projectId;

    public SampleOverviewComponentHandler(ProjectInformationService projectInformationService,
        SampleInformationService sampleInformationService) {
      this.projectInformationService = projectInformationService;
      this.sampleInformationService = sampleInformationService;
    }

    public void setProjectId(ProjectId projectId) {
      this.projectId = projectId;
      projectInformationService.find(projectId.value())
          .ifPresentOrElse(this::setSampleDataProviderFromProject, this::emptyAction);
    }

    private void setSampleDataProviderFromProject(Project project) {
      CallbackDataProvider<Sample, Void> sampleDataProvider = DataProvider.fromCallbacks(
          query -> getSamplesForProject(project).stream().skip(query.getOffset())
              .limit(query.getLimit()), query -> getSamplesForProject(project).size());
      samples.setDataProvider(sampleDataProvider);
    }

    private Collection<Sample> getSamplesForProject(Project project) {
      return;
    }

    //ToDo what should happen in the UI if neither projects or samples have been found?
    private void emptyAction() {
    }
  }

}
