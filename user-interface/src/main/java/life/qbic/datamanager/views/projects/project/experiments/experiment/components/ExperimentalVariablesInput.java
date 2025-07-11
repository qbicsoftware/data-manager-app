package life.qbic.datamanager.views.projects.project.experiments.experiment.components;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static life.qbic.datamanager.views.general.dialog.InputValidation.failed;
import static life.qbic.datamanager.views.general.dialog.InputValidation.passed;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.shared.Registration;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import life.qbic.datamanager.views.general.dialog.InputValidation;
import life.qbic.datamanager.views.general.dialog.UserInput;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.ExperimentalVariablesInput.ExperimentalVariableRow.RemoveButtonClickEvent;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public class ExperimentalVariablesInput extends Div implements UserInput {


  private final Div variableInformationContainer = new Div();
  private final HashMap<ExperimentalVariableRow, VariableInformationModification> changes = new HashMap<>();

  sealed interface VariableChange permits Renaming, LevelChange, Deletion, Addition {


  }

  sealed interface Renaming extends VariableChange {

  }

  record VariableRenaming(String oldVariableName, String newVariableName) implements Renaming {

  }

  sealed interface LevelChange extends VariableChange {

  }

  record VariableLevelChange(String variableName,)

  sealed

  interface Deletion extends VariableChange {


  }

  sealed interface Addition extends VariableChange {


  }

  private List<ExperimentalVariableRow> initialState;

  public ExperimentalVariablesInput() {
    initLayout();
    addNew(); //provide first empty row
    markAsInitialized();
  }

  public ExperimentalVariablesInput(
      List<ExperimentalVariableInformation> experimentalVariableInformationList) {
    initLayout();
    for (ExperimentalVariableInformation experimentalVariableInformation : experimentalVariableInformationList) {
      edit(experimentalVariableInformation);
    }
    markAsInitialized();
  }

  private void initLayout() {
    variableInformationContainer.addClassNames("flex-vertical");
    add(variableInformationContainer);
    var addVariableIcon = VaadinIcon.PLUS.create();
    addVariableIcon.addClassNames("color-primary clickable");
    addVariableIcon.addClickListener(it -> addNew());
    Span addVariableText = new Span("Add Experimental Variable");
    addVariableText.addClassNames("color-primary clickable");
    addVariableText.addClickListener(it -> addNew());
    var addVariableControl = new Div(addVariableIcon, addVariableText);
    addVariableControl.addClassNames("flex-horizontal", "gap-04");
    add(addVariableControl);
  }

  private void markAsInitialized() {
    initialState = rows().stream().filter(it -> !it.isEmpty()).toList();
  }


  public void edit(ExperimentalVariableInformation variableInformation) {
    addRow(new ExperimentalVariableRow(variableInformation));
  }

  public void addNew() {
    addRow(new ExperimentalVariableRow());
  }

  public enum LockMode {
    COMPLETE,
    RENAME_ONLY
  }

  public void lock(ExperimentalVariableInformation experimentalVariableInformation,
      LockMode lockMode) {
    Objects.requireNonNull(experimentalVariableInformation);
    ExperimentalVariableRow experimentalVariableRow = rows().stream()
        .filter(it -> experimentalVariableInformation.equals(it.initialValue))
        .findAny().orElseThrow(() -> new IllegalArgumentException(
            "Variable " + experimentalVariableInformation + " is not set in this dialog"));
    Consumer<ExperimentalVariableRow> locking = switch (lockMode) {
      case COMPLETE -> row -> row.setEnabled(false);
      case RENAME_ONLY -> row -> row.setRenameOnly(true);
    };
    locking.accept(experimentalVariableRow);
  }

  private void addRow(ExperimentalVariableRow rowLayout) {
    rowLayout.setRemoveButtonListener(it -> removeRow(rowLayout, it));
    rowLayout.addValueChangeListener(event -> recordChange(rowLayout, event));
    variableInformationContainer.addComponentAtIndex(rows().size(), rowLayout);
  }

  private void recordChange(
      ExperimentalVariableRow rowLayout,
      ComponentValueChangeEvent<CustomField<ExperimentalVariableInformation>, ExperimentalVariableInformation> event) {
    changes.put(rowLayout,
        new VariableInformationModification(rowLayout.initialValue, event.getValue()));
  }

  private void removeRow(ExperimentalVariableRow rowLayout, RemoveButtonClickEvent it) {
    variableInformationContainer.remove(it.origin());
    changes.put(rowLayout, VariableInformationModification.deletion(rowLayout.initialValue));
    if (rows().isEmpty()) {
      //if there are no rows after the deletion, create an empty one.
      addNew();
    }
  }

  private List<ExperimentalVariableRow> rows() {
    return variableInformationContainer
        .getChildren()
        .filter(ExperimentalVariableRow.class::isInstance)
        .map(ExperimentalVariableRow.class::cast)
        .toList();
  }

  @NonNull
  @Override
  public InputValidation validate() {
    List<ExperimentalVariableRow> rows = rows();
    if (rows.isEmpty()) {
      return passed();
    }
    if (rows.stream()
        .map(ExperimentalVariableRow::validate)
        .allMatch(InputValidation::hasPassed)) {
      return passed();
    }
    return failed();
  }

  @Override
  public boolean hasChanges() {
    List<ExperimentalVariableRow> rows = rows();
    if (initialState.isEmpty() && rows.isEmpty()) {
      return false;
    }
    if (initialState.size() != getChanges().size()) {
      // added or removed something
      return true;
    } else {
      //same length
      if (rows.stream().anyMatch(ExperimentalVariableRow::hasChanges)) {
        return true;
      }
      return !new HashSet<>(rows.stream()
          .map(HasValue::getOptionalValue).toList())
          .containsAll(initialState.stream()
              .map(ExperimentalVariableRow::getOptionalValue).toList());
    }
  }

  public record VariableInformationModification(
      @Nullable ExperimentalVariableInformation oldInformation,
      @Nullable ExperimentalVariableInformation newInformation) {

    static VariableInformationModification deletion(
        ExperimentalVariableInformation oldInformation) {
      return new VariableInformationModification(oldInformation, null);
    }

    static VariableInformationModification creation(
        ExperimentalVariableInformation newInformation) {
      return new VariableInformationModification(null, newInformation);
    }


    public boolean isRename() {
      if (isCreation() || isDeletion()) {
        return false;
      }
      // both should be there if it is neither a creation nor a deletion
      requireNonNull(oldInformation);
      requireNonNull(newInformation);
      return !newInformation.variableName().equals(oldInformation.variableName());
    }

    public boolean isDeletion() {
      return nonNull(oldInformation) && isNull(newInformation);
    }

    public boolean isCreation() {
      return isNull(oldInformation) && nonNull(newInformation);
    }

    public boolean hasChanged() {
      if (isNull(oldInformation)) {
        return nonNull(newInformation);
      }
      if (isNull(newInformation)) {
        return true;
      }
      return oldInformation.equals(newInformation);
    }
  }

  /**
   * This method filters out empty variable information and returns all filled variable
   * information.
   *
   * @return a list of variable information
   */
  public List<VariableInformationModification> getChanges() {
    return rows().stream()
        .filter(it -> !it.isEmpty())
        .map(row -> new VariableInformationModification(row.initialValue, row.getValue()))
        .filter(VariableInformationModification::hasChanged)
        .toList();
  }

  /**
   * This method filters out empty variable information and returns all filled variable
   * information.
   *
   * @return a list of variable information
   */
  public List<ExperimentalVariableInformation> getRenamedVariableInformation() {
    return rows().stream()
        .filter(ExperimentalVariableRow::wasRenamed)
        .map(HasValue::getOptionalValue)
        .filter(Optional::isPresent)
        .map(Optional::orElseThrow)
        .toList();
  }

  public record ExperimentalVariableInformation(String variableName, List<String> levels,
                                                @Nullable String unit) implements Serializable {

    public ExperimentalVariableInformation {
      Objects.requireNonNull(variableName);
      Objects.requireNonNull(levels);
      levels = List.copyOf(levels);
    }
  }


  /**
   * A layout containing rows for experimental variable input
   */
  private static final class ExperimentalVariableRow extends
      CustomField<ExperimentalVariableInformation> implements UserInput {


    @Serial
    private static final long serialVersionUID = 8301389606566298879L;
    private final TextField nameField = new TextField("Experimental Variable");
    private final TextField unitField = new TextField("Unit");
    private final TextArea levelArea = new TextArea("Levels");
    private final Icon deleteIcon = new Icon(VaadinIcon.CLOSE_SMALL);
    private Registration clickListener;
    private ExperimentalVariableInformation initialValue;
    private final Div fieldsContainer = new Div();
    private final List<AbstractField<?, ?>> fields = List.of(nameField, unitField, levelArea);
    private boolean isRenameOnly = false;

    ExperimentalVariableRow(ExperimentalVariableInformation initialValue) {
      super(null);
      init();
      setValue(initialValue);
    }

    ExperimentalVariableRow() {
      super(null);
      init();
      setValue(getEmptyValue());
    }

    /**
     * @param value the new value. Can be null.
     */
    @Override
    public void setValue(ExperimentalVariableInformation value) {
      super.setValue(value);
      this.initialValue = value;
    }

    @Override
    public void setReadOnly(boolean readOnly) {
      super.setReadOnly(readOnly);
      fields.forEach(field -> field.setReadOnly(readOnly));
      deleteIcon.getElement().setProperty("readonly", readOnly);
      deleteIcon.setVisible(!readOnly);
    }

    /**
     * Only enables fields for renaming
     *
     * @param renameOnly true if only the renaming field should be enabled.
     */
    public void setRenameOnly(boolean renameOnly) {
      fields.forEach(field -> field.setEnabled(!renameOnly));
      deleteIcon.getElement().setEnabled(!renameOnly);
      deleteIcon.setVisible(!renameOnly);
      nameField.setEnabled(true);
      this.isRenameOnly = renameOnly;
    }

    @Override
    public void setEnabled(boolean enabled) {
      super.setEnabled(enabled);
      fields.forEach(field -> field.setEnabled(enabled));
      deleteIcon.getElement().setEnabled(enabled);
      deleteIcon.setVisible(enabled);
    }

    @Override
    protected ExperimentalVariableInformation generateModelValue() {
      if (nameField.isEmpty() && levelArea.isEmpty() && unitField.isEmpty()) {
        return null;
      }
      return new ExperimentalVariableInformation(nameField.getValue(),
          readLevelValues(),
          readUnitValue().orElse(null));
    }

    private void setLevelsValue(List<String> levels) {
      this.levelArea.setValue(String.join("\n", levels));
    }

    private List<String> readLevelValues() {
      return levelArea.getValue().lines().filter(it -> !it.isBlank()).toList();
    }

    private void setUnitValue(@Nullable String unit) {
      if (isNull(unit)) {
        this.unitField.clear();
        return;
      }
      this.unitField.setValue(unit);
    }

    private Optional<String> readUnitValue() {
      if (unitField.isEmpty()) {
        return Optional.empty();
      }
      return Optional.of(unitField.getValue());
    }

    @Override
    protected void setPresentationValue(ExperimentalVariableInformation newPresentationValue) {
      if (isNull(newPresentationValue)) {
        nameField.clear();
        unitField.clear();
        levelArea.clear();
        return;
      }
      nameField.setValue(newPresentationValue.variableName());
      setUnitValue(newPresentationValue.unit());
      setLevelsValue(newPresentationValue.levels());
    }

    private void init() {
      fieldsContainer.addClassNames("flex-horizontal", "column-gap-05",
          "flex-align-items-baseline");
      nameField.setRequired(true);
      nameField.setPlaceholder("e.g. age");
      unitField.setPlaceholder("e.g. years");
      levelArea.setRequired(true);
      levelArea.setHelperText(
          "Please enter each level on a new line. Comma separated values are treated as a single level.");
      levelArea.setPlaceholder("""
          32
          42
          68
          """);
      deleteIcon.addClassNames("color-primary");
      /*Span around Icon is necessary otherwise icon size will be scaled down if a scrollbar appears*/
      fieldsContainer.add(nameField, unitField, levelArea, new Span(deleteIcon));
      add(fieldsContainer);
    }

    public void setRemoveButtonListener(
        Consumer<RemoveButtonClickEvent> closeListener) {
      if (Objects.nonNull(clickListener)) {
        clickListener.remove();
      }
      clickListener = deleteIcon.addClickListener(it -> closeListener.accept(
          new RemoveButtonClickEvent(this)));
    }

    @Override
    @NonNull
    public InputValidation validate() {
      if (isEmpty()) {
        return passed();
      }
      if (nameField.isRequired() && nameField.isEmpty()) {
        nameField.setInvalid(true);
      }
      if (unitField.isRequired() && unitField.isEmpty()) {
        unitField.setInvalid(true);
      }
      if (levelArea.isRequired() && levelArea.isEmpty()) {
        levelArea.setInvalid(true);
      }
      if (nameField.isInvalid() || unitField.isInvalid() || levelArea.isInvalid()) {
        setInvalid(true);
        return failed();
      }
      setInvalid(false);
      return passed();
    }

    @Override
    public boolean hasChanges() {
      if (isNull(initialValue) && !isEmpty()) {
        //something was filled in where nothing was before
        return true;
      }
      return getOptionalValue().map(it -> !it.equals(initialValue)).orElse(false);
    }

    public boolean wasRenamed() {
      if (isNull(initialValue)) {
        return getOptionalValue()
            .map(ExperimentalVariableInformation::variableName)
            .isPresent();
      }
      var initialName = initialValue.variableName();
      return getOptionalValue()
          .map(ExperimentalVariableInformation::variableName)
          .map(currentName -> !currentName.equals(initialName))
          .orElse(true);
    }

    public boolean hasUnitChanged() {
      if (isNull(initialValue)) {
        return getOptionalValue()
            .map(ExperimentalVariableInformation::unit)
            .isPresent();
      }
      var initialUnit = initialValue.unit();
      return getOptionalValue()
          .map(ExperimentalVariableInformation::unit)
          .map(currentUnit -> !currentUnit.equals(initialUnit))
          .orElse(true);
    }

    public boolean haveLevelsChanged() {
      if (isNull(initialValue)) {
        return getOptionalValue()
            .map(ExperimentalVariableInformation::levels)
            .filter(levels -> !levels.isEmpty())
            .isPresent();
      }
      var initialLevels = String.join("\n", initialValue.levels());
      return getOptionalValue()
          .map(ExperimentalVariableInformation::levels)
          .map(levels -> String.join("\n", levels))
          .map(it -> !it.equals(initialLevels))
          .orElse(true);
    }

    public record RemoveButtonClickEvent(ExperimentalVariableRow origin) {

    }

  }
}
