package life.qbic.datamanager.views.projects.project.info;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.Objects;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.projects.purchase.UploadPurchaseDialog;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
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

  public ProjectContentComponent(
      @Autowired ProjectDetailsComponent projectDetailsComponent) {
    Objects.requireNonNull(projectDetailsComponent);
    UploadPurchaseDialog uploadPurchaseDialog = new UploadPurchaseDialog();
    uploadPurchaseDialog.setOpened(false);
    add(uploadPurchaseDialog);
    Button uploadOffer = new Button("Upload offer");
    uploadOffer.addClickListener(listener -> uploadPurchaseDialog.open());

    add(uploadOffer);

    this.projectDetailsComponent = projectDetailsComponent;
    layoutComponent();
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
    projectDetailsComponent.setContext(context);
  }

}
