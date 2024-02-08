package life.qbic.datamanager.views.projects.project.experiments.experiment.components;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import life.qbic.datamanager.views.general.DialogWindow;
import life.qbic.datamanager.views.projects.project.experiments.experiment.ExperimentalGroupInput;
import life.qbic.projectmanagement.application.VariableValueFormatter;
import life.qbic.projectmanagement.domain.model.experiment.VariableLevel;

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
  private final Div experimentalGroupsCollection = new Div();
  private final Div content = new Div();
  private final Div addNewGroupContainer = new Div();
  private final boolean editMode;

  private ExperimentalGroupsDialog(Collection<VariableLevel> experimentalVariableLevels,
      boolean editMode) {
    super();
    this.editMode = editMode;
    this.experimentalVariableLevels = Objects.requireNonNull(experimentalVariableLevels);
    layoutComponent();
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
      var groupEntry = new ExperimentalGroupInput(experimentalVariableLevels, editMode);
      groupEntry.setCondition(group.variableLevels());
      groupEntry.setReplicateCount(group.size());
      groupEntry.setEnabled(editMode);
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
   * Creates an ExperimentalGroupsDialog prefilled with the experimental groups provided. These
   * groups can be edited.
   * @param experimentalVariables the variable levels to choose from
   * @param experimentalGroupContents the experimental groups prefilled into the input fields
   * @return a prefilled ExperimentalGroupDialog
   */
  public static ExperimentalGroupsDialog editable(Collection<VariableLevel> experimentalVariables,
      Collection<ExperimentalGroupContent> experimentalGroupContents) {
    return new ExperimentalGroupsDialog(experimentalVariables, experimentalGroupContents, true);
  }

  /**
   * Creates an ExperimentalGroupsDialog prefilled with the experimental groups provided. Existing
   * groups can't be edited, but new groups added. Existing groups are therefore greyed out in the UI.
   * @param experimentalVariables the variable levels to choose from
   * @param experimentalGroupContents the experimental groups prefilled into the input fields
   * @return a prefilled ExperimentalGroupDialog
   */
  public static ExperimentalGroupsDialog nonEditable(Collection<VariableLevel> experimentalVariables,
      Collection<ExperimentalGroupContent> experimentalGroupContents) {
    ExperimentalGroupsDialog dialog = new ExperimentalGroupsDialog(experimentalVariables, experimentalGroupContents, false);
    dialog.addNewGroupEntry();
    return dialog;
  }

  private static ExperimentalGroupContent convert(ExperimentalGroupInput experimentalGroupInput) {
    return new ExperimentalGroupContent(experimentalGroupInput.getReplicateCount(),
        experimentalGroupInput.getCondition());
  }

  @Override
  protected void onConfirmClicked(ClickEvent<Button> clickEvent) {
    fireEvent(new ConfirmEvent(this, clickEvent.isFromClient()));
  }

  @Override
  protected void onCancelClicked(ClickEvent<Button> clickEvent) {
    fireEvent(new CancelEvent(this, clickEvent.isFromClient()));
  }

  public void addCancelEventListener(
      ComponentEventListener<CancelEvent> listener) {
    addListener(CancelEvent.class, listener);
  }

  public void addConfirmEventListener(
      ComponentEventListener<ConfirmEvent> listener) {
    addListener(ConfirmEvent.class, listener);
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
    var groupEntry = new ExperimentalGroupInput(experimentalVariableLevels, true);
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
      var groupEntry = new ExperimentalGroupInput(experimentalVariableLevels, true);
      experimentalGroupsCollection.add(groupEntry);
      groupEntry.addRemoveEventListener(event -> removeExperimentalGroupEntry(event.getSource()));
    }
  }

  /**
   * Provides experimental groups defined by the user. Only group information in enabled components
   * is returned, as the list of existing groups cannot be changed in "add group" mode.
   * @return a collection of {@link ExperimentalGroupContent}, describing the group size (biological
   * replicates) and the variable level combination.
   * @since 1.0.0
   */
  public Collection<ExperimentalGroupContent> experimentalGroups() {
    return this.experimentalGroupsCollection.getChildren()
        .filter(component -> component.getClass().equals(ExperimentalGroupInput.class)
            && ((ExperimentalGroupInput) component).isEnabled())
        .map(experimentalGroupEntry -> convert((ExperimentalGroupInput) experimentalGroupEntry))
        .toList();
  }

  public record ExperimentalGroupContent(int size, List<VariableLevel> variableLevels) {}

  public static class ConfirmEvent extends
      life.qbic.datamanager.views.general.ConfirmEvent<ExperimentalGroupsDialog> {


    public ConfirmEvent(ExperimentalGroupsDialog source, boolean fromClient) {
      super(source, fromClient);
    }
  }

  public static class CancelEvent extends
      life.qbic.datamanager.views.general.CancelEvent<ExperimentalGroupsDialog> {


    public CancelEvent(ExperimentalGroupsDialog source, boolean fromClient) {
      super(source, fromClient);
    }
  }

}
