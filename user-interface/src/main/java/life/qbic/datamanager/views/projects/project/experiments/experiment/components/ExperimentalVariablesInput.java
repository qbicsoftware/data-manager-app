package life.qbic.datamanager.views.projects.project.experiments.experiment.components;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static life.qbic.datamanager.views.general.dialog.InputValidation.failed;
import static life.qbic.datamanager.views.general.dialog.InputValidation.passed;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.EventData;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.dnd.DropEffect;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.shared.Registration;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import life.qbic.datamanager.views.general.dialog.InputValidation;
import life.qbic.datamanager.views.general.dialog.UserInput;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.ExperimentalVariablesInput.ExperimentalVariableRow.RemoveButtonClickEvent;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public class ExperimentalVariablesInput extends Div implements UserInput {


  private final Div variableInformationContainer = new Div();
  private List<ExperimentalVariableRow> initialState;

  public ExperimentalVariablesInput() {
    initLayout();
    markInitialState();
    ensureAtLeastOneInputPresent();
  }

  private void initLayout() {
    variableInformationContainer.addClassNames("flex-vertical");
    add(variableInformationContainer);
    var addVariableIcon = VaadinIcon.PLUS.create();
    addVariableIcon.addClassNames("color-primary-text clickable");
    addVariableIcon.addClickListener(it -> addNewVariableInput());
    Span addVariableText = new Span("Add Experimental Variable");
    addVariableText.addClassNames("color-primary-text clickable");
    addVariableText.addClickListener(it -> addNewVariableInput());
    var addVariableControl = new Div(addVariableIcon, addVariableText);
    addVariableControl.addClassNames("flex-horizontal", "gap-04");
    add(addVariableControl);
  }

  private void markInitialState() {
    initialState = rows().stream().filter(it -> !it.isEmpty()).toList();
  }

  public void editUnusedVariable(@NonNull ExperimentalVariableInformation variableInformation) {
    addRow(new ExperimentalVariableRow(variableInformation));
  }

  public void editUsedVariable(@NonNull ExperimentalVariableInformation variableInformation,
      @NonNull List<String> usedLevels) {
    if (!(new HashSet<>(variableInformation.levels())).containsAll(usedLevels)) {
      throw new IllegalArgumentException(
          "The variable information must contain all used levelInputs");
    }
    addRow(new ExperimentalVariableRow(variableInformation));
  }

  public void addNewVariableInput() {
    addRow(new ExperimentalVariableRow());
  }

  void deleteUnusedVariable(@NonNull String variableName) {
    //TODO implement
    throw new RuntimeException("Not implemented");
  }


  void lockUnit(@NonNull ExperimentalVariableRow row) {
    //TODO implement
    throw new RuntimeException("Not implemented");
  }

  void lockLevels(@NonNull ExperimentalVariableRow row, @NonNull String... levels) {
    //TODO implement
    throw new RuntimeException("Not implemented");
  }

  void setUnit(@NonNull String unit) {
    //TODO implement
    throw new RuntimeException("Not implemented");
  }

  public List<VariableChange> getChanges() {
    //TODO implement compute changes and report
    throw new RuntimeException("Not implemented");
  }

  record VariableChange(
      String oldVarName,
      String newVarName,
      String oldUnit,
      String newUnit,
      List<String> oldLevels,
      List<String> newLevels) {

    VariableChange {
      boolean noOldVarName = isNull(oldVarName) || oldVarName.isBlank();
      boolean noNewVarName = isNull(newVarName) || newVarName.isBlank();
      if (noOldVarName && noNewVarName) {
        throw new IllegalArgumentException("No variable name provided");
      }
    }

    Optional<VariableRenaming> renaming() {
      if (isVariableRenaming()) {
        return Optional.of(new VariableRenaming(oldVarName, newVarName));
      }
      return Optional.empty();
    }

    Optional<VariableUnitChange> unitChange() {
      if (isVariableUnitChange()) {
        return Optional.of(new VariableUnitChange(oldUnit, newUnit));
      }
      return Optional.empty();
    }

    Optional<VariableCreation> creation() {
      if (isVariableCreation()) {
        return Optional.of(new VariableCreation(new ExperimentalVariableInformation(
            newVarName,
            newLevels,
            newUnit
        )));
      }
      return Optional.empty();
    }

    Optional<VariableDeletion> deletion() {
      if (isVariableDeletion()) {
        return Optional.of(new VariableDeletion(oldVarName));
      }
      return Optional.empty();
    }

    boolean isVariableRenaming() {
      return isNull(oldVarName) || !oldVarName.equals(newVarName);
    }

    boolean isVariableCreation() {
      return isNull(oldVarName) && !isNull(newVarName);
    }

    boolean isVariableUnitChange() {
      if (nonNull(oldUnit)) {
        if (isNull(newUnit)) {
          return true;
        }
        return oldUnit.equals(newUnit);
      }
      return nonNull(newUnit);
    }

    boolean isVariableDeletion() {
      return nonNull(oldVarName) && isNull(newVarName);
    }

    List<String> deletedLevels() {
      return oldLevels.stream().filter(it -> !newLevels.contains(it)).toList();
    }

    List<String> addedLevels() {
      return newLevels.stream().filter(it -> !oldLevels.contains(it)).toList();
    }
  }


  record VariableRenaming(String oldName, String newName) {

  }

  record VariableCreation(ExperimentalVariableInformation variableInformation) {
  }

  record VariableDeletion(String variableName) {

  }

  record VariableUnitChange(String oldUnit, String newUnit) {

  }

  // when variable was created it cannot be deleted
  // when variable was deleted no other changes to the variable apply
  // when level was deleted no other changes to the level apply

  // the parent needs to know which variables to create, which variables to delete, which variables to rename, which levels to create and which levels to remove
  // unit changing is only allowed if none of the levels were in use which is only the case if no exp groups exist

  // renaming must be done last or first, as the variable might not be found otherwise
  // changes to one variable must happen in one flow as to not block each other.

  /* so the caller must
  1. query variable deletion -> make a snapshot for rollback and delete
  2. query for variable level deletion -> make a snapshot for rollback and delete
  3. query for variable unit change -> make a snapshot for rollback and delete
  4. query for variable level addition -> make a snapshot for rollback and create
  5. query for variable creation -> make a snapshot for rollback and create
  6. query for variable renaming -> make a snapshot for rollback and rename
   */

  ExperimentalVariableRow getRow(ExperimentalVariableInformation experimentalVariableInformation) {
    return rows().stream()
        .filter(it -> experimentalVariableInformation.equals(it.initialValue))
        .findAny().orElseThrow(() -> new IllegalArgumentException(
            "Variable " + experimentalVariableInformation + " is not set in this dialog"));
  }

  private void addRow(ExperimentalVariableRow rowLayout) {
    rowLayout.setRemoveButtonListener(it -> removeRow(rowLayout, it));
    //TODO record change
//    rowLayout.addValueChangeListener(event -> recordChange(rowLayout, event));
    variableInformationContainer.addComponentAtIndex(rows().size(), rowLayout);
  }

  private void removeRow(ExperimentalVariableRow rowLayout, RemoveButtonClickEvent it) {
    variableInformationContainer.remove(it.origin());
    ensureAtLeastOneInputPresent();
  }

  private void ensureAtLeastOneInputPresent() {
    if (rows().isEmpty()) {
      //if there are no rows after the deletion, create an empty one.
      addNewVariableInput();
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

  public record ExperimentalVariableInformation(String variableName, List<String> levels,
                                                @Nullable String unit) implements Serializable {

    public ExperimentalVariableInformation {
      requireNonNull(variableName);
      requireNonNull(levels);
      levels = List.copyOf(levels);
    }
  }

  static final class LevelArea extends Composite<Div> {

    private Div helperText;
    private Div levelContainer;
    private final List<TextField> levels = new ArrayList<>();
    private final List<DropTarget<?>> dropTargets = new ArrayList<>();
    private Component helper;
    private Div errorText;

    public LevelArea() {
      var content = getContent();
      helperText = new Div();
      helperText.setVisible(false);
      levelContainer = new Div();
      levelContainer.addClassNames("flex-vertical");
      errorText = new Div("Please provide at least one level.");
      errorText.setVisible(false);
      var addLevelControls = createAddLevelControls();
      content.add(levelContainer, addLevelControls, errorText, helperText);
      addLevelPasteListener();
    }

    public void setRequired(boolean required) {
      getContent().getElement().setProperty("required", required);
    }

    public boolean isRequired() {
      return getContent().getElement().getProperty("required", false);
    }

    public void setInvalid(boolean invalid) {
      getContent().getElement().setProperty("invalid", invalid);
      errorText.setVisible(invalid);
    }


    private TextField createAddLevelControls() {
      TextField textField = new TextField();
      var addBtn = new Button(VaadinIcon.PLUS.create());
      addBtn.setText("Add");
      addBtn.setAriaLabel("Add Level");
      addBtn.addClickListener(clickEvent -> {
        var value = textField.getOptionalValue();
        if (value.map(it -> !it.isBlank()).isEmpty()) {
          return;
        }
        addLevel(textField.getValue());
        textField.clear();
        textField.focus();
      });
      addBtn.addClickShortcut(Key.ENTER);
      textField.setPlaceholder("Enter a new level value");
      textField.setSuffixComponent(addBtn);
      return textField;
    }

    Optional<String> getHelperText() {
      return Optional.ofNullable(helperText.getText())
          .filter(it -> !it.isBlank());
    }

    void addLevel(@NonNull String value) {
      requireNonNull(value);
      if (value.isBlank()) {
        return;
      }
      TextField textField = new TextField();
      textField.setValue(value);
      var deleteBtn = VaadinIcon.TRASH.create();
      deleteBtn.addClassName("clickable");
      deleteBtn.addClickListener(it -> {
        levels.remove(textField);
        textField.removeFromParent();
      });
      var dragSource = DragSource.create(textField);
      dragSource.setDraggable(true);
      final String draggableCssClass = "draggable";
      final String draggingInProgressCssClass = "dragging";
      var dragHandle = VaadinIcon.ELLIPSIS_V.create();
      dragHandle.addClassName(draggableCssClass);
      dragSource.addDragStartListener(it -> {
        //only allow drop in same variable
        dropTargets.forEach(target -> target.setActive(true));
        dragHandle.addClassName(draggingInProgressCssClass);
        dragHandle.removeClassName(draggableCssClass);
      });
      dragSource.addDragEndListener(it -> {
        //disable drop in this variable after drag end
        dropTargets.forEach(target -> target.setActive(false));
        dragHandle.removeClassName(draggingInProgressCssClass);
        dragHandle.addClassName(draggableCssClass);
      });
      var dropTarget = DropTarget.configure(textField, false);
      textField.addClassName("drop-above");
      dropTarget.setDropEffect(DropEffect.MOVE);
      dropTarget.addDropListener(it -> {
        if (it.getDragSourceComponent().isEmpty()) {
          return;
        }
        var dragSourceComponent = it.getDragSourceComponent().orElseThrow();
        if (dragSourceComponent instanceof TextField sourceInput) {
          int targetIndex = levelContainer.indexOf(it.getComponent());
          int sourceIndex = levelContainer.indexOf(sourceInput);
          if (targetIndex > -1 && sourceIndex > -1) {
            levelContainer.addComponentAtIndex(targetIndex, sourceInput);
          }
        }
      });
      textField.setPrefixComponent(dragHandle);
      textField.setSuffixComponent(deleteBtn);
      levelContainer.add(textField);
      dropTargets.add(dropTarget);
      levels.add(textField);
    }

    void setLevels(@NonNull List<String> values) {
      levels.forEach(Component::removeFromParent);
      levels.clear();
      values.forEach(this::addLevel);
    }

    void setHelperText(@Nullable String text) {
      this.helperText.setText(text);
      this.helperText.setVisible(getHelperText().isPresent());
    }

    List<String> getLevels() {
      return levels.stream()
          .map(TextField::getOptionalValue)
          .filter(Optional::isPresent)
          .map(Optional::get)
          .toList();
    }

    private void addLevelPasteListener() {
      addListener(PasteEvent.class, it -> {
        List<String> pastedLines = it.getClipboardText().lines()
            .filter(line -> !line.isBlank())
            .toList();
        pastedLines.forEach(this::addLevel);
      });
    }

    public boolean isEmpty() {
      return levels.stream().map(HasValue::getOptionalValue)
          .filter(Optional::isPresent)
          .map(Optional::get)
          .allMatch(String::isBlank);
    }

    @DomEvent(value = "paste",
        allowUpdates = DisabledUpdateMode.ONLY_WHEN_ENABLED,
        preventDefault = true,
        stopPropagation = true)
    public static class PasteEvent extends ComponentEvent<Component> {

      private final String clipboardText;

      /**
       * Creates a new event using the given source and indicator whether the event originated from
       * the client side or the server side.
       *
       * @param source     the source component
       * @param fromClient <code>true</code> if the event originated from the client
       *                   side, <code>false</code> otherwise
       */
      public PasteEvent(Component source, boolean fromClient,
          @EventData("event.clipboardData.getData(\"text\")") String clipboardText) {
        super(source, fromClient);
        this.clipboardText = clipboardText;
      }

      public String getClipboardText() {
        return clipboardText;
      }
    }
  }

  /**
   * A layout containing rows for experimental variable input
   */
  static final class ExperimentalVariableRow extends
      CustomField<ExperimentalVariableInformation> implements UserInput {

    @Serial
    private static final long serialVersionUID = 8301389606566298879L;
    private final TextField nameField = new TextField("Experimental Variable");
    private final TextField unitField = new TextField("Unit");
    private final LevelArea levelArea = new LevelArea();
    private final Icon deleteIcon = new Icon(VaadinIcon.CLOSE_SMALL);
    private Registration clickListener;
    private ExperimentalVariableInformation initialValue;
    private final Div fieldsContainer = new Div();
    private final List<AbstractField<?, ?>> fields = List.of(nameField, unitField);
    private final ProgressBar progressBar;

    {
      progressBar = new ProgressBar();
      progressBar.setIndeterminate(true);
      progressBar.setVisible(false);
    }

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

    private void init() {
      fieldsContainer.addClassNames("flex-horizontal", "column-gap-05",
          "flex-align-items-baseline");
      nameField.setRequired(true);
      nameField.setPlaceholder("e.g. age");
      unitField.setPlaceholder("e.g. years");
      levelArea.setRequired(true);
      levelArea.setHelperText(
          "Please enter each level on a new line. Comma separated values are treated as a single level.");
//      levelArea.setPlaceholder("""
//          32
//          42
//          68
//          """); TODO needed?
      deleteIcon.addClassNames("color-primary-text");
      /*Span around Icon is necessary otherwise icon size will be scaled down if a scrollbar appears*/
      fieldsContainer.add(nameField, unitField, levelArea, new Span(deleteIcon));
      add(fieldsContainer);
      add(progressBar);
      //TODO mark pending or completed for async loading
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
    public void setEnabled(boolean enabled) {
      super.setEnabled(enabled);
      fields.forEach(field -> field.setEnabled(enabled));
      deleteIcon.getElement().setEnabled(enabled);
      deleteIcon.setVisible(enabled);
    }

    @Override
    protected ExperimentalVariableInformation generateModelValue() {
      List<String> levels = levelArea.getLevels();
      if (nameField.isEmpty() && levels.isEmpty() && unitField.isEmpty()) {
        return null;
      }
      return new ExperimentalVariableInformation(nameField.getValue(),
          levels,
          readUnitValue().orElse(null));
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
        levelArea.setLevels(List.of());
        return;
      }
      nameField.setValue(newPresentationValue.variableName());
      setUnitValue(newPresentationValue.unit());
      levelArea.setLevels(newPresentationValue.levels()); //FIXME preserve locking
    }



    public void setRemoveButtonListener(
        Consumer<RemoveButtonClickEvent> closeListener) {
      if (nonNull(clickListener)) {
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
      if (nameField.isInvalid() || unitField.isInvalid() || levelArea.isEmpty()) {
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

    public record RemoveButtonClickEvent(ExperimentalVariableRow origin) {

    }

  }
}
