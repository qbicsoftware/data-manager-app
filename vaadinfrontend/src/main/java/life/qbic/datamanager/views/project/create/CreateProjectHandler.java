package life.qbic.datamanager.views.project.create;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.data.value.ValueChangeMode;
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
import life.qbic.projectmanagement.domain.project.ExperimentalDesignDescription;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.ProjectObjective;
import life.qbic.projectmanagement.domain.project.ProjectTitle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CreateProjectHandler implements CreateProjectHandlerInterface {


  private static final Logger log = LoggerFactory.logger(CreateProjectHandler.class);

  private static final String OFFER_ID_QUERY_PARAM = "offerId";

  private final ApplicationExceptionHandler exceptionHandler;


  private CreateProjectLayout createProjectLayout;

  private final ProjectCreationService projectCreationService;
  private final OfferLookupService offerLookupService;

  public CreateProjectHandler(@Autowired OfferLookupService offerLookupService,
      @Autowired ProjectCreationService projectCreationService, @Autowired ApplicationExceptionHandler exceptionHandler) {
    this.offerLookupService = offerLookupService;
    this.projectCreationService = projectCreationService;
    this.exceptionHandler = exceptionHandler;
  }

  @Override
  public void handle(CreateProjectLayout createProjectLayout) {
    if (this.createProjectLayout != createProjectLayout) {
      this.createProjectLayout = createProjectLayout;
      addSaveClickListener();
      restrictInputLength();
    }
  }

  private void restrictInputLength() {
    createProjectLayout.titleField.setMaxLength((int) ProjectTitle.maxLength());
    createProjectLayout.projectObjective.setMaxLength((int) ProjectObjective.maxLength());
    createProjectLayout.experimentalDesignField.setMaxLength(
        (int) ExperimentalDesignDescription.maxLength());

    createProjectLayout.titleField.setValueChangeMode(ValueChangeMode.EAGER);
    createProjectLayout.projectObjective.setValueChangeMode(ValueChangeMode.EAGER);
    createProjectLayout.experimentalDesignField.setValueChangeMode(ValueChangeMode.EAGER);

    createProjectLayout.titleField.addValueChangeListener(e -> {
      e.getSource().setHelperText(
          e.getValue().length() + "/" + createProjectLayout.titleField.getMaxLength());
    });
    createProjectLayout.projectObjective.addValueChangeListener(e -> {
      e.getSource().setHelperText(
          e.getValue().length() + "/" + createProjectLayout.projectObjective.getMaxLength());
    });
    createProjectLayout.experimentalDesignField.addValueChangeListener(e -> {
      e.getSource().setHelperText(
          e.getValue().length() + "/" + createProjectLayout.experimentalDesignField.getMaxLength());
    });
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
    offer.experimentalDesignDescription()
        .ifPresent(it -> createProjectLayout.experimentalDesignField.setValue(it.description()));
  }

  private void addSaveClickListener() {
    createProjectLayout.saveButton.addClickListener(it -> saveClicked());
  }

  private void saveClicked() {
    String titleFieldValue = createProjectLayout.titleField.getValue();
    String objectiveFieldValue = createProjectLayout.projectObjective.getValue();
    String experimentalDesignDescription = createProjectLayout.experimentalDesignField.getValue();
    Result<Project, ApplicationException> project = projectCreationService.createProject(
        titleFieldValue, objectiveFieldValue, experimentalDesignDescription);
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
