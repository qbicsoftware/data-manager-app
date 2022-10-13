package life.qbic.datamanager.views.project.overview;

import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;

import java.util.Objects;
import java.util.Optional;

import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.Result;
import life.qbic.datamanager.exceptionhandlers.ApplicationExceptionHandler;
import life.qbic.datamanager.views.Command;
import life.qbic.datamanager.views.notifications.StyledNotification;
import life.qbic.datamanager.views.notifications.SuccessMessage;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.ProjectCreationService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.finances.offer.OfferLookupService;
import life.qbic.projectmanagement.domain.finances.offer.Offer;
import life.qbic.projectmanagement.domain.finances.offer.OfferId;
import life.qbic.projectmanagement.domain.finances.offer.OfferPreview;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static life.qbic.logging.service.LoggerFactory.logger;

/**
 * <b>Handler</b>
 *
 * <p>Orchestrates the layout {@link ProjectOverviewLayout} and determines how the components
 * behave.</p>
 *
 * @since 1.0.0
 */
@Component
public class ProjectOverviewHandler implements ProjectOverviewHandlerInterface {

  private static final Logger log = logger(ProjectOverviewHandler.class);
  private final ApplicationExceptionHandler exceptionHandler;

  private ProjectOverviewLayout registeredProjectOverview;
  private final OfferLookupService offerLookupService;
  private final ProjectCreationService projectCreationService;

  private final ProjectRepository projectRepository;

  private final ProjectInformationService projectInformationService;


  public ProjectOverviewHandler(@Autowired OfferLookupService offerLookupService,
                                @Autowired ProjectRepository projectRepository,
                                @Autowired ProjectInformationService projectInformationService,
                                @Autowired ProjectCreationService projectCreationService,
                                @Autowired ApplicationExceptionHandler exceptionHandler) {
    Objects.requireNonNull(offerLookupService);
    this.offerLookupService = offerLookupService;

    Objects.requireNonNull(projectRepository);
    this.projectRepository = projectRepository;

    Objects.requireNonNull(projectInformationService);
    this.projectInformationService = projectInformationService;

    Objects.requireNonNull(projectCreationService);
    this.projectCreationService = projectCreationService;

    Objects.requireNonNull(exceptionHandler);
    this.exceptionHandler = exceptionHandler;
  }

  @Override
  public void handle(ProjectOverviewLayout layout) {
    if (registeredProjectOverview != layout) {
      this.registeredProjectOverview = layout;

      configurePageButtons();
      configureProjectCreationDialog();
      loadOfferPreview();

      setProjectsToGrid();
      setupSearchBar();
    }
  }

  private void setupSearchBar() {
    registeredProjectOverview.projectSearchField.setValueChangeMode(ValueChangeMode.LAZY);
    registeredProjectOverview.projectSearchField
        .addValueChangeListener(event -> loadProjectPreview(event.getValue()).execute());
  }

  private Command loadProjectPreview() {
    var searchTerm = registeredProjectOverview.projectSearchField.getValue().trim();
    return loadProjectPreview(searchTerm);
  }

  private Command loadProjectPreview(String filter) {
    return () -> registeredProjectOverview.projectGrid.setItems(
        query -> projectInformationService.queryPreview(filter,
            query.getOffset(), query.getLimit()).stream());
  }

  private void setProjectsToGrid() {
    loadProjectPreview().execute();
  }

  private void configurePageButtons() {
    registeredProjectOverview.create.addClickListener(
        e -> registeredProjectOverview.projectInformationDialog.open());

  }

  private void configureProjectCreationDialog() {
    registeredProjectOverview.projectInformationDialog.createButton.addClickListener(
        e -> createClicked());
    registeredProjectOverview.projectInformationDialog.cancelButton.addClickListener(
        e -> cancelSelection().execute());
    registeredProjectOverview.projectInformationDialog.isCloseOnEsc();
  }

  private Command cancelSelection() {
    return () -> {
      registeredProjectOverview.projectInformationDialog.close();
      registeredProjectOverview.projectInformationDialog.reset();
    };
  }

  private void createClicked() {
    String titleFieldValue = registeredProjectOverview.projectInformationDialog.getTitle();
    String objectiveFieldValue = registeredProjectOverview.projectInformationDialog.getObjective();
    String experimentalDesignDescription = registeredProjectOverview.projectInformationDialog.getExperimentalDesign();

    String loadedOfferId = registeredProjectOverview.projectInformationDialog.searchField.getValue() != null ? registeredProjectOverview.projectInformationDialog.searchField.getValue().offerId().id() : null;

    Result<Project, ApplicationException> project = projectCreationService.createProject(
        titleFieldValue, objectiveFieldValue, experimentalDesignDescription, loadedOfferId);

    project.ifSuccessOrElse(
        result -> displaySuccessfulProjectCreationNotification(),
        applicationException -> exceptionHandler.handle(UI.getCurrent(), applicationException));

    registeredProjectOverview.projectInformationDialog.close();
    registeredProjectOverview.projectGrid.getDataProvider().refreshAll();
  }

  private void displaySuccessfulProjectCreationNotification() {
    SuccessMessage successMessage = new SuccessMessage("Project creation succeeded.", "");
    StyledNotification notification = new StyledNotification(successMessage);
    notification.open();
  }

  private void loadOfferPreview() {
    // Configure the filter and pagination for the lazy loaded OfferPreview items
    registeredProjectOverview.projectInformationDialog.searchField.setItems(
        query -> offerLookupService.findOfferContainingProjectTitleOrId(
            query.getFilter().orElse(""),
            query.getFilter().orElse(""), query.getOffset(), query.getLimit()).stream());

    // Render the preview
    registeredProjectOverview.projectInformationDialog.searchField.setRenderer(
        new ComponentRenderer<>(preview ->
            new Text(previewToString(preview))));

    // Generate labels like the rendering
    registeredProjectOverview.projectInformationDialog.searchField.setItemLabelGenerator(
        (ItemLabelGenerator<OfferPreview>) it -> it.offerId().id());

    registeredProjectOverview.projectInformationDialog.searchField.addValueChangeListener(e -> {
      if (registeredProjectOverview.projectInformationDialog.searchField.getValue() != null) {
        preloadContentFromOffer(registeredProjectOverview.projectInformationDialog.searchField.getValue().offerId().id());
      }
    });
  }

  private void preloadContentFromOffer(String offerId) {
    log.info("Receiving offerId " + offerId);
    OfferId id = OfferId.from(offerId);
    Optional<Offer> offer = offerLookupService.findOfferById(id);
    offer.ifPresentOrElse(it -> registeredProjectOverview.projectInformationDialog.setOffer(it),
        () -> log.error("No offer found with id: " + offerId));
  }

  /**
   * Render the preview like `#offer-id, #project title`
   *
   * @param offerPreview the offer preview
   * @return the formatted String representation
   * @since 1.0.0
   */
  private static String previewToString(OfferPreview offerPreview) {
    return offerPreview.offerId().id() + ", " + offerPreview.getProjectTitle().title();
  }


}
