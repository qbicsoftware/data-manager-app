package life.qbic.datamanager.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.util.List;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.BatchRegistrationDialog2;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Species;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Specimen;

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
    BatchRegistrationDialog2 dialog = new BatchRegistrationDialog2(
        List.of(Species.create("Test Species 1"), Species.create("Test Species 2")),
        List.of(Specimen.create("Test specimen 1"), Specimen.create("Test specimen 2")),
        List.of(Analyte.create("Test analyte 2"), Analyte.create("Test analyte 1")));

    dialog.addCancelListener(cancelEvent -> cancelEvent.getSource().close());
    dialog.addConfirmListener(confirmEvent -> System.out.println(confirmEvent.getData()));
    return dialog;
  }
}
