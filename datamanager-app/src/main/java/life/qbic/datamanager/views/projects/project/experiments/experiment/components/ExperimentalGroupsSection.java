package life.qbic.datamanager.views.projects.project.experiments.experiment.components;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import life.qbic.datamanager.views.projects.project.experiments.experiment.ExperimentalGroupInput;
import life.qbic.datamanager.views.projects.project.experiments.experiment.ExperimentalGroupInput.VariableLevel;

/**
 * Inline editor for experimental groups.
 *
 * <p>Extracted from the former {@code ExperimentalGroupsDialog} so the experiment design route can
 * host it directly. The group entries, the cross-row duplicate-condition validation and the result
 * conversion are unchanged; only the dialog shell (header, footer, open/close) is gone. The section
 * renders its own save action and emits {@link SaveEvent} instead of a dialog confirm.</p>
 *
 * @since 1.19.0
 */
public class ExperimentalGroupsSection extends Div {

  @Serial
  private static final long serialVersionUID = 1657697182040756406L;

  /**
   * User-facing message shown on group entries that duplicate another group's condition.
   */
  private static final String DUPLICATE_CONDITION_ERROR_MESSAGE =
      "Duplicate condition: another experimental group already uses the same variable levels.";

  private final Collection<VariableLevel> experimentalVariableLevels;
  private final Div experimentalGroupsCollection = new Div();
  private final Div addNewGroupContainer = new Div();
  private final Div content = new Div();
  private final boolean editMode;
  private final List<Integer> groupsToDelete = new ArrayList<>();
  /**
   * True when the experiment defines no variables, so there are no levels to build a condition
   * from. The editor then states that prerequisite instead of rendering a form that cannot be
   * completed.
   */
  private final boolean locked;

  private ExperimentalGroupsSection(Collection<VariableLevel> experimentalVariableLevels,
      boolean editMode) {
    this.editMode = editMode;
    this.experimentalVariableLevels = Objects.requireNonNull(experimentalVariableLevels);
    this.locked = experimentalVariableLevels.isEmpty();
    layoutComponent();
  }

  private ExperimentalGroupsSection(Collection<VariableLevel> experimentalVariableLevels,
      Collection<ExperimentalGroupContent> experimentalGroupContents, boolean editMode) {
    this(experimentalVariableLevels, editMode);
    this.experimentalGroupsCollection.removeAll();
    addEntries(experimentalVariableLevels, experimentalGroupContents);
  }

  /**
   * Creates an empty section for creating new experimental groups.
   *
   * @param experimentalVariables the variable levels to choose from
   */
  public static ExperimentalGroupsSection empty(Collection<VariableLevel> experimentalVariables) {
    return new ExperimentalGroupsSection(experimentalVariables, false);
  }

  /**
   * Creates a section prefilled with editable experimental groups.
   *
   * @param experimentalVariables    available variable levels
   * @param experimentalGroupContents existing groups used to prefill the UI
   */
  public static ExperimentalGroupsSection editable(Collection<VariableLevel> experimentalVariables,
      Collection<ExperimentalGroupContent> experimentalGroupContents) {
    return new ExperimentalGroupsSection(experimentalVariables, experimentalGroupContents, true);
  }

  /**
   * Creates a section where existing groups are shown read-only, but new groups can be added.
   *
   * <p>This mode is used when existing groups must not be modified (e.g., samples already
   * registered), but users may still add additional groups.</p>
   */
  public static ExperimentalGroupsSection nonEditable(
      Collection<VariableLevel> experimentalVariables,
      Collection<ExperimentalGroupContent> experimentalGroupContents) {
    ExperimentalGroupsSection section = new ExperimentalGroupsSection(experimentalVariables,
        experimentalGroupContents, false);
    section.addNewGroupEntry();
    return section;
  }

  /** Whether existing groups can be modified (as opposed to only adding new ones). */
  public boolean isEditMode() {
    return editMode;
  }

  private void addEntries(Collection<VariableLevel> experimentalVariableLevels,
      Collection<ExperimentalGroupContent> experimentalGroupContents) {
    experimentalGroupContents.stream().map(group -> {
      var groupEntry = new ExperimentalGroupInput(experimentalVariableLevels, editMode);
      groupEntry.setGroupName(group.name());
      groupEntry.setGroupNumber(group.groupNumber());
      groupEntry.setGroupId(group.id());
      groupEntry.setCondition(group.variableLevels());
      groupEntry.setReplicateCount(group.size());
      groupEntry.setEnabled(editMode);
      groupEntry.addRemoveEventListener(listener -> removeExperimentalGroupEntry(groupEntry));
      groupEntry.addValueChangeListener(e -> validateNoDuplicateConditions());
      return groupEntry;
    }).forEach(experimentalGroupsCollection::add);
  }

  private static ExperimentalGroupContent convert(ExperimentalGroupInput experimentalGroupInput) {
    return new ExperimentalGroupContent(experimentalGroupInput.getGroupId(),
        experimentalGroupInput.groupNumber(), experimentalGroupInput.getName(),
        experimentalGroupInput.getReplicateCount(),
        experimentalGroupInput.getCondition());
  }

  private void layoutComponent() {
    content.addClassName("experimental-groups-section-content");

    if (locked) {
      layoutLockedState();
      return;
    }

    content.add(experimentalGroupsCollection);
    content.add(addNewGroupContainer);

    experimentalGroupsCollection.addClassName("group-collection");
    addNewGroupContainer.addClassName("add-new-group-action");
    addNewGroupEntry();

    var addNewGroupIcon = new Icon(VaadinIcon.PLUS);
    addNewGroupIcon.addClickListener(listener -> addNewGroupEntry());
    Span addGroupHelperText = new Span("Add Experimental Group");
    addGroupHelperText.addClickListener(listener -> addNewGroupEntry());
    addNewGroupContainer.add(addNewGroupIcon, addGroupHelperText);

    Button save = new Button("Save groups");
    save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    save.addClassName("experimental-groups-save");
    save.addClickListener(event -> {
      validateNoDuplicateConditions();
      if (isValid()) {
        fireEvent(new SaveEvent(this, event.isFromClient()));
      }
    });
    Div actionBar = new Div(save);
    actionBar.addClassName("inline-section-actions");
    // Action first so the save sits at the top-right, aligned with the metadata section's save.
    add(actionBar, content);
  }

  /**
   * Renders the state in which no group can be defined yet, because the experiment has no
   * variables and therefore no levels for a condition to select.
   *
   * <p><b>Why this exists.</b> Without variables the condition field offered an empty list while the
   * input's validator was {@code levels.size() == variableCount} with {@code variableCount == 0}.
   * Selecting nothing therefore passed validation, so "Save groups" fired, {@code experimentalGroups()}
   * dropped the condition-less entry, and {@code addExperimentalGroups} returned early on the empty
   * list. The net effect was that clicking Save did <em>nothing at all</em>: no save, no error, no
   * toast. A validator must only fire when the user can satisfy it, and here they could not -- the
   * empty dropdown had no next action. So the form is replaced by the prerequisite that is actually
   * missing, with the way to resolve it.</p>
   */
  private void layoutLockedState() {
    Div lockedState = new Div();
    lockedState.addClassName("experimental-groups-locked");

    Span title = new Span("Define variables first");
    title.addClassName("experimental-groups-locked-title");
    Span explanation = new Span("A group's condition selects one level per variable, so the "
        + "variables and their levels have to exist before a group can be described. Adding the "
        + "first experimental group is what unlocks sample registration.");
    explanation.addClassName("experimental-groups-locked-text");
    lockedState.add(title, explanation);

    Button goToVariables = new Button("Go to variables");
    goToVariables.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    goToVariables.addClassName("experimental-groups-locked-action");
    goToVariables.addClickListener(
        event -> fireEvent(new GoToVariablesRequestedEvent(this, event.isFromClient())));
    lockedState.add(goToVariables);

    add(lockedState);
  }

  /**
   * Raised when the user asks to be taken to the variables section. The section does not select the
   * tab itself: the owning page builds the tabs, so it owns which one is shown.
   */
  public void addGoToVariablesListener(
      ComponentEventListener<GoToVariablesRequestedEvent> listener) {
    addListener(GoToVariablesRequestedEvent.class, listener);
  }

  public static class GoToVariablesRequestedEvent extends
      ComponentEvent<ExperimentalGroupsSection> {

    @Serial
    private static final long serialVersionUID = 1L;

    public GoToVariablesRequestedEvent(ExperimentalGroupsSection source, boolean fromClient) {
      super(source, fromClient);
    }
  }

  /** Whether the editor is showing the missing-variables prerequisite instead of a group form. */
  public boolean isLocked() {
    return locked;
  }

  /** Registers a listener invoked when the user saves valid groups. */
  public void addSaveListener(ComponentEventListener<SaveEvent> listener) {
    addListener(SaveEvent.class, listener);
  }

  private void addNewGroupEntry() {
    var groupEntry = new ExperimentalGroupInput(experimentalVariableLevels, true);
    experimentalGroupsCollection.add(groupEntry);
    groupEntry.addRemoveEventListener(event -> removeExperimentalGroupEntry(event.getSource()));
    groupEntry.addValueChangeListener(e -> validateNoDuplicateConditions());
    validateNoDuplicateConditions();
  }

  void removeExperimentalGroupEntry(ExperimentalGroupInput entry) {
    experimentalGroupsCollection.getChildren().filter(entry::equals)
        .findAny().ifPresent(experimentalGroupsCollection::remove);
    groupsToDelete.add(entry.groupNumber());
    refreshGroupEntries();
    validateNoDuplicateConditions();
  }

  private void refreshGroupEntries() {
    if (experimentalGroupsCollection.getChildren().toList().isEmpty()) {
      var groupEntry = new ExperimentalGroupInput(experimentalVariableLevels, true);
      experimentalGroupsCollection.add(groupEntry);
      groupEntry.addRemoveEventListener(event -> removeExperimentalGroupEntry(event.getSource()));
      groupEntry.addValueChangeListener(e -> validateNoDuplicateConditions());
    }
  }

  /**
   * Returns the experimental groups defined by the user. Only entries with a non-empty condition are
   * returned; callers should check {@link #isValid()} first.
   */
  public Collection<ExperimentalGroupContent> experimentalGroups() {
    return this.experimentalGroupsCollection.getChildren()
        .filter(component -> component.getClass().equals(ExperimentalGroupInput.class))
        .map(ExperimentalGroupInput.class::cast)
        .filter(groupInput -> !groupInput.getCondition().isEmpty())
        .map(ExperimentalGroupsSection::convert)
        .toList();
  }

  /**
   * Validates the current state, including cross-row duplicate conditions.
   *
   * <p>Always {@code false} while locked: the section is showing a prerequisite rather than a form,
   * so it cannot hold a saveable state. Reporting {@code true} there is what let a click on Save
   * become a silent no-op.</p>
   */
  public boolean isValid() {
    if (locked) {
      return false;
    }
    validateNoDuplicateConditions();
    return this.experimentalGroupsCollection.getChildren()
        .filter(component -> component.getClass().equals(ExperimentalGroupInput.class))
        .noneMatch(g -> ((ExperimentalGroupInput) g).isInvalid());
  }

  /** The group numbers of groups the user removed; used to delete them on save. */
  public List<Integer> groupsToDelete() {
    return this.groupsToDelete.stream().toList();
  }

  private List<ExperimentalGroupInput> groupInputs() {
    return this.experimentalGroupsCollection.getChildren()
        .filter(component -> component.getClass().equals(ExperimentalGroupInput.class))
        .map(ExperimentalGroupInput.class::cast)
        .toList();
  }

  private void validateNoDuplicateConditions() {
    List<ExperimentalGroupInput> inputs = groupInputs();
    inputs.forEach(this::clearDuplicateConditionError);
    var conditionToInputs = inputs.stream()
        .filter(in -> !in.getCondition().isEmpty())
        .collect(Collectors.groupingBy(this::conditionKey));
    conditionToInputs.values().stream()
        .filter(list -> list.size() > 1)
        .flatMap(List::stream)
        .forEach(this::markDuplicateConditionError);
  }

  private String conditionKey(ExperimentalGroupInput input) {
    Set<VariableLevel> unique = new HashSet<>(input.getCondition());
    return unique.stream()
        .map(this::variableLevelKey)
        .sorted()
        .collect(Collectors.joining("|"));
  }

  private String variableLevelKey(VariableLevel level) {
    return new StringJoiner("::")
        .add(Objects.toString(level.variableName(), ""))
        .add(Objects.toString(level.value(), ""))
        .add(Objects.toString(level.unit(), ""))
        .toString();
  }

  private void markDuplicateConditionError(ExperimentalGroupInput input) {
    input.setConditionInvalid(true);
    input.setConditionErrorMessage(DUPLICATE_CONDITION_ERROR_MESSAGE);
  }

  private void clearDuplicateConditionError(ExperimentalGroupInput input) {
    if (DUPLICATE_CONDITION_ERROR_MESSAGE.equals(input.getConditionErrorMessage())) {
      input.setConditionErrorMessage(null);
      input.setConditionInvalid(false);
    }
  }

  /** Emitted when the user saves valid groups. */
  public static class SaveEvent extends ComponentEvent<ExperimentalGroupsSection> {

    @Serial
    private static final long serialVersionUID = 1L;

    public SaveEvent(ExperimentalGroupsSection source, boolean fromClient) {
      super(source, fromClient);
    }
  }

  /**
   * Immutable result record representing the group information entered in one row.
   */
  public record ExperimentalGroupContent(long id, int groupNumber, String name, int size,
                                         List<VariableLevel> variableLevels) {

  }
}
