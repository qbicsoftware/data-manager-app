package life.qbic.datamanager.views.projects.project.measurements;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.util.List;
import java.util.Objects;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentMainLayout;
import life.qbic.datamanager.views.projects.project.measurements.MeasurementTemplateListComponent.MeasurementTemplate;
import life.qbic.datamanager.views.projects.project.samples.SampleInformationMain;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.ExperimentInformationService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Measurement Main Component
 * <p>
 * This component hosts the components necessary to show and update the Measurement information
 * associated with an {@link Experiment} within a {@link Project} via the provided
 * {@link ExperimentId} and {@link ProjectId} in the URL
 */

@SpringComponent
@UIScope
@Route(value = "projects/:projectId?/experiments/:experimentId?/measurements", layout = ExperimentMainLayout.class)
@PermitAll
public class MeasurementMain extends Main implements BeforeEnterObserver {

  @Serial
  private static final long serialVersionUID = 3778218989387044758L;
  private static final Logger log = LoggerFactory.logger(SampleInformationMain.class);
  public static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  public static final String EXPERIMENT_ID_ROUTE_PARAMETER = "experimentId";
  private final transient ProjectInformationService projectInformationService;
  private final transient ExperimentInformationService experimentInformationService;
  private final transient MeasurementTemplateService measurementTemplateService;
  private final MeasurementTemplateListComponent measurementTemplateListComponent;
  private transient Context context;

  public MeasurementMain(@Autowired ProjectInformationService projectInformationService,
      @Autowired ExperimentInformationService experimentInformationService,
      @Autowired MeasurementTemplateService measurementTemplateService,
      @Autowired MeasurementTemplateListComponent measurementTemplateListComponent) {
    Objects.requireNonNull(projectInformationService);
    Objects.requireNonNull(experimentInformationService);
    Objects.requireNonNull(measurementTemplateService);
    Objects.requireNonNull(measurementTemplateListComponent);
    this.projectInformationService = projectInformationService;
    this.experimentInformationService = experimentInformationService;
    this.measurementTemplateService = measurementTemplateService;
    this.measurementTemplateListComponent = measurementTemplateListComponent;
    addClassName("measurement");
    add(measurementTemplateListComponent);
    log.debug(String.format(
        "New instance for %s(#%s) created with %s(#%s)",
        getClass().getSimpleName(), System.identityHashCode(this),
        measurementTemplateListComponent.getClass().getSimpleName(),
        System.identityHashCode(measurementTemplateListComponent)));
  }

  /**
   * Callback executed before navigation to attaching Component chain is made.
   *
   * @param event before navigation event with event details
   */
  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    String projectID = event.getRouteParameters().get(PROJECT_ID_ROUTE_PARAMETER)
        .orElseThrow();
    if (!ProjectId.isValid(projectID)) {
      throw new ApplicationException("invalid project id " + projectID);
    }
    ProjectId parsedProjectId = ProjectId.parse(projectID);
    context = new Context().with(parsedProjectId);
    String experimentId = event.getRouteParameters().get(EXPERIMENT_ID_ROUTE_PARAMETER)
        .orElseThrow();
    if (!ExperimentId.isValid(experimentId)) {
      throw new ApplicationException("invalid experiment id " + experimentId);
    }
    ExperimentId parsedExperimentId = ExperimentId.parse(experimentId);
    this.context = context.with(parsedExperimentId);
    setContext(context);
  }

  public void setContext(Context context) {
    this.context = context;
    List<MeasurementTemplate> templates = measurementTemplateService.getMeasurementTemplates();
    measurementTemplateListComponent.setMeasurementTemplates(templates);
  }

  @Service
  public static final class MeasurementTemplateService {

    public MeasurementTemplateService() {

    }

    public List<MeasurementTemplate> getMeasurementTemplates() {
      return List.of(new MeasurementTemplate(1, "Genomics.xlsx"),
          new MeasurementTemplate(2, "Imaging.xlsx"), new MeasurementTemplate(3, "Proteomics.xlsx"),
          new MeasurementTemplate(4, "Immunopeptidomics.xlsx"));
    }
  }
}
