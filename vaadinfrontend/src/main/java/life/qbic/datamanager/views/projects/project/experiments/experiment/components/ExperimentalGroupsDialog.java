package life.qbic.datamanager.views.projects.project.experiments.experiment.components;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
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
import life.qbic.datamanager.views.general.ConfirmEvent;
import life.qbic.datamanager.views.general.DialogWindow;
import life.qbic.projectmanagement.application.ExperimentValueFormatter;
import life.qbic.projectmanagement.domain.project.experiment.VariableLevel;

/**
 * <b>Experimental Groups Dialog</b>
 *
 * <p>A dialog window that enables the user to add new experimental groups to an experiment
 * or edit existing ones.</p>
 *
 * @since 1.0.0
 */
public class ExperimentalGroupsDialog extends DialogWindow {

  private static final ItemLabelGenerator<VariableLevel> VARIABLE_LEVEL_ITEM_LABEL_GENERATOR = it -> String.format(
      "%s: %s", it.variableName().value(),
      ExperimentValueFormatter.format(it.experimentalValue()));
  @Serial
  private static final long serialVersionUID = 1657697182040756406L;
  private final Collection<VariableLevel> experimentalVariables;
  private final List<ComponentEventListener<CancelEvent<ExperimentalGroupsDialog>>> cancelListeners = new ArrayList<>();
  private final Div experimentalGroupsCollection = new Div();
  private final Div content = new Div();
  private final Div addNewGroupContainer = new Div();
  private final boolean editMode;
  private final List<ComponentEventListener<ConfirmEvent<ExperimentalGroupsDialog>>> confirmListeners = new ArrayList<>();

  private ExperimentalGroupsDialog(Collection<VariableLevel> experimentalVariables,
      boolean editMode) {
    super();
    this.editMode = editMode;
    this.experimentalVariables = Objects.requireNonNull(experimentalVariables);
    layoutComponent();
    configureComponent();
  }

  private ExperimentalGroupsDialog(Collection<VariableLevel> experimentalVariables,
      Collection<ExperimentalGroupContent> experimentalGroupContents, boolean editMode) {
    this(experimentalVariables, editMode);
    this.experimentalGroupsCollection.removeAll();
    addEntries(experimentalVariables, experimentalGroupContents);
  }

  private void addEntries(Collection<VariableLevel> experimentalVariables,
      Collection<ExperimentalGroupContent> experimentalGroupContents) {
    experimentalGroupContents.stream().map(group -> {
      var groupEntry = new ExperimentalGroupEntry();
      groupEntry.setAvailableVariableLevels(experimentalVariables);
      groupEntry.setCondition(group.variableLevels());
      groupEntry.setSampleSize(group.size());
      groupEntry.registerForRemoveEvents(
          listener -> experimentalGroupsCollection.remove(groupEntry));
      return groupEntry;
    }).forEach(experimentalGroupsCollection::add);
  }

  public static ExperimentalGroupsDialog empty(Collection<VariableLevel> experimentalVariables) {
    return new ExperimentalGroupsDialog(experimentalVariables, false);
  }

  public static ExperimentalGroupsDialog prefilled(Collection<VariableLevel> experimentalVariables,
      Collection<ExperimentalGroupContent> experimentalGroupContents) {
    return new ExperimentalGroupsDialog(experimentalVariables, experimentalGroupContents, true);
  }

  private static ExperimentalGroupContent convert(ExperimentalGroupEntry experimentalGroupEntry) {
    return new ExperimentalGroupContent(experimentalGroupEntry.sampleSize.getValue().intValue(),
        experimentalGroupEntry.condition.getSelectedItems());
  }

  private void configureComponent() {
    cancelButton.addClickListener(event -> fireCancelEvent());
    confirmButton.addClickListener(event -> validateAndFireEvent());
  }

  private void validateAndFireEvent() {
    var event = new ConfirmEvent<>(this, true);
    confirmListeners.forEach(listener -> listener.onComponentEvent(event));
  }

  private void fireCancelEvent() {
    CancelEvent<ExperimentalGroupsDialog> cancelEvent = new CancelEvent<>(this, true);
    cancelListeners.forEach(
        cancelEventComponentEventListener -> cancelEventComponentEventListener.onComponentEvent(
            cancelEvent));
  }

  public void addCancelEventListener(
      ComponentEventListener<CancelEvent<ExperimentalGroupsDialog>> listener) {
    this.cancelListeners.add(listener);
  }

  public void addConfirmEventListener(
      ComponentEventListener<ConfirmEvent<ExperimentalGroupsDialog>> listener) {
    this.confirmListeners.add(listener);
  }

  private void layoutComponent() {
    layoutHeaderAndFooter();

    content.addClassName("content");
    content.add(experimentalGroupsCollection);
    content.add(addNewGroupContainer);
    add(content);

    experimentalGroupsCollection.addClassName("group-collection");
    addNewGroupContainer.addClassName("add-new-group-action");
    addNewGroupEntry();

    var addNewGroupIcon = new Icon(VaadinIcon.PLUS);
    addNewGroupIcon.addClickListener(listener -> addNewGroupEntry());
    Span addGroupHelperText = new Span("Add Experimental Group");
    addGroupHelperText.addClickListener(listener -> addNewGroupEntry());
    addNewGroupContainer.add(addNewGroupIcon, addGroupHelperText);
  }

  private void layoutHeaderAndFooter() {
    setHeaderTitle(editMode ? "Edit Experimental Groups" : "Add Experimental Groups");
    addClassName("experiment-group-dialog");
    setConfirmButtonLabel(editMode ? "Save" : "Add");
    setCancelButtonLabel("Cancel");
    getFooter().add(cancelButton, confirmButton);
  }

  private void addNewGroupEntry() {
    var groupEntry = new ExperimentalGroupEntry();
    groupEntry.setAvailableVariableLevels(experimentalVariables);
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
      var groupEntry = new ExperimentalGroupEntry();
      groupEntry.setAvailableVariableLevels(experimentalVariables);
      experimentalGroupsCollection.add(groupEntry);
      groupEntry.registerForRemoveEvents(event -> removeExperimentalGroupEntry(event.getSource()));
    }
  }

  /**
   * Provides the current experimental groups defined by the user.
   *
   * @return a collection of {@link ExperimentalGroupContent}, describing the group size (biological
   * replicates) and the variable level combination.
   * @since 1.0.0
   */
  public Collection<ExperimentalGroupContent> experimentalGroups() {
    return this.experimentalGroupsCollection.getChildren()
        .filter(component -> component.getClass().equals(ExperimentalGroupEntry.class))
        .map(experimentalGroupEntry -> convert((ExperimentalGroupEntry) experimentalGroupEntry))
        .toList();
  }

  public record ExperimentalGroupContent(int size, Collection<VariableLevel> variableLevels) {}

  class ExperimentalGroupEntry extends Div {

    @Serial
    private static final long serialVersionUID = -1387021927263833261L;
    private final List<ComponentEventListener<RemoveEvent>> removeEventListeners = new ArrayList<>();
    private final MultiSelectComboBox<VariableLevel> condition = new MultiSelectComboBox<>();
    @Min(1)
    private final NumberField sampleSize = new NumberField();

    private ExperimentalGroupEntry() {
      super();
      addClassName("experimental-group-entry");

      configureNumberFied();
      configureConditionField();

      var deleteIcon = new Icon(VaadinIcon.CLOSE_SMALL);
      deleteIcon.addClickListener(listener -> notifyListenersAboutRemove());

      add(condition, sampleSize, deleteIcon);
    }

    private void configureConditionField() {
      condition.setItems(new ArrayList<>());
      condition.setItemLabelGenerator(VARIABLE_LEVEL_ITEM_LABEL_GENERATOR);
      condition.setWidthFull();
      condition.setLabel("Condition");
      condition.addClassName("combo-box");
      condition.addClassName("chip-badge");
      condition.setAllowCustomValue(false);
    }

    private void configureNumberFied() {
      sampleSize.addClassName("number-field");
      sampleSize.setLabel("Biological Replicates");
      sampleSize.setStepButtonsVisible(true);
      sampleSize.setStep(1);
      sampleSize.setMin(1);
      sampleSize.setValue(1.0);
      sampleSize.setErrorMessage("Please specify a valid number of replicates");
    }

    void setAvailableVariableLevels(Collection<VariableLevel> availableVariableLevels) {
      condition.setItems(availableVariableLevels);
    }

    void setCondition(Collection<VariableLevel> condition) {
      this.condition.setValue(condition);
    }

    void setSampleSize(int size) {
      sampleSize.setValue((double) size);
    }

    void registerForRemoveEvents(ComponentEventListener<RemoveEvent> listener) {
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
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
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
