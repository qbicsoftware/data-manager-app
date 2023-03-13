package life.qbic.datamanager.views.project.view.components;

import java.util.List;
import life.qbic.datamanager.views.project.view.components.ExperimentalDesignCard.Experiment;
import life.qbic.datamanager.views.project.view.components.ExperimentalDesignDetailComponent.ExperimentInformationService;
import life.qbic.projectmanagement.domain.project.ProjectId;

/**
 * <class short description - One Line!>
 * <p>
 * <More detailed description - When to use, what it solves, etc.>
 *
 * @since <version tag>
 */
@org.springframework.stereotype.Component
public class ExperimentInformationServiceImpl implements ExperimentInformationService {

  @Override
  public List<Experiment> listExperimentsWithProject(ProjectId projectId) {
    return List.of(new Experiment("E1", "Title_1", "Description1"),
        new Experiment("E2", "Title_2", "Description2"));
  }
}
