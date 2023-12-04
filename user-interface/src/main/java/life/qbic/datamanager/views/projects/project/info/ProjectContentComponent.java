package life.qbic.datamanager.views.projects.project.info;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.List;
import java.util.Objects;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.projects.purchase.UploadPurchaseDialog;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.purchase.OfferDTO;
import life.qbic.projectmanagement.application.purchase.ProjectPurchaseService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Project Content component
 * <p>
 * The content component is a {@link Div} container, which is responsible for hosting the components
 * handling the content within the {@link ProjectInformationMain}. It is intended to propagate
 * project information provided in the {@link ProjectDetailsComponent} to the
 * {@link ProjectInformationMain} and vice versa and can be easily extended with additional
 * components if necessary
 */

@SpringComponent
@UIScope
public class ProjectContentComponent extends Div {

  private static final Logger log = LoggerFactory.logger(ProjectContentComponent.class);
  @Serial
  private static final long serialVersionUID = -1061134126086910532L;
  private final ProjectDetailsComponent projectDetailsComponent;

  private final ProjectPurchaseService projectPurchaseService;
  private Context context;

  public ProjectContentComponent(
      @Autowired ProjectDetailsComponent projectDetailsComponent,
      @Autowired ProjectPurchaseService projectPurchaseService) {
    Objects.requireNonNull(projectDetailsComponent);
    this.projectPurchaseService = Objects.requireNonNull(projectPurchaseService);
    UploadPurchaseDialog uploadPurchaseDialog = new UploadPurchaseDialog();
    uploadPurchaseDialog.setOpened(false);
    add(uploadPurchaseDialog);
    Button uploadOffer = new Button("Upload offer");
    uploadOffer.addClickListener(listener -> uploadPurchaseDialog.open());
    uploadPurchaseDialog.addConfirmListener(
        uploadPurchaseDialogConfirmEvent -> {
          var offers = uploadPurchaseDialog.purchaseItems();
          uploadPurchaseDialog.close();
          addPurchaseItemsToProject(offers);
        });

    add(uploadOffer);

    this.projectDetailsComponent = projectDetailsComponent;
    layoutComponent();
  }

  private void addPurchaseItemsToProject(List<OfferDTO> offerDTOS) {
    if (context == null || context.projectId().isEmpty()) {
      throw new ApplicationException("No project context found, cannot save offers");
    }
    offerDTOS.forEach(offer -> projectPurchaseService.addPurchase(context.projectId().get().toString(), offer));
  }

  private void layoutComponent() {
    this.add(projectDetailsComponent);
  }

  /**
   * Propagates the context to internal components.
   *
   * @param context the context in which the user is.
   */
  public void setContext(Context context) {
    this.context = context;
    projectDetailsComponent.setContext(context);
  }

}
