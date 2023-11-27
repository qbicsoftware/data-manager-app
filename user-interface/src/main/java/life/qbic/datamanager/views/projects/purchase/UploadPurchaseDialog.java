package life.qbic.datamanager.views.projects.purchase;

import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
import java.io.Serial;
import life.qbic.datamanager.views.general.DialogWindow;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class UploadPurchaseDialog extends DialogWindow {

  private final Upload upload;

  private MultiFileMemoryBuffer multiFileMemoryBuffer;
  public UploadPurchaseDialog() {
    multiFileMemoryBuffer = new MultiFileMemoryBuffer();
    upload = new Upload(multiFileMemoryBuffer);

  }

  @Serial
  private static final long serialVersionUID = 6602134795666762831L;


}
