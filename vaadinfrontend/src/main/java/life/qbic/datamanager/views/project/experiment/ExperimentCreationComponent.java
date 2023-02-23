package life.qbic.datamanager.views.project.experiment;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.exceptionhandlers.ApplicationExceptionHandler;
import life.qbic.datamanager.views.layouts.CardLayout;
import life.qbic.projectmanagement.domain.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@SpringComponent
@UIScope
public class ExperimentCreationComponent extends Composite<CardLayout> {

  private Handler componentHandler;

  private List<Div> experimentalVariables = new ArrayList<>();


  @Autowired
  public ExperimentCreationComponent(ApplicationExceptionHandler applicationExceptionHandler) {
    Objects.requireNonNull(applicationExceptionHandler);
    this.componentHandler = new Handler(applicationExceptionHandler);
    getContent().addFields(experimentalVariables.toArray());
  }

  public void setProjectContext(ProjectId projectId) {
    componentHandler.setProjectContext(projectId);
  }

  private class Handler {

    Optional<ProjectId> projectId;

    ApplicationExceptionHandler applicationExceptionHandler;

    Handler(ApplicationExceptionHandler applicationExceptionHandler) {
      this.applicationExceptionHandler = applicationExceptionHandler;
      this.projectId = Optional.empty();

    }

    void setProjectContext(ProjectId projectId) {
      if (projectId == null) {
        warnAboutMissingProjectContext();
      }
      this.projectId = Optional.of(projectId);
    }

    private void warnAboutMissingProjectContext() {
      throw new ApplicationException() {
        @Override
        public ErrorCode errorCode() {
          return ErrorCode.MISSING_PROJECT_CONTEXT;
        }
      };
    }

  }


}
