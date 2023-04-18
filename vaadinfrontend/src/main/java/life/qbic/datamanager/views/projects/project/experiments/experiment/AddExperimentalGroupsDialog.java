package life.qbic.datamanager.views.projects.project.experiments.experiment;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import life.qbic.projectmanagement.domain.project.experiment.VariableLevel;

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

  record ExperimentalGroupInformation(Set<VariableLevel> levels, int sampleSize) {
  }


  public AddExperimentalGroupsDialog() {
    setHeaderTitle("Please enter group information");
    setCloseOnEsc(false);
    setCloseOnOutsideClick(false);
    levels = Collections.emptySet();
    submitButton = new Button("Create");
    submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    cancelButton = new Button("Cancel");
    cancelButton.addClickListener(event -> close());
    getFooter().add(cancelButton, submitButton);
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
    add(experimentalGroupInput);
    setWidth(66, Unit.PERCENTAGE);
    super.open();
  }

}
