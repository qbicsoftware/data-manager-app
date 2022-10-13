package life.qbic.datamanager.views.project.overview;

import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.QueryParameters;
import java.util.Objects;
import life.qbic.datamanager.views.Command;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.finances.offer.OfferLookupService;
import life.qbic.projectmanagement.domain.finances.offer.OfferPreview;
import life.qbic.projectmanagement.domain.project.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

  private static final String PROJECT_CREATION_URL = "projects/create";

  private static final String OFFER_ID_QUERY_PARAMETER = "offerId";

  private static final String QUERY_PARAMETER_SEPARATOR = "=";
  private ProjectOverviewLayout registeredProjectOverview;
  private final OfferLookupService offerLookupService;
  private final ProjectRepository projectRepository;

  private final ProjectInformationService projectInformationService;

  private CreationMode creationMode = CreationMode.NONE;

  public ProjectOverviewHandler(@Autowired OfferLookupService offerLookupService,
      @Autowired ProjectRepository projectRepository,
      @Autowired ProjectInformationService projectInformationService) {
    Objects.requireNonNull(offerLookupService);
    this.offerLookupService = offerLookupService;

    Objects.requireNonNull(projectRepository);
    this.projectRepository = projectRepository;

    Objects.requireNonNull(projectInformationService);
    this.projectInformationService = projectInformationService;

    System.out.println();
  }

  @Override
  public void handle(ProjectOverviewLayout layout) {
    if (registeredProjectOverview != layout) {
      this.registeredProjectOverview = layout;
      configureSearchDropbox();
      configureSelectionModeDialog();
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

  private void configureSelectionModeDialog() {
    configureSelectionModeDialogFooterButtons();

    registeredProjectOverview.selectCreationModeDialog.blankButton.addClickListener(
        e -> creationMode = CreationMode.BLANK);
    registeredProjectOverview.selectCreationModeDialog.fromOfferButton.addClickListener(
        e -> creationMode = CreationMode.FROM_OFFER);
  }

  private void configureSelectionModeDialogFooterButtons() {
    registeredProjectOverview.selectCreationModeDialog.next.addClickListener(
        e -> navigateToProjectCreation().execute());
    registeredProjectOverview.selectCreationModeDialog.cancel.addClickListener(
        e -> cancelSelection().execute());
  }

  private Command navigateToProjectCreation() {
    return switch (creationMode) {
      case BLANK -> () -> {
        UI.getCurrent().navigate(PROJECT_CREATION_URL);
        registeredProjectOverview.selectCreationModeDialog.close();
        registeredProjectOverview.selectCreationModeDialog.reset();
      };
      case FROM_OFFER -> () -> {
        registeredProjectOverview.selectCreationModeDialog.close();
        loadOfferPreview();
        registeredProjectOverview.searchDialog.open();
      };
      case NONE -> () -> {
        // Nothing to do, user has not made a selection
      };
    };
  }

  private Command cancelSelection() {
    return () -> {
      registeredProjectOverview.selectCreationModeDialog.close();
      registeredProjectOverview.selectCreationModeDialog.reset();
      creationMode = CreationMode.NONE;
    };
  }

  private void configureSearchDropbox() {
    configureSearchDialogFooterButtons();

    registeredProjectOverview.searchDialog.ok.addClickListener(e -> {
      //check if value is selected
      registeredProjectOverview.searchDialog.searchField.getOptionalValue()
          .map(this::navigateToProjectCreation)
          .ifPresent(Command::execute);
    });
  }

  private void configureSearchDialogFooterButtons() {
    registeredProjectOverview.searchDialog.cancel.addClickListener(
        e -> registeredProjectOverview.searchDialog.close());

    registeredProjectOverview.create.addClickListener(
        e -> registeredProjectOverview.selectCreationModeDialog.open());

  }

  private Command navigateToProjectCreation(OfferPreview offerPreview) {
    return () -> {
      registeredProjectOverview.searchDialog.close();
      QueryParameters queryParameters = QueryParameters.fromString(
          OFFER_ID_QUERY_PARAMETER + QUERY_PARAMETER_SEPARATOR + offerPreview.offerId().id());
      UI.getCurrent().navigate(PROJECT_CREATION_URL, queryParameters);
    };
  }

  private void loadOfferPreview() {
    // Configure the filter and pagination for the lazy loaded OfferPreview items
    registeredProjectOverview.searchDialog.searchField.setItems(
        query -> offerLookupService.findOfferContainingProjectTitleOrId(
            query.getFilter().orElse(""),
            query.getFilter().orElse(""), query.getOffset(), query.getLimit()).stream());

    // Render the preview
    registeredProjectOverview.searchDialog.searchField.setRenderer(
        new ComponentRenderer<>(preview ->
            new Text(previewToString(preview))));

    // Generate labels like the rendering
    registeredProjectOverview.searchDialog.searchField.setItemLabelGenerator(
        (ItemLabelGenerator<OfferPreview>) ProjectOverviewHandler::previewToString);
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

  /**
   * Enum to define in which mode the project will be created
   */
  private enum CreationMode {
    BLANK, FROM_OFFER, NONE
  }

}
