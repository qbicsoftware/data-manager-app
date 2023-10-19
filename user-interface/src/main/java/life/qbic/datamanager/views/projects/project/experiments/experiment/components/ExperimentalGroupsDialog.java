package life.qbic.datamanager.views.projects.project.experiments.experiment.components;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import life.qbic.datamanager.views.general.CancelEvent;
import life.qbic.datamanager.views.general.ConfirmEvent;
import life.qbic.datamanager.views.general.DialogWindow;
import life.qbic.datamanager.views.projects.project.experiments.experiment.ExperimentalGroupInput;
import life.qbic.controlling.application.VariableValueFormatter;
import life.qbic.controlling.domain.model.experiment.VariableLevel;

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
      VariableValueFormatter.format(it.experimentalValue()));
  @Serial
  private static final long serialVersionUID = 1657697182040756406L;
  private final Collection<VariableLevel> experimentalVariableLevels;
  private final List<ComponentEventListener<CancelEvent<ExperimentalGroupsDialog>>> cancelListeners = new ArrayList<>();
  private final Div experimentalGroupsCollection = new Div();
  private final Div content = new Div();
  private final Div addNewGroupContainer = new Div();
  private final boolean editMode;
  private final List<ComponentEventListener<ConfirmEvent<ExperimentalGroupsDialog>>> confirmListeners = new ArrayList<>();

  private ExperimentalGroupsDialog(Collection<VariableLevel> experimentalVariableLevels,
      boolean editMode) {
    super();
    this.editMode = editMode;
    this.experimentalVariableLevels = Objects.requireNonNull(experimentalVariableLevels);
    layoutComponent();
    configureComponent();
  }

  private ExperimentalGroupsDialog(Collection<VariableLevel> experimentalVariableLevels,
      Collection<ExperimentalGroupContent> experimentalGroupContents, boolean editMode) {
    this(experimentalVariableLevels, editMode);
    this.experimentalGroupsCollection.removeAll();
    addEntries(experimentalVariableLevels, experimentalGroupContents);
  }

  private void addEntries(Collection<VariableLevel> experimentalVariableLevels,
      Collection<ExperimentalGroupContent> experimentalGroupContents) {
    experimentalGroupContents.stream().map(group -> {
      var groupEntry = new ExperimentalGroupInput(experimentalVariableLevels);
      groupEntry.setCondition(group.variableLevels());
      groupEntry.setReplicateCount(group.size());
      groupEntry.addRemoveEventListener(
          listener -> experimentalGroupsCollection.remove(groupEntry));
      return groupEntry;
    }).forEach(experimentalGroupsCollection::add);
  }

  /**
   * Creates an empty instance of the ExperimentalGroupsDialog.
   *
   * @param experimentalVariables the variable levels to choose from
   * @return a new ExperimentalGroupsDialog
   */
  public static ExperimentalGroupsDialog empty(Collection<VariableLevel> experimentalVariables) {
    return new ExperimentalGroupsDialog(experimentalVariables, false);
  }

  /**
   * Creates an ExperimentalGroupsDialog prefilled with the experimental groups provided.
   * @param experimentalVariables the variable levels to choose from
   * @param experimentalGroupContents the experimental groups prefilled into the input fields
   * @return a prefilled ExperimentalGroupDialog
   */
  public static ExperimentalGroupsDialog prefilled(Collection<VariableLevel> experimentalVariables,
      Collection<ExperimentalGroupContent> experimentalGroupContents) {
    return new ExperimentalGroupsDialog(experimentalVariables, experimentalGroupContents, true);
  }

  private static ExperimentalGroupContent convert(ExperimentalGroupInput experimentalGroupInput) {
    return new ExperimentalGroupContent(experimentalGroupInput.getReplicateCount(),
        experimentalGroupInput.getCondition());
  }

  private void configureComponent() {
    cancelButton.addClickListener(event -> fireCancelEvent());
    confirmButton.addClickListener(event -> fireConfirmEvent());
  }

  private void fireConfirmEvent() {
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
    var groupEntry = new ExperimentalGroupInput(experimentalVariableLevels);
    experimentalGroupsCollection.add(groupEntry);
    groupEntry.addRemoveEventListener(event -> removeExperimentalGroupEntry(event.getSource()));
  }

  void removeExperimentalGroupEntry(ExperimentalGroupInput entry) {
    experimentalGroupsCollection.getChildren().filter(entry::equals)
        .findAny().ifPresent(experimentalGroupsCollection::remove);
    refreshGroupEntries();
  }

  private void refreshGroupEntries() {
    if (experimentalGroupsCollection.getChildren().toList().isEmpty()) {
      var groupEntry = new ExperimentalGroupInput(experimentalVariableLevels);
      experimentalGroupsCollection.add(groupEntry);
      groupEntry.addRemoveEventListener(event -> removeExperimentalGroupEntry(event.getSource()));
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
        .filter(component -> component.getClass().equals(ExperimentalGroupInput.class))
        .map(experimentalGroupEntry -> convert((ExperimentalGroupInput) experimentalGroupEntry))
        .toList();
  }

  public record ExperimentalGroupContent(int size, Collection<VariableLevel> variableLevels) {}

}
