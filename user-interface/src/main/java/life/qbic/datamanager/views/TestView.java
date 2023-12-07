package life.qbic.datamanager.views;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.util.List;
import life.qbic.datamanager.views.projects.project.info.OfferList;
import life.qbic.datamanager.views.projects.project.info.OfferList.DeleteOfferClickEvent;
import life.qbic.datamanager.views.projects.project.info.OfferList.OfferInfo;
import life.qbic.datamanager.views.projects.project.info.OfferList.UploadOfferClickEvent;
import life.qbic.datamanager.views.projects.purchase.UploadPurchaseDialog;
import life.qbic.projectmanagement.application.purchase.OfferDTO;
import life.qbic.projectmanagement.application.purchase.ProjectPurchaseService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
@Route("/test")
@PermitAll
public class TestView extends Div {

  private final OfferList offerList;
  private final ProjectPurchaseService projectPurchaseService;

  final String projectId = "91537201-b5ff-489a-b2b4-720aaebbfbb9"; //FIXME remove mock data

  public TestView(@Autowired ProjectPurchaseService projectPurchaseService) {

    this.projectPurchaseService = projectPurchaseService;
    offerList = new OfferList();
    offerList.addDeleteOfferClickListener(this::onDeleteOfferClicked);
    offerList.addDownloadOfferClickListener(
        event -> {
          Notification notification = new Notification("download offer " + event.offerId());
          notification.setDuration(2000);
          notification.open();
        }
    );
    offerList.addUploadOfferClickListener(
        event -> onUploadOfferClicked(event, projectPurchaseService, projectId));
    add(offerList);
    refreshOffers(projectPurchaseService, projectId, offerList);
  }

  private void onDeleteOfferClicked(DeleteOfferClickEvent deleteOfferClickEvent) {
    // get the offers from the event and delete them from the project
    projectPurchaseService.deleteOffer(projectId, deleteOfferClickEvent.offerId());
    deleteOfferClickEvent.getSource().remove(deleteOfferClickEvent.offerId());
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
}
