package life.qbic.datamanager.views.projects.project.experiments.experiment;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.data.binder.Binder;
import life.qbic.datamanager.views.projects.project.experiments.experiment.ExperimentalGroupInput.ExperimentalGroupBean;
import life.qbic.projectmanagement.domain.project.experiment.VariableLevel;

import java.util.*;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public class AddExperimentalGroupsDialog extends Dialog {

  private Collection<VariableLevel> levels;

  private ExperimentalGroupInput experimentalGroupInput;

  private final Button submitButton;

  private final Button cancelButton;

  private final List<Binder<?>> binders = new ArrayList<>();

  private final List<ExperimentalGroupSubmitListener> submitListeners = new ArrayList<>();

  public record ExperimentalGroupSubmitEvent(Dialog source, Set<VariableLevel> variableLevels,
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
    setHeaderTitle("Please enter group information");
    setCloseOnEsc(false);
    setCloseOnOutsideClick(false);
    levels = Collections.emptySet();
    submitButton = new Button("Create");
    submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    submitButton.addClickListener(event -> submit());
    cancelButton = new Button("Cancel");
    cancelButton.addClickListener(event -> close());
    getFooter().add(cancelButton, submitButton);
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
    experimentalGroupInput.setWidthFull();
    experimentalGroupInput.setRequiredIndicatorVisible(true);
    add(experimentalGroupInput);
    setWidth(66, Unit.PERCENTAGE);
    super.open();
  }

}
