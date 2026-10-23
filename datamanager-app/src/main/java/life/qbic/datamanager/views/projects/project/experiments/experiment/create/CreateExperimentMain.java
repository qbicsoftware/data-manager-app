package life.qbic.datamanager.views.projects.project.experiments.experiment.create;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.Result;
import life.qbic.datamanager.security.UserPermissions;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.datamanager.views.notifications.Toast;
import life.qbic.datamanager.views.projects.project.ProjectMainLayout;
import life.qbic.datamanager.views.projects.project.experiments.experiment.ExperimentDesignForm;
import life.qbic.datamanager.views.projects.project.info.ProjectInformationMain;
import life.qbic.projectmanagement.application.AddExperimentToProjectService;
import life.qbic.projectmanagement.application.ontology.SpeciesLookupService;
import life.qbic.projectmanagement.application.ontology.TerminologyService;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Experiment creation surface.
 *
 * <p>Hosts the experiment design form inline at {@code projects/:projectId/experiments/create}
 * instead of opening a dialog (project owner, 2026-10-19). On success it routes to the experiment's
 * landing.</p>
 *
 * @since 1.19.0
 */
@SpringComponent
@UIScope
@Route(value = "projects/:projectId?/experiments/create", layout = ProjectMainLayout.class)
@PermitAll
public class CreateExperimentMain extends Main implements BeforeEnterObserver {

  @Serial
  private static final long serialVersionUID = 1L;

  public static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  public static final String EXPERIMENT_ID_ROUTE_PARAMETER = "experimentId";

  private final transient AddExperimentToProjectService addExperimentToProjectService;
  private final transient UserPermissions userPermissions;
  private final transient MessageSourceNotificationFactory notificationFactory;
  private final ExperimentDesignForm designForm;

  private ProjectId projectId;

  public CreateExperimentMain(
      @Autowired AddExperimentToProjectService addExperimentToProjectService,
      @Autowired UserPermissions userPermissions,
      @Autowired SpeciesLookupService speciesLookupService,
      @Autowired TerminologyService terminologyService,
      @Autowired MessageSourceNotificationFactory notificationFactory) {
    this.addExperimentToProjectService = requireNonNull(addExperimentToProjectService);
    this.userPermissions = requireNonNull(userPermissions);
    this.notificationFactory = requireNonNull(notificationFactory);
    this.designForm = new ExperimentDesignForm(speciesLookupService, terminologyService);
    addClassName("experiment-create");
    layout();
  }

  private void layout() {
    Div band = new Div();
    band.addClassName("experiment-create-band");

    // Same grammar as the edit page: the way back, then the view name. The primary action does NOT
    // follow the edit page's top-right placement: this page is a form submission, not a section
    // save. The edit page has four independent sections each saving its own fields, so a per-section
    // top-right action makes ownership legible there. Here one button submits the whole form, so it
    // belongs below it where web users look for a submit -- and where the field errors land, since a
    // refused submit marks fields the user has to go back to.
    Button back = new Button("Back to project", VaadinIcon.ARROW_LEFT.create());
    back.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    back.addClassName("experiment-design-back");
    back.addClickListener(event -> navigateToSummary());

    Span title = new Span("Create experiment");
    title.addClassName("experiment-create-title");

    Div titleBlock = new Div(back, title);
    titleBlock.addClassName("experiment-design-title-block");

    Span sectionTitle = new Span("Experiment metadata");
    sectionTitle.addClassName("experiment-design-section-title");
    Span purpose = new Span("The experiment name and its sample origins. The origins become the "
        + "controlled options in the sample-registration template, so sample annotations stay "
        + "consistent and use public ontology terms.");
    purpose.addClassName("experiment-design-section-purpose");
    Div sectionText = new Div(sectionTitle, purpose);
    sectionText.addClassName("experiment-design-section-text");

    Div sectionHeader = new Div(sectionText);
    sectionHeader.addClassName("experiment-design-section-header");

    // The action row is the last thing in the band, after the form it submits.
    band.add(titleBlock, sectionHeader, designForm, actions());
    add(band);
  }

  private Div actions() {
    Div actions = new Div();
    actions.addClassName("experiment-create-actions");
    // Sticky to the bottom of the viewport while the form is taller than the screen, so the submit
    // stays in reach without pushing the user past the fields. It is not fixed: once the form ends
    // the row settles at its natural place at the bottom of the content.
    actions.addClassName("experiment-create-actions--sticky");
    Button cancel = new Button("Cancel");
    cancel.addClickListener(event -> navigateToSummary());
    Button create = new Button("Create experiment");
    create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    create.addClickListener(event -> createExperiment());
    actions.add(cancel, create);
    return actions;
  }

  @Override
  public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
    String projectIdValue = beforeEnterEvent.getRouteParameters().get(PROJECT_ID_ROUTE_PARAMETER)
        .orElseThrow(() -> new ApplicationException("Missing project id in route"));
    if (!ProjectId.isValid(projectIdValue)) {
      throw new ApplicationException("invalid project id " + projectIdValue);
    }
    ProjectId parsedProjectId = ProjectId.parse(projectIdValue);
    if (!userPermissions.readProject(parsedProjectId)) {
      beforeEnterEvent.rerouteToError(NotFoundException.class);
      return;
    }
    this.projectId = parsedProjectId;
    designForm.reset();
  }

  private void createExperiment() {
    designForm.writeDraftIfValid().ifPresentOrElse(draft -> {
      Result<ExperimentId, RuntimeException> result = addExperimentToProjectService
          .addExperimentToProject(projectId, draft.getExperimentName(), draft.getSpecies(),
              draft.getSpecimens(), draft.getAnalytes());
      if (result.isValue()) {
        displaySuccess(draft.getExperimentName());
        // Back to the project summary, where the new experiment now appears in context. Creating an
        // experiment is a project-level action, so it returns to the project's page.
        navigateToSummary();
      } else {
        displayError();
      }
    }, this::displayValidationError);
  }

  /**
   * A refused submit used to do nothing at all: {@code ifPresent} skipped the body silently and the
   * only explanation was an error attached to a field further down the page. The field errors say
   * what is wrong, this says why the click appeared to have no effect, and the focus move takes the
   * user to the field to fix. The level is info, not warning: nothing failed, the form is simply not
   * finished yet.
   */
  private void displayValidationError() {
    Toast toast = notificationFactory.toast("experiment.created.incomplete",
        new Object[]{}, getLocale());
    toast.open();
    designForm.focusFirstInvalidField();
  }

  private void displaySuccess(String experimentName) {
    Toast toast = notificationFactory.toast("experiment.created.success",
        new Object[]{experimentName}, getLocale());
    toast.open();
  }

  private void displayError() {
    Toast toast = notificationFactory.toast("experiment.created.error",
        new Object[]{}, getLocale());
    toast.open();
  }

  private void navigateToSummary() {
    RouteParameters routeParameters = new RouteParameters(
        new RouteParam(PROJECT_ID_ROUTE_PARAMETER, projectId.value()));
    getUI().ifPresent(ui -> ui.navigate(ProjectInformationMain.class, routeParameters));
  }
}
