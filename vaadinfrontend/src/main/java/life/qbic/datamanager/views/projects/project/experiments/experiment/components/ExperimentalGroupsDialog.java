package life.qbic.datamanager.views.projects.project.experiments.experiment.components;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import life.qbic.datamanager.views.general.CancelEvent;
import life.qbic.datamanager.views.general.DialogWindow;
import life.qbic.projectmanagement.application.ExperimentValueFormatter;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.project.experiment.VariableLevel;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class ExperimentalGroupsDialog extends DialogWindow {

  private final Collection<VariableLevel> experimentalVariables;

  private final List<ComponentEventListener<CancelEvent<ExperimentalGroupsDialog>>> cancelListeners = new ArrayList<>();

  private static final ItemLabelGenerator<VariableLevel> VARIABLE_LEVEL_ITEM_LABEL_GENERATOR = it -> String.format(
      "%s: %s", it.variableName().value(),
      ExperimentValueFormatter.format(it.experimentalValue()));

  @Serial
  private static final long serialVersionUID = 1657697182040756406L;

  private ExperimentalGroupsDialog(Collection<VariableLevel> experimentalVariables) {
    super();
    this.experimentalVariables = Objects.requireNonNull(experimentalVariables);
    layoutComponent();
    configureComponent();
  }

  private void configureComponent() {
    cancelButton.addClickListener(event -> fireCancelEvent());
  }

  private void fireCancelEvent() {
    CancelEvent<ExperimentalGroupsDialog> cancelEvent = new CancelEvent<>(this, true);
    cancelListeners.forEach(cancelEventComponentEventListener -> cancelEventComponentEventListener.onComponentEvent(cancelEvent));
  }

  public void subscribeToCancelEvent(ComponentEventListener<CancelEvent<ExperimentalGroupsDialog>> listener) {
    this.cancelListeners.add(listener);
  }

  private void layoutComponent() {
    MultiSelectComboBox<VariableLevel> multiSelectComboBox = new MultiSelectComboBox<>();
    multiSelectComboBox.setItems(experimentalVariables);
    multiSelectComboBox.setItemLabelGenerator(VARIABLE_LEVEL_ITEM_LABEL_GENERATOR);
    multiSelectComboBox.setWidthFull();
    multiSelectComboBox.setLabel("Condition");
    multiSelectComboBox.addClassName("combo-box");
    multiSelectComboBox.addClassName("chip-badge");
    multiSelectComboBox.setAllowCustomValue(false);
    add(multiSelectComboBox);

    addClassName("experiment-group-dialog");
    setConfirmButtonLabel("Add");
    setCancelButtonLabel("Cancel");
    getFooter().add(cancelButton, confirmButton);
  }


  public static ExperimentalGroupsDialog empty(Collection<VariableLevel> experimentalVariables) {
    return new ExperimentalGroupsDialog(experimentalVariables);
  }

  public static ExperimentalGroupsDialog prefilled(Collection<VariableLevel> experimentalVariables, Collection<ExperimentalGroup> experimentalGroups) {
    //TODO
    return null;
  }


}
