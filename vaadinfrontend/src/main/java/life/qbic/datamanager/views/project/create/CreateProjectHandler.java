package life.qbic.datamanager.views.project.create;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.BeforeEvent;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.Result;
import life.qbic.datamanager.exceptionhandlers.ApplicationExceptionHandler;
import life.qbic.datamanager.views.components.StyledNotification;
import life.qbic.datamanager.views.components.SuccessMessage;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.ProjectCreationService;
import life.qbic.projectmanagement.application.finances.offer.OfferLookupService;
import life.qbic.projectmanagement.domain.finances.offer.Offer;
import life.qbic.projectmanagement.domain.finances.offer.OfferId;
import life.qbic.projectmanagement.domain.project.Project;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CreateProjectHandler implements CreateProjectHandlerInterface {


  private static final Logger log = LoggerFactory.logger(CreateProjectHandler.class);

  private static final String OFFER_ID_QUERY_PARAM = "offerId";

  private final ProjectInformationHandler projectInformationHandler;
  private final ApplicationExceptionHandler exceptionHandler;


  private CreateProjectLayout createProjectLayout;

  private final ProjectCreationService projectCreationService;
  private final OfferLookupService offerLookupService;

  public CreateProjectHandler(@Autowired ApplicationExceptionHandler exceptionHandler,
      @Autowired OfferLookupService offerLookupService,
      @Autowired ProjectCreationService projectCreationService,
      @Autowired ProjectInformationHandler projectInformationHandler) {
    this.offerLookupService = offerLookupService;
    this.projectCreationService = projectCreationService;
    this.projectInformationHandler = projectInformationHandler;
    this.exceptionHandler = exceptionHandler;
  }

  @Override
  public void handle(CreateProjectLayout createProjectLayout) {
    if (this.createProjectLayout != createProjectLayout) {
      this.createProjectLayout = createProjectLayout;
      addSaveClickListener();
    }
  }

  @Override
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
    projectInformationHandler.loadOfferContent(offer);
  }

  private void addSaveClickListener() {
    createProjectLayout.projectInformationLayout.saveButton.addClickListener(it -> saveClicked());
  }

  private void saveClicked() {
    String titleFieldValue = createProjectLayout.projectInformationLayout.titleField.getValue();
    String objectiveFieldValue = createProjectLayout.projectInformationLayout.projectObjective.getValue();
    String experimentalDesignDescription = createProjectLayout.projectInformationLayout.experimentalDesignField.getValue();
    String loadedOfferId = createProjectLayout.projectInformationLayout.loadedOfferIdentifier.getText();
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
