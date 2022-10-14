package life.qbic.datamanager.views.project.create;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.security.PermitAll;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.Result;
import life.qbic.datamanager.exceptionhandlers.ApplicationExceptionHandler;
import life.qbic.datamanager.views.MainLayout;
import life.qbic.datamanager.views.notifications.StyledNotification;
import life.qbic.datamanager.views.notifications.SuccessMessage;
import life.qbic.datamanager.views.project.view.components.ProjectLinksComponent;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.ProjectCreationService;
import life.qbic.projectmanagement.application.finances.offer.OfferLookupService;
import life.qbic.projectmanagement.domain.finances.offer.Offer;
import life.qbic.projectmanagement.domain.finances.offer.OfferId;
import life.qbic.projectmanagement.domain.project.Project;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
@CssImport("./styles/components/create-project.css")
@PageTitle("Create Project")
@PermitAll
@Route(value = "projects/create", layout = MainLayout.class)
@Tag("create-project")
public class CreateProjectComponent extends Composite<HorizontalLayout> implements
    HasUrlParameter<String> {


  private final ProjectLinksComponent projectLinksComponent;
  private final Handler handler;
  private final ProjectInformationDialog projectInformationDialog;


  public CreateProjectComponent(@Autowired ApplicationExceptionHandler exceptionHandler,
      @Autowired OfferLookupService offerLookupService,
      @Autowired ProjectCreationService projectCreationService,
      @Autowired ProjectInformationDialog projectInformationDialog,
      @Autowired ProjectLinksComponent projectLinksComponent) {

    this.projectLinksComponent = projectLinksComponent;
    this.projectInformationDialog = projectInformationDialog;
    getContent().add(projectInformationDialog, projectLinksComponent);
    this.handler = new Handler(exceptionHandler, offerLookupService, projectCreationService);
    handler.handle();
  }

  @Override
  public void setParameter(BeforeEvent beforeEvent, @OptionalParameter String s) {
    handler.handleEvent(beforeEvent);
  }


  private class Handler {

    private static final Logger log = logger(Handler.class);

    private static final String OFFER_ID_QUERY_PARAM = "offerId";

    private final ApplicationExceptionHandler exceptionHandler;
    private final ProjectCreationService projectCreationService;
    private final OfferLookupService offerLookupService;

    public Handler(ApplicationExceptionHandler exceptionHandler,
        OfferLookupService offerLookupService,
        ProjectCreationService projectCreationService) {
      this.offerLookupService = offerLookupService;
      this.projectCreationService = projectCreationService;
      this.exceptionHandler = exceptionHandler;
    }

    public void handle() {
      addSaveClickListener();
    }

    public void handleEvent(BeforeEvent event) {
      Map<String, List<String>> queryParams = event.getLocation().getQueryParameters()
          .getParameters();
      if (queryParams.containsKey(OFFER_ID_QUERY_PARAM)) {
        String offerId = queryParams.get(OFFER_ID_QUERY_PARAM).iterator().next();
        preloadContentFromOffer(offerId);
      }
    }

    private void preloadContentFromOffer(String offerId) {
      log.info("Receiving offerId " + offerId);
      OfferId id = OfferId.from(offerId);
      Optional<Offer> offer = offerLookupService.findOfferById(id);
      offer.ifPresentOrElse(this::loadOfferContent,
          () -> log.error("No offer found with id: " + offerId));
    }

    private void loadOfferContent(Offer offer) {
      projectInformationDialog.setOffer(offer);
      projectLinksComponent.addLink(offer);
    }

    private void addSaveClickListener() {
      projectInformationDialog.createButton.addClickListener(it -> saveClicked());
    }

    private void saveClicked() {
      String titleFieldValue = projectInformationDialog.getTitle();
      String objectiveFieldValue = projectInformationDialog.getObjective();
      String experimentalDesignDescription = projectInformationDialog.getExperimentalDesign();
      String loadedOfferId = projectLinksComponent.linkedOffers().stream()
          .findFirst().orElse(null);
      Result<Project, ApplicationException> project = projectCreationService.createProject(
          titleFieldValue, objectiveFieldValue, experimentalDesignDescription, loadedOfferId);
      project.ifSuccessOrElse(
          result -> displaySuccessfulProjectCreationNotification(),
          applicationException -> exceptionHandler.handle(UI.getCurrent(), applicationException));
    }

    private void displaySuccessfulProjectCreationNotification() {
      SuccessMessage successMessage = new SuccessMessage("Project creation succeeded.", "");
      StyledNotification notification = new StyledNotification(successMessage);
      notification.open();
    }
  }

}
