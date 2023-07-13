package life.qbic.datamanager.views.projects.project.experiments.experiment;

import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.data.binder.Binder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import life.qbic.datamanager.views.general.DialogWindow;
import life.qbic.datamanager.views.projects.project.experiments.experiment.ExperimentalGroupInput.ExperimentalGroupBean;
import life.qbic.projectmanagement.domain.project.experiment.VariableLevel;

/**
 * <b>Dialog allowing the addition of experimental groups.</b>
 *
 * <p>Displays input/selection fields for the creation of a new experimental group and handles
 * validation of said inputs.</p>
 *
 * @since 1.0.0
 */
public class AddExperimentalGroupsDialog extends DialogWindow {

  private Collection<VariableLevel> levels;

  private ExperimentalGroupInput experimentalGroupInput;

  private final List<Binder<?>> binders = new ArrayList<>();

  private final List<ExperimentalGroupSubmitListener> submitListeners = new ArrayList<>();

  public record ExperimentalGroupSubmitEvent(Dialog eventSourceDialog, Set<VariableLevel> variableLevels,
                                             int sampleSize) {

  }

  @FunctionalInterface
  public interface ExperimentalGroupSubmitListener {

    void handle(ExperimentalGroupSubmitEvent event);
  }

  private void fireExperimentalGroupSubmitEvent(ExperimentalGroupSubmitEvent event) {
    submitListeners.forEach(it -> it.handle(event));
  }

  public void addExperimentalGroupSubmitListener(
      ExperimentalGroupSubmitListener experimentalGroupSubmitListener) {
    this.submitListeners.add(experimentalGroupSubmitListener);
  }


  public AddExperimentalGroupsDialog() {
    super();
    setConfirmButtonLabel("Add");
    setCancelButtonLabel("Cancel");
    addClassName("experiment-group-dialog");
    setHeaderTitle("Please enter group information");
    levels = Collections.emptySet();

    confirmButton.addClickListener(event -> submit());
    cancelButton.addClickListener(event -> close());
    getFooter().add(cancelButton, confirmButton);
  }

  private void submit() {
    //TODO validate all fields
    binders.forEach(Binder::validate);
    if (experimentalGroupInput.isInvalid()) {
      return;
    }
    ExperimentalGroupBean value = experimentalGroupInput.getValue();
    ExperimentalGroupSubmitEvent event = new ExperimentalGroupSubmitEvent(
        this,
        new HashSet<>(value.getLevels()),
        value.getSampleSize());
    fireExperimentalGroupSubmitEvent(event);
  }

  public void setLevels(Collection<VariableLevel> levels) {
    if (isOpened()) {
      return;
    }
    this.levels = levels;
  }

  @Override
  public void open() {
    if (Objects.nonNull(experimentalGroupInput)) {
      remove(experimentalGroupInput);
    }
    experimentalGroupInput = new ExperimentalGroupInput(levels);
    experimentalGroupInput.setRequiredIndicatorVisible(true);
    add(experimentalGroupInput);
    super.open();
  }

}
