package life.qbic.datamanager.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.util.List;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.BatchRegistrationDialog2;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.BatchRegistrationDialog2.ConfirmEvent.Data;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.BatchRegistrationDialog2.SampleInfo;
import life.qbic.projectmanagement.domain.model.experiment.Condition;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalValue;
import life.qbic.projectmanagement.domain.model.experiment.VariableLevel;
import life.qbic.projectmanagement.domain.model.experiment.VariableName;
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

    VariableLevel v1L1 = VariableLevel.create(VariableName.create("status"),
        ExperimentalValue.create("tumor"));
    VariableLevel v1L2 = VariableLevel.create(VariableName.create("status"),
        ExperimentalValue.create("normal"));
    VariableLevel v2L1 = VariableLevel.create(VariableName.create("group"),
        ExperimentalValue.create("test"));
    VariableLevel v2L2 = VariableLevel.create(VariableName.create("group"),
        ExperimentalValue.create("control"));
    Condition c1 = Condition.create(List.of(v1L1, v2L1));
    Condition c2 = Condition.create(List.of(v1L2, v2L1));
    Condition c3 = Condition.create(List.of(v1L1, v2L2));
    Condition c4 = Condition.create(List.of(v1L2, v2L2));
    ExperimentalGroup eg1 = ExperimentalGroup.create(c1, 1);
    ExperimentalGroup eg2 = ExperimentalGroup.create(c2, 2);
    ExperimentalGroup eg3 = ExperimentalGroup.create(c3, 3);
    ExperimentalGroup eg4 = ExperimentalGroup.create(c4, 4);

    BatchRegistrationDialog2 dialog = new BatchRegistrationDialog2("My awesome experiment",
        List.of(Species.create("Test Species A"), Species.create("Test Species B")),
        List.of(Specimen.create("Test specimen A")/*, Specimen.create("Test specimen B")*/),
        List.of(Analyte.create("Test analyte A")/*, Analyte.create("Test analyte B")*/),
        List.of(eg1, eg2, eg3, eg4));
    dialog.addCancelListener(cancelEvent -> cancelEvent.getSource().close());
    dialog.addConfirmListener(
        confirmEvent -> {
          Data data = confirmEvent.getData();
          System.out.println("Batch: " + data.batchName());
          for (SampleInfo sample : data.samples()) {
            System.out.println(sample);
          }
        });
    return dialog;
  }
}
