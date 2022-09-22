package life.qbic.datamanager.views.project.create;

import com.vaadin.flow.router.BeforeEvent;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.finances.offer.OfferLookupService;
import life.qbic.projectmanagement.domain.finances.offer.Offer;
import life.qbic.projectmanagement.domain.finances.offer.OfferId;
import com.vaadin.flow.component.notification.Notification;
import life.qbic.projectmanagement.application.ProjectCreationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CreateProjectHandler implements CreateProjectHandlerInterface {


  private static final Logger log = LoggerFactory.logger(CreateProjectHandler.class);

  private static final String OFFER_ID_QUERY_PARAM = "offerId";

  private CreateProjectLayout createProjectLayout;

  private final ProjectCreationService projectCreationService;
  private final OfferLookupService offerLookupService;

  public CreateProjectHandler(@Autowired OfferLookupService offerLookupService,
      @Autowired ProjectCreationService projectCreationService) {
    this.offerLookupService = offerLookupService;
    this.projectCreationService = projectCreationService;
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
    log.info("Loading content from offer " + offer.offerId().id());
    createProjectLayout.titleField.setValue(offer.projectTitle().title());
    createProjectLayout.projectObjective.setValue(offer.projectObjective().objective());
  }

  private void addSaveClickListener() {
    createProjectLayout.saveButton.addClickListener(it -> saveClicked());
  }

  private void saveClicked() {
    String titleFieldValue = createProjectLayout.titleField.getValue();
    String objectiveFieldValue = createProjectLayout.projectObjective.getValue();
    //add exp design

    projectCreationService.createProject(titleFieldValue, objectiveFieldValue)
        .ifSuccess(it -> displaySuccessfulProjectCreationNotification());
  }

  private void displaySuccessfulProjectCreationNotification() {
    Notification.show("Project creation succeeded.");
  }
}
