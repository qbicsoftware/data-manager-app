package life.qbic.datamanager.views.projects.project.info;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.List;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.OfferDownload;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentListComponent;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentListComponent.AddExperimentClickEvent;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentListComponent.ExperimentSelectionEvent;
import life.qbic.datamanager.views.projects.project.info.OfferList.DeleteOfferClickEvent;
import life.qbic.datamanager.views.projects.project.info.OfferList.DownloadOfferClickEvent;
import life.qbic.datamanager.views.projects.project.info.OfferList.OfferInfo;
import life.qbic.datamanager.views.projects.project.info.OfferList.UploadOfferClickEvent;
import life.qbic.datamanager.views.projects.purchase.UploadPurchaseDialog;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.purchase.OfferDTO;
import life.qbic.projectmanagement.application.purchase.ProjectPurchaseService;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Project support component
 * <p>
 * The support component is a {@link Div} container, which is responsible for hosting the components
 * handling the sidebar content within the {@link ProjectInformationMain}. It propagates the project
 * information provided in the {@link ProjectLinksComponent} and the experiment information provided
 * in the {@link ExperimentListComponent} to the {@link ProjectInformationMain}, and vice
 * versa and can be easily extended with additional components if necessary
 */
@SpringComponent
@UIScope
public class ProjectSupportComponent extends Div {

  @Serial
  private static final long serialVersionUID = -6996282848714468102L;
  private final ExperimentListComponent experimentListComponent;
  private static final Logger log = LoggerFactory.logger(ProjectSupportComponent.class);

  private final ProjectPurchaseService projectPurchaseService;
  private final OfferDownload offerDownload;
  private final OfferList offerList;
  private Context context;


  public ProjectSupportComponent(@Autowired ExperimentListComponent experimentListComponent,
      @Autowired ProjectPurchaseService projectPurchaseService) {
    this.projectPurchaseService = requireNonNull(projectPurchaseService,
        "projectPurchaseService must not be null");
    this.experimentListComponent = requireNonNull(experimentListComponent,
        "experimentListComponent must not be null");

    offerList = getConfiguredOfferList();
    offerDownload = new OfferDownload(
        (projectId, offerId) -> projectPurchaseService.getOfferWithContent(projectId, offerId)
            .orElseThrow());
    this.experimentListComponent.addExperimentSelectionListener(this::fireEvent);
    this.experimentListComponent.addAddButtonListener(this::fireEvent);
    add(offerDownload, this.experimentListComponent, offerList);
  }

  private OfferList getConfiguredOfferList() {
    OfferList component = new OfferList();
    component.addDeleteOfferClickListener(this::onDeleteOfferClicked);
    component.addDownloadOfferClickListener(this::onDownloadOfferClicked);
    component.addUploadOfferClickListener(
        event -> onUploadOfferClicked(event, projectPurchaseService,
            context.projectId().orElseThrow().value()));
    return component;
  }

  private void onDownloadOfferClicked(DownloadOfferClickEvent downloadOfferClickEvent) {
    offerDownload.trigger(context.projectId().orElseThrow().value(),
        downloadOfferClickEvent.offerId());
  }

  private void onDeleteOfferClicked(DeleteOfferClickEvent deleteOfferClickEvent) {
    projectPurchaseService.deleteOffer(context.projectId().orElseThrow().value(),
        deleteOfferClickEvent.offerId());
    deleteOfferClickEvent.getSource().remove(deleteOfferClickEvent.offerId());
    offerDownload.removeHref();
  }

  private void onUploadOfferClicked(UploadOfferClickEvent uploadOfferClickEvent,
      ProjectPurchaseService projectPurchaseService,
      String projectId) {
    UploadPurchaseDialog dialog = new UploadPurchaseDialog();
    dialog.addConfirmListener(confirmEvent -> {
      List<OfferDTO> offerDTOs = confirmEvent.getSource().purchaseItems().stream()
          .map(it -> new OfferDTO(it.signed(), it.fileName(), it.content()))
          .toList();
      projectPurchaseService.addPurchases(projectId, offerDTOs);
      refreshOffers(projectPurchaseService, projectId, uploadOfferClickEvent.getSource());
      confirmEvent.getSource().close();
    });
    dialog.addCancelListener(cancelEvent -> cancelEvent.getSource().close());
    dialog.open();
  }

  private static void refreshOffers(ProjectPurchaseService projectPurchaseService, String projectId,
      OfferList offerList) {
    List<OfferInfo> offers = projectPurchaseService.linkedOffers(projectId)
        .stream()
        .map(offer -> new OfferInfo(offer.id(), offer.getFileName(), offer.isSigned()))
        .toList();
    offerList.setOffers(offers);
  }

  /**
   * Propagates the context to internal components.
   *
   * @param context the context in which the user is.
   */
  public void setContext(Context context) {
    this.context = context;
    refreshOffers(projectPurchaseService, context.projectId().orElseThrow().value(), offerList);
    experimentListComponent.setContext(context);
  }

  /**
   * Propagates the listener which will retrieve notification if an {@link Experiment} was selected
   * to the {@link ExperimentListComponent} within this container
   */
  public void addExperimentSelectionListener(
      ComponentEventListener<ExperimentSelectionEvent> listener) {
    addListener(ExperimentSelectionEvent.class, listener);
  }

  public void addExperimentAddButtonClickEventListener(
      ComponentEventListener<AddExperimentClickEvent> listener) {
    addListener(AddExperimentClickEvent.class, listener);
  }

}
