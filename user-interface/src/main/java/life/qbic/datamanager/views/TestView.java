package life.qbic.datamanager.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.BatchRegistrationDialog2;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
@PermitAll
@Route(value = "test", layout = MainLayout.class)
public class TestView extends HorizontalLayout {

  //FIXME REMOVE THIS AGAIN
  public TestView() {
    setSizeFull();
    add(new Button("open dialog", event -> getBatchRegistrationDialog().open()));
  }

  private static BatchRegistrationDialog2 getBatchRegistrationDialog() {
    BatchRegistrationDialog2 dialog = new BatchRegistrationDialog2();
    dialog.addCancelListener(cancelEvent -> cancelEvent.getSource().close());
    return dialog;
  }
}
