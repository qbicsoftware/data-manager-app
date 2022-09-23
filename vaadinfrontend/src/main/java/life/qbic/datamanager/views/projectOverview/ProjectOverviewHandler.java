package life.qbic.datamanager.views.projectOverview;

import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.QueryParameters;
import life.qbic.projectmanagement.application.finances.offer.OfferLookupService;
import life.qbic.projectmanagement.domain.finances.offer.OfferPreview;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

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

  private CreationMode creationMode = CreationMode.NONE;

  public ProjectOverviewHandler(@Autowired OfferLookupService offerLookupService) {
    Objects.requireNonNull(offerLookupService);
    this.offerLookupService = offerLookupService;
  }

  @Override
  public void handle(ProjectOverviewLayout layout) {
    if (registeredProjectOverview != layout) {
      this.registeredProjectOverview = layout;
      configureSearchDropbox();
      configureSelectionModeDialog();
    }
  }

  private void configureSelectionModeDialog() {
    configureSelectionModeDialogFooterButtons();

    registeredProjectOverview.selectCreationModeDialog.blankButton.addClickListener(
        e -> creationMode = CreationMode.BLANK);
    registeredProjectOverview.selectCreationModeDialog.fromOfferButton.addClickListener(
        e -> creationMode = CreationMode.FROM_OFFER);
  }

  private void configureSelectionModeDialogFooterButtons() {
    registeredProjectOverview.selectCreationModeDialog.next.addClickListener(e -> {
      switch (creationMode) {
        case BLANK -> {
          UI.getCurrent().navigate(PROJECT_CREATION_URL);
          registeredProjectOverview.selectCreationModeDialog.close();
          registeredProjectOverview.selectCreationModeDialog.reset();
        }
        case FROM_OFFER -> {
          registeredProjectOverview.selectCreationModeDialog.close();
          loadItemsWithService(offerLookupService);
          registeredProjectOverview.searchDialog.open();
        }
        case NONE -> {
          // Nothing to do, user has not made a selection
        }
      }
    });
    registeredProjectOverview.selectCreationModeDialog.cancel.addClickListener(e -> {
      registeredProjectOverview.selectCreationModeDialog.close();
      registeredProjectOverview.selectCreationModeDialog.reset();
      creationMode = CreationMode.NONE;
    });
  }

  private void configureSearchDropbox() {
    configureSearchDialogFooterButtons();

    registeredProjectOverview.searchDialog.ok.addClickListener(e -> {
      //check if value is selected
      registeredProjectOverview.searchDialog.searchField.getOptionalValue()
          .ifPresent(this::navigateToProjectCreation);

    });
  }

  private void configureSearchDialogFooterButtons() {
    registeredProjectOverview.searchDialog.cancel.addClickListener(
        e -> registeredProjectOverview.searchDialog.close());

    registeredProjectOverview.create.addClickListener(
        e -> registeredProjectOverview.selectCreationModeDialog.open());

  }

  private void navigateToProjectCreation(OfferPreview offerPreview) {
    registeredProjectOverview.searchDialog.close();
    QueryParameters queryParameters = QueryParameters.fromString(
        OFFER_ID_QUERY_PARAMETER + QUERY_PARAMETER_SEPARATOR + offerPreview.offerId().id());

    UI.getCurrent().navigate(PROJECT_CREATION_URL, queryParameters);
  }

  private void loadItemsWithService(OfferLookupService service) {
    registeredProjectOverview.searchDialog.searchField.setItems(
        query -> service.findOfferContainingProjectTitleOrId(query.getFilter().orElse(""),
            query.getFilter().orElse(""), query.getOffset(), query.getLimit()).stream());

    registeredProjectOverview.searchDialog.searchField.setRenderer(
        new ComponentRenderer<>(preview ->
            new Text(preview.offerId().id() + ", " + preview.getProjectTitle().title())));

    registeredProjectOverview.searchDialog.searchField.setItemLabelGenerator(
        (ItemLabelGenerator<OfferPreview>) preview ->
            preview.offerId().id() + ", " + preview.getProjectTitle().title());
  }

  /**
   * Enum to define in which mode the project will be created
   */
  enum CreationMode {
    BLANK, FROM_OFFER, NONE
  }

}
