package life.qbic.datamanager.views.projects.project.experiments.experiment.components;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.NumberField;
import jakarta.validation.constraints.Min;
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

  private final ExperimentalGroupEntry experimentalGroupEntry = new ExperimentalGroupEntry();

  private final Div experimentalGroupsCollection = new Div();

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
    addNewGroupEntry();
    var addNewGroupIcon = new Icon(VaadinIcon.PLUS);
    addNewGroupIcon.addClickListener(listener -> addNewGroupEntry());
    add(experimentalGroupsCollection);
    add(addNewGroupIcon);

    addClassName("experiment-group-dialog");
    setConfirmButtonLabel("Add");
    setCancelButtonLabel("Cancel");
    getFooter().add(cancelButton, confirmButton);
  }

  private void addNewGroupEntry() {
    var groupEntry = experimentalGroupEntry.empty();
    groupEntry.setItems(experimentalVariables);
    experimentalGroupsCollection.add(groupEntry);
    groupEntry.registerForRemoveEvents(event -> removeExperimentalGroupEntry(event.getSource()));
  }

  void removeExperimentalGroupEntry(ExperimentalGroupEntry entry) {
    experimentalGroupsCollection.getChildren().filter(entry::equals)
        .findAny().ifPresent(experimentalGroupsCollection::remove);
    refreshGroupEntries();
  }

  private void refreshGroupEntries() {
    if (experimentalGroupsCollection.getChildren().toList().isEmpty()) {
      var groupEntry = experimentalGroupEntry.empty();
      groupEntry.setItems(experimentalVariables);
      experimentalGroupsCollection.add(groupEntry);
      groupEntry.registerForRemoveEvents(event -> removeExperimentalGroupEntry(event.getSource()));
    }
  }

  public static ExperimentalGroupsDialog empty(Collection<VariableLevel> experimentalVariables) {
    return new ExperimentalGroupsDialog(experimentalVariables);
  }

  public static ExperimentalGroupsDialog prefilled(Collection<VariableLevel> experimentalVariables, Collection<ExperimentalGroup> experimentalGroups) {
    //TODO
    return null;
  }

  private class ExperimentalGroupEntry extends Div {

    @Serial
    private static final long serialVersionUID = -1387021927263833261L;
    private MultiSelectComboBox<VariableLevel> variableComboBox;

    private final List<ComponentEventListener<RemoveEvent>> removeEventListeners = new ArrayList<>();

    @Min(1)
    private NumberField sampleSize;
    private ExperimentalGroupEntry() {
      super();
      variableComboBox = new MultiSelectComboBox<>();
      sampleSize = new NumberField();
      sampleSize.addClassName("number-field");
      sampleSize.setLabel("Biological Replicates");
      sampleSize.setStepButtonsVisible(true);
      sampleSize.setStep(1);
      sampleSize.setMin(1);
      sampleSize.setValue(1.0);
      sampleSize.setErrorMessage("Please specify a valid number of replicates");
      //variableComboBox.setItems(experimentalVariables);
      variableComboBox.setItemLabelGenerator(VARIABLE_LEVEL_ITEM_LABEL_GENERATOR);
      variableComboBox.setWidthFull();
      variableComboBox.setLabel("Condition");
      variableComboBox.addClassName("combo-box");
      variableComboBox.addClassName("chip-badge");
      variableComboBox.setAllowCustomValue(false);
      add(variableComboBox);
      add(sampleSize);
      var deleteIcon = new Icon(VaadinIcon.CLOSE_SMALL);
      deleteIcon.addClickListener(listener -> notifyListenersAboutRemove());
      add(deleteIcon);
      addClassName("experimental-group-entry");
    }

    ExperimentalGroupEntry empty() {
      return new ExperimentalGroupEntry();
    }

    void setItems(Collection<VariableLevel> availableVariableLevels) {
      variableComboBox.setItems(availableVariableLevels);
    }

    void registerForRemoveEvents(ComponentEventListener<RemoveEvent> listener){
      removeEventListeners.add(listener);
    }

    private void notifyListenersAboutRemove() {
      var removeEvent = new RemoveEvent(this, true);
      removeEventListeners.forEach(listener -> listener.onComponentEvent(removeEvent));
    }
  }

  class RemoveEvent extends ComponentEvent<ExperimentalGroupEntry> {

    @Serial
    private static final long serialVersionUID = 2934203596212238997L;

    /**
     * Creates a new event using the given source and indicator whether the event originated from the
     * client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public RemoveEvent(ExperimentalGroupEntry source, boolean fromClient) {
      super(source, fromClient);
    }
  }
}
