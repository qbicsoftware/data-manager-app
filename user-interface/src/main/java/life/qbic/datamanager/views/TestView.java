package life.qbic.datamanager.views;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.util.List;
import life.qbic.datamanager.views.projects.project.info.OfferList;
import life.qbic.datamanager.views.projects.project.info.OfferList.DeleteOfferClickEvent;
import life.qbic.datamanager.views.projects.project.info.OfferList.DownloadOfferClickEvent;
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
  private OfferDownload offerDownload;

  public TestView(@Autowired ProjectPurchaseService projectPurchaseService) {

    this.projectPurchaseService = projectPurchaseService;
    offerList = new OfferList();
    offerList.addDeleteOfferClickListener(this::onDeleteOfferClicked);
    offerList.addDownloadOfferClickListener(this::onDownloadOfferClicked);
    offerList.addUploadOfferClickListener(
        event -> onUploadOfferClicked(event, projectPurchaseService, projectId));
    offerDownload = new OfferDownload(
        (projectId, offerId) -> projectPurchaseService.getOfferWithContent(projectId, offerId)
            .orElseThrow());
    add(offerList, offerDownload);
    refreshOffers(projectPurchaseService, projectId, offerList);
  }

  private void onDownloadOfferClicked(DownloadOfferClickEvent downloadOfferClickEvent) {
    offerDownload.trigger(projectId, downloadOfferClickEvent.offerId());
    offerDownload.removeHref();
  }

  private void onDeleteOfferClicked(DeleteOfferClickEvent deleteOfferClickEvent) {
    projectPurchaseService.deleteOffer(projectId, deleteOfferClickEvent.offerId());
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

}
