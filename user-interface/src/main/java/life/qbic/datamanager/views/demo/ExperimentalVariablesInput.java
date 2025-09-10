package life.qbic.datamanager.views.demo;

import static java.util.Objects.nonNull;

import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.EventData;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.HasValue.ValueChangeListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.ShortcutRegistration;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.shared.HasValidationProperties;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.shared.Registration;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import life.qbic.application.commons.CanSnapshot;
import life.qbic.application.commons.Snapshot;
import life.qbic.datamanager.views.demo.ExperimentalVariablesInput.VariableLevelsInput.LevelChange;
import life.qbic.datamanager.views.demo.ExperimentalVariablesInput.VariableRow.InvalidChangesException;
import life.qbic.datamanager.views.demo.ExperimentalVariablesInput.VariableRow.VariableChange;
import life.qbic.datamanager.views.general.ButtonFactory;
import life.qbic.datamanager.views.general.dialog.InputValidation;
import life.qbic.datamanager.views.general.dialog.UserInput;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public class ExperimentalVariablesInput extends Composite<Div> implements UserInput, CanSnapshot {

  private final Button addVariableButton = new Button("Add Variable", VaadinIcon.PLUS.create());
  private final Div variablesInput = new Div();
  private final List<Snapshot> deletedVariables = new ArrayList<>();
  private Snapshot initialState;

  public ExperimentalVariablesInput() {
    addVariableButton.addClickListener(
        e -> addVariable());
    getContent().add(new Button("Valid?", e -> System.out.println("valid = "
        + this.validate())));
    getContent().add(new Button("Has Changes?", e -> System.out.println("hasChanges? = "
        + this.hasChanges())));
    getContent().add(new Button("Print Changes", e -> System.out.println("changes = "
        + Stream.concat(
        deletedVariables.stream()
            .map(this::toRestoredVariableRow)
            .map(VariableRow::getVariableName),
        this.getVariableRows().stream().flatMap(
            (VariableRow variableRow) -> {
              try {
                List<VariableChange> changes = variableRow.getChanges();
                return changes.stream();
              } catch (InvalidChangesException ex) {
                return Stream.of();
              }
            })).toList())));
    getContent().add(new Button("Mark Initialized", e -> markInitialized()));
    getContent().add(new Button("Add all deleted variables", e -> {
      var copy = List.copyOf(deletedVariables);

      copy.reversed() //FIFO
          .forEach(deletedVariable -> {
            addRow(toRestoredVariableRow(deletedVariable));
            deletedVariables.remove(deletedVariable);
          });
    }));
    markInitialized();
  }

  private void markInitialized() {
    getVariableRows().forEach(VariableRow::markInitialized);
    this.deletedVariables.clear();
    this.initialState = this.snapshot();
  }

  @NonNull
  private List<VariableRow> getVariableRows() {
    return variablesInput.getChildren()
        .filter(VariableRow.class::isInstance)
        .map(VariableRow.class::cast)
        .toList();
  }

  @Override
  @NonNull
  protected Div initContent() {
    var body = new Div();
    body.addClassNames(
        "dialog-section border dashed padding-vertical-05 padding-horizontal-07 margin-05");
    variablesInput.addClassNames("flex-vertical gap-04");

    addVariable();

    addVariableButton.addClassNames(
        "margin-bottom-04 flex-vertical width-max-content justify-self-start button-color-primary");
    variablesInput.add(addVariableButton);
    body.add(variablesInput);
    return body;
  }

  private void addVariable() {
    VariableRow variableRow = new VariableRow();
    addRow(variableRow);
  }

  private void addRow(VariableRow variableRow) {
    variableRow.addDeleteVariableListener(it -> removeVariable(it.getSource()));
    variablesInput.addComponentAtIndex(
        Math.max(0, (int) (variablesInput.getChildren().count() - 1)),
        variableRow);
    variableRow.focus();
  }

  @NonNull
  private List<VariableRow> variableRows() {
    return variablesInput.getChildren()
        .filter(VariableRow.class::isInstance)
        .map(VariableRow.class::cast)
        .toList();
  }

  private void removeVariable(@NonNull VariableRow variableRow) {
    var initialComponentState = toRestoredVariablesInput(this.initialState);
    var initialRowInformation = toRestoredVariableRow(variableRow.initialState);
    if (initialComponentState.variableRows().stream()
        .anyMatch(row -> row.getVariableName().equals(initialRowInformation.getVariableName()))) {
      //only record deletion of variable rows that were there initially
      var snapshotOfVariableRow = variableRow.snapshot();
      deletedVariables.add(snapshotOfVariableRow);
    }
    variableRow.removeFromParent();
    if (variableRows().isEmpty()) {
      addVariable();
    }
  }

  private ExperimentalVariablesInput toRestoredVariablesInput(Snapshot snapshot) {
    var dummy = new ExperimentalVariablesInput();
    try {
      dummy.restore(snapshot);
      return dummy;
    } catch (SnapshotRestorationException e) {
      throw new IllegalStateException("Initial state not correctly set.", e);
    }
  }

  @Override
  @NonNull
  public InputValidation validate() {
    return getVariableRows().stream()
        .map(VariableRow::validate)
        .allMatch(InputValidation::hasPassed)
        ? InputValidation.passed() : InputValidation.failed();
  }

  @Override
  public boolean hasChanges() {
    return !deletedVariables.isEmpty() || getVariableRows().stream()
        .anyMatch(VariableRow::hasChanges);
  }

  record VariablesSnapshot(List<Snapshot> variableSnapshots,
                           @Nullable Snapshot initialState) implements Snapshot {

    public VariablesSnapshot {
      variableSnapshots = variableSnapshots.stream().toList();
    }

  }

  @Override
  public Snapshot snapshot() {
    return new VariablesSnapshot(getVariableRows().stream().map(VariableRow::snapshot).toList(),
        initialState);
  }

  private VariableRow toRestoredVariableRow(Snapshot snapshot) {
    var dummy = new VariableRow();
    try {
      dummy.restore(snapshot);
      return dummy;
    } catch (SnapshotRestorationException e) {
      throw new IllegalStateException("Initial state not correctly set.", e);
    }
  }

  @Override
  public void restore(Snapshot snapshot) throws SnapshotRestorationException {
    if (snapshot instanceof VariablesSnapshot variablesSnapshot) {
      if (nonNull(variablesSnapshot.initialState())
          && variablesSnapshot.initialState() instanceof VariablesSnapshot) {
        this.initialState = variablesSnapshot.initialState();
      }
      var previousChildren = getVariableRows();
      previousChildren.forEach(Component::removeFromParent);
      try {
        variablesSnapshot.variableSnapshots()
            .stream()
            .map(this::toRestoredVariableRow)
            .forEach(this::addRow);
        return;
      } catch (Exception e) {
        variableRows().forEach(Component::removeFromParent);
        previousChildren.forEach(this::addRow);
        throw e;
      }
    }
    throw new SnapshotRestorationException(
        "Unknown snapshot type. Expected %s but got %s".formatted(VariablesSnapshot.class,
            snapshot.getClass()));
  }


  static class VariableRow extends Composite<Div> implements UserInput, CanSnapshot,
      HasValidationProperties {

    private final TextField name = new TextField();
    private final TextField unit = new TextField();
    private final VariableLevelsInput variableLevels = new VariableLevelsInput();
    private final Button deleteVariable = new ButtonFactory()
        .createTertirayButton("Delete Variable", VaadinIcon.TRASH.create());
    private Snapshot initialState;

    public VariableRow() {
      deleteVariable.addClickListener(
          e -> fireEvent(new DeleteVariableEvent(this, e.isFromClient())));
      markInitialized();
    }

    public void markInitialized() {
      variableLevels.markInitialized();
      this.initialState = snapshot();
    }

    public Registration addDeleteVariableListener(
        ComponentEventListener<DeleteVariableEvent> listener) {
      return addListener(DeleteVariableEvent.class, listener);
    }


    @Override
    protected Div initContent() {
      var root = new Div();
      root.addClassNames(
          "border rounded-02 padding-04 gap-04 column-gap-05 grid-experimental-variable-input");
      var fields = new Div();
      fields.getStyle().set("grid-area", "a");
      fields.addClassNames("flex-horizontal gap-05");
      name.addClassNames("dynamic-growing-flex-item");
      name.setLabel("Variable Name");
      name.setErrorMessage("Please provide a name for the variable.");
      name.setRequired(true);
      unit.addClassNames("dynamic-growing-flex-item");
      unit.setLabel("Unit (optional)");
      unit.setRequired(false);
      variableLevels.getStyle().set("grid-area", "b");
      deleteVariable.getStyle().set("grid-area", "c");
      deleteVariable.addClassNames("width-max-content");
      fields.add(name, unit);
      root.add(fields, variableLevels, deleteVariable);
      return root;
    }

    @Override
    @NonNull
    public InputValidation validate() {
      if (this.isEmpty()) {
        setInvalid(false);
        return InputValidation.passed();
      }
      var valiableLevelsValidationFailed = !variableLevels.validate().hasPassed();
      if (name.isInvalid() || name.isEmpty() || unit.isInvalid()
          || valiableLevelsValidationFailed) {
        setInvalid(true);
        return InputValidation.failed();
      }
      setInvalid(false);
      return InputValidation.passed();
    }

    @Override
    public void setInvalid(boolean invalid) {
      HasValidationProperties.super.setInvalid(invalid);
      getElement().setAttribute("invalid", invalid);
      name.setInvalid(invalid && name.isEmpty()); //make sure to check again
      variableLevels.setInvalid(variableLevels.isInvalid());
    }

    private boolean isEmpty() {
      return name.isEmpty() && unit.isEmpty() && variableLevels.isEmpty();
    }

    private String getVariableName() {
      return name.getValue();
    }

    private Optional<String> getUnit() {
      return unit.getOptionalValue();
    }

    private List<LevelChange> getLevelChanges() {
      return variableLevels.getChanges();
    }

    @Override
    public boolean hasChanges() {
      return variableLevels.hasChanges() ||
          !initialState.equals(snapshot());
    }

    public class InvalidChangesException extends RuntimeException {

    }

    /**
     * Validates the component and returns all changes. Throws InvalidChangesException in case of
     * invalid input.
     *
     * @return a list of variable changes
     * @throws InvalidChangesException in case of invalid user input
     */
    public List<VariableChange> getChanges() throws InvalidChangesException {
      if (!hasChanges()) {
        return List.of(); //avoid unnecessary work
      }
      if (!validate().hasPassed()) {
        throw new InvalidChangesException();
      }
      if (initialState instanceof ExperimentalVariableSnapshot experimentalVariableSnapshot) {
        var previousRepresentation = new VariableRow();
        previousRepresentation.restore(experimentalVariableSnapshot);
        var changes = new ArrayList<VariableChange>();
        if (previousRepresentation.getVariableName().isBlank() && !this.getVariableName()
            .isBlank()) {
          return Collections.singletonList(
              new VariableAdded(getVariableName(), getUnit().orElse(null),
                  variableLevels.getLevels()));
        }
        if (!previousRepresentation.getVariableName().equals(this.getVariableName())) {
          changes.add(new VariableRenamed(previousRepresentation.getVariableName(),
              this.getVariableName()));
        }
        if (!previousRepresentation.getUnit().equals(this.getUnit())) {
          changes.add(new VariableUnitChanged(this.getVariableName(),
              previousRepresentation.getUnit().orElse(null),
              this.getUnit().orElse(null)));
        }
        if (!this.getLevelChanges().isEmpty()) {
          changes.add(new VariableLevelsChanged(getVariableName(), this.getLevelChanges()));
        }
        return changes;
      }
      throw new IllegalStateException();
    }

    record ExperimentalVariableSnapshot(String name, String unit,
                                        Snapshot levels, @Nullable Snapshot initialState) implements
        Snapshot, Serializable {

    }

    @Override
    public Snapshot snapshot() {
      return new ExperimentalVariableSnapshot(name.getValue(), unit.getValue(),
          variableLevels.snapshot(), initialState);
    }

    @Override
    public void restore(@NonNull Snapshot snapshot) throws SnapshotRestorationException {
      if (snapshot instanceof ExperimentalVariableSnapshot experimentalVariableSnapshot) {
        if (nonNull((experimentalVariableSnapshot).initialState())) {
          initialState = experimentalVariableSnapshot.initialState();
        }
        variableLevels.restore(experimentalVariableSnapshot.levels());
        name.setValue(experimentalVariableSnapshot.name());
        unit.setValue(experimentalVariableSnapshot.unit());
        return;
      }
      throw new SnapshotRestorationException("Cannot restore snapshot");
    }

    public void focus() {
      name.focus();
    }

    interface VariableChange {

    }

    public record VariableAdded(String name, String unit, List<String> levels) implements
        VariableChange {

    }

    public record VariableRenamed(String oldName, String newName) implements VariableChange {

    }

    public record VariableLevelsChanged(String name,
                                        List<LevelChange> levelChanges) implements VariableChange {

    }

    public record VariableUnitChanged(String name, String oldUnit, String newUnit) implements
        VariableChange {

    }

    static class DeleteVariableEvent extends ComponentEvent<VariableRow> {

      /**
       * Creates a new event using the given source and indicator whether the event originated from
       * the client side or the server side.
       *
       * @param source     the source component
       * @param fromClient <code>true</code> if the event originated from the client
       *                   side, <code>false</code> otherwise
       */
      public DeleteVariableEvent(VariableRow source, boolean fromClient) {
        super(source, fromClient);
      }
    }
  }

  @Tag("variable-levels-input")
  static class VariableLevelsInput extends Div implements UserInput, CanSnapshot,
      HasValidationProperties {

    private final List<LevelField> levelFields = new ArrayList<>();
    private final Div levelsContainer = new Div();
    private Snapshot initialState;
    LevelField focusedLevelField = null;

    public List<String> getLevels() {
      return filledLevels();
    }


    public interface LevelChange {

    }

    public record LevelAdded(int position, String value) implements LevelChange {

    }

    public record LevelDeleted(int position) implements LevelChange {

    }

    public record LevelMoved(int oldPosition, int newPosition) implements LevelChange {

    }

    @NonNull
    private LevelField addEmptyLevel() {
      LevelField levelField = new LevelField();
      addLevel(levelField);
      levelField.focus();
      return levelField;
    }

    @NonNull
    private LevelField addFilledLevel(@NonNull String value) {
      LevelField levelField = new LevelField();
      levelField.setValue(value);
      addLevel(levelField);
      levelField.focus();
      return levelField;
    }

    @NonNull
    private LevelField addFilledLevel(int index, @NonNull String value) {
      LevelField levelField = new LevelField();
      levelField.setValue(value);
      addLevel(index, levelField);
      levelField.focus();
      return levelField;
    }


    private void addLevel(LevelField levelField) {
      addLevel((int) levelsContainer.getChildren().count(), levelField);
    }

    /**
     * Tries to add the level at the specified index. If the index is smaller than or equal to 0,
     * the level is added as first level. If the index is greater than the current number of levels,
     * the level is added as the last level.
     *
     * @param index      the index where the level field should be added
     * @param levelField the field to add
     */
    private void addLevel(int index, LevelField levelField) {
      int componentIndex = Math.max(0, index);

      if (levelsContainer.getChildren().count() < index) {
        componentIndex = Math.toIntExact(levelsContainer.getChildren().count());
      }
      levelsContainer.addComponentAtIndex(componentIndex, levelField);
      levelFields.add(componentIndex, levelField);
      levelField.addDeleteListener(event -> removeLevelField(event.getSource()));
      // when the value changes, the validation is outdated and removed.
      levelField.addValueChangeListener(valueChangeEvent -> setInvalid(false));
      // when an empty field is added, the validation is outdated and removed.
      setInvalid(false);
      levelField.addFocusListener(event -> this.focusedLevelField = levelField);
      levelField.addBlurListener(event -> this.focusedLevelField = null);
    }

    public void removeLevelField(LevelField levelField) {
      levelFields.remove(levelField);
      levelField.removeFromParent();
      if (levelFields.isEmpty()) {
        addEmptyLevel();
      }
    }

    public VariableLevelsInput() {
      final String rootCssClasses = "border rounded-02 flex-vertical gap-none padding-04 padding-top-04";
      final String bodyClassNames = "flex-vertical justify-start gap-04 input-with-label";
      this.getStyle().set("grid-area", "b");
      this.addClassNames(rootCssClasses);

      var body = new Div();
      body.addClassNames(bodyClassNames);

      final String levelsContainerCss = "flex-horizontal gap-03 column-gap-03 width-full";
      levelsContainer.setId("levels-container"); //needed for labelling
      levelsContainer.addClassNames(levelsContainerCss);
      ButtonFactory buttonFactory = new ButtonFactory();

      String labelCss = "form-label input-label";
      var label = new NativeLabel("Levels");
      label.setFor(levelsContainer);
      label.addClassNames(labelCss);


      var addLevelButton = buttonFactory.createTertirayButton("Add Level",
          VaadinIcon.PLUS.create());
      addLevelButton.addClickListener(clickEvent -> addEmptyLevel());

      body.add(levelsContainer, addLevelButton);
      addLevelButton.addClassNames("width-max-content justify-self-start");
      this.add(label, body);

      addEmptyLevel();
      markInitialized();
      Shortcuts.addShortcutListener(this, it -> {
        var focusIndex = focusedLevelIndex();
        int nextFocus = focusIndex + 1;
        if (levelFields.size() > nextFocus) {
          levelFields.get(nextFocus).focus();
        } else {
          addEmptyLevel();
        }
      }, Key.ENTER).listenOn(this);

      addListener(PasteEvent.class, pasteEvent -> {
        List<String> pastedLines = pasteEvent.getClipboardText().lines()
            .filter(Objects::nonNull)
            .filter(line -> !line.isBlank())
            .toList();
        if (pastedLines.isEmpty()) {
          return;
        }
        var startIndex = focusedLevelIndex();
        if (Objects.nonNull(focusedLevelField) && focusedLevelField.isEmpty()) {
          //fill the first item in the focused field
          focusedLevelField.setValue(pastedLines.getFirst());
        } else {
          startIndex++;
          //fill the first item in a new field after the focused field
          addFilledLevel(startIndex, pastedLines.getFirst());
        }
        for (int lineIndex = 1; lineIndex < pastedLines.size(); lineIndex++) {
          var line = pastedLines.get(lineIndex);
          var insertIndex = startIndex + lineIndex;
          addFilledLevel(insertIndex, line);
        }
      });
    }

    private int focusedLevelIndex() {
      return Optional.ofNullable(focusedLevelField)
          .map(levelFields::indexOf)
          .orElse(-1); //null leads to same result as indexOf if not a child yet
    }


    @Override
    public void setInvalid(boolean invalid) {
      HasValidationProperties.super.setInvalid(invalid);
      getElement().setAttribute("invalid", invalid);
      levelFields.stream().findFirst().ifPresent(levelField -> {
        levelField.setErrorMessage(getErrorMessage());
        levelField.setInvalid(invalid);
      });
    }

    Function<List<LevelField>, List<String>> extractFilledLevels() {
      return fields -> fields.stream()
          .map(LevelField::getValue)
          .filter(Optional::isPresent)
          .map(Optional::get)
          .filter(s -> !s.isBlank())
          .toList();
    }

    private List<String> filledLevels() {
      return extractFilledLevels().apply(levelFields);
    }

    private boolean isEmpty() {
      return filledLevels().isEmpty();
    }

    private record LevelsSnapshot(List<Snapshot> levels) implements Snapshot {

      private LevelsSnapshot {
        levels = levels.stream().toList(); //make it unmodifiable
      }

    }

    @Override
    public InputValidation validate() {
      //valid if at least one level is present
      InputValidation validation =
          isEmpty() ? InputValidation.failed() : InputValidation.passed();
      setErrorMessage("Please provide at least one level.");
      setInvalid(!validation.hasPassed());
      return validation;
    }

    public List<LevelChange> getChanges() {
      if (initialState instanceof LevelsSnapshot(List<Snapshot> levels)) {

        var previousLevels = extractFilledLevels().apply(
            levels
                .stream()
                .map(this::mapToRestoredLevelField)
                .toList()
        );
        var currentLevels = extractFilledLevels().apply(levelFields);
        //find all the deletions
        List<LevelDeleted> levelDeletions = previousLevels.stream()
            .filter(previousLevel -> !currentLevels.contains(previousLevel))
            .map(removedLevel -> new LevelDeleted(previousLevels.indexOf(removedLevel)))
            .toList();
        // find all additions
        List<LevelAdded> levelAdditions = currentLevels.stream()
            .filter(currentLevel -> !previousLevels.contains(currentLevel))
            .map(addedLevel -> new LevelAdded(currentLevels.indexOf(addedLevel), addedLevel))
            .toList();
        //find all moved levels
        List<LevelMoved> levelMoves = previousLevels.stream()
            .filter(currentLevels::contains)
            .filter(previousLevel -> previousLevels.indexOf(previousLevel) != currentLevels.indexOf(
                previousLevel))
            .map(movedLevel -> new LevelMoved(previousLevels.indexOf(movedLevel),
                currentLevels.indexOf(movedLevel)))
            .toList();
        var changes = new ArrayList<LevelChange>();
        changes.addAll(levelDeletions);
        changes.addAll(levelAdditions);
        changes.addAll(levelMoves);
        return changes;
      }

      throw new IllegalStateException();
    }

    private LevelField mapToRestoredLevelField(Snapshot snapshot) {
      var dummy = new LevelField();
      try {
        dummy.restore(snapshot);
        return dummy;
      } catch (SnapshotRestorationException it) {
        throw new IllegalStateException("Initial state not correctly set", it);
      }
    }

    @Override
    public boolean hasChanges() {
      return levelFields.stream().anyMatch(LevelField::hasChanges);
    }


    public void markInitialized() {
      levelFields.forEach(LevelField::markInitialized);
      this.initialState = snapshot();
    }

    @Override
    public Snapshot snapshot() {
      return new LevelsSnapshot(levelFields.stream().map(LevelField::snapshot).toList());
    }

    @Override
    public void restore(Snapshot snapshot) throws SnapshotRestorationException {
      if (snapshot instanceof LevelsSnapshot(List<Snapshot> levels)) {
        var previousChildren = levelsContainer.getChildren().toList();
        var previousLevels = levelFields.stream().toList();
        levelsContainer.removeAll();
        levelFields.clear();
        try {
          levels.stream()
              .map(this::mapToRestoredLevelField)
              .forEach(this::addLevel);
          return;
        } catch (Exception e) {
          levelsContainer.removeAll();
          levelFields.clear();
          levelsContainer.add(previousChildren);
          levelFields.addAll(previousLevels);
          throw e;
        }
      }
      throw new SnapshotRestorationException(
          "Unknown snapshot type. Expected %s but got %s".formatted(LevelsSnapshot.class,
              snapshot.getClass()));

    }
  }

  static class LevelField extends Composite<Div> implements Focusable<Component>,
      CanSnapshot,
      UserInput,
      HasValidationProperties {


    static final String LEVEL_CLASS = "level";
    static final String LEVEL_FIELD_CSS = "flex-horizontal gap-03 width-full no-flex-wrap no-wrap input-with-label";
    static final String LEVEL_VALUE_CSS = "dynamic-growing-flex-item";

    private final TextField levelValue = new TextField();
    private final Button deleteLevelButton;
    private LevelSnapshot initialState;
    private final SerializablePredicate<String> levelValidator = String::isBlank;

    private LevelField() {
      var buttonFactory = new ButtonFactory();
      this.deleteLevelButton = buttonFactory.createGreyIconButton(VaadinIcon.TRASH.create());
      deleteLevelButton.addClickListener(
          clickEvent -> fireEvent(new DeleteLevelEvent(this, clickEvent.isFromClient())));
      initialState = (LevelSnapshot) this.snapshot();

      levelValue.setManualValidation(true);
      levelValue.addValueChangeListener(
          event -> setInvalid(levelValidator.negate().test(event.getValue())));
    }

    @Override
    public void setErrorMessage(String errorMessage) {
      levelValue.setErrorMessage(errorMessage);
    }

    @Override
    public String getErrorMessage() {
      return levelValue.getErrorMessage();
    }

    @Override
    public void setInvalid(boolean invalid) {
      levelValue.setInvalid(invalid);
    }

    @Override
    public boolean isInvalid() {
      return levelValue.isInvalid();
    }

    public Registration addValueChangeListener(
        ValueChangeListener<? super ComponentValueChangeEvent<TextField, String>> listener) {
      return levelValue.addValueChangeListener(listener);
    }


    @Override
    protected Div initContent() {
      var levelField = new Div();
      levelField.addClassNames(LEVEL_FIELD_CSS);
      levelValue.addClassNames(LEVEL_VALUE_CSS);
      levelField.add(levelValue, deleteLevelButton);
      levelField.addClassNames(LEVEL_CLASS);
      return levelField;
    }

    /**
     * After modification, this method should be used to inform the level field that its current
     * value is to be seen as initial value of the field.
     */
    public void markInitialized() {
      initialState = (LevelSnapshot) this.snapshot();
    }

    @Override
    public void focus() {
      levelValue.focus();
    }

    @Override
    public void setTabIndex(int tabIndex) {
      levelValue.setTabIndex(tabIndex);
    }

    @Override
    public int getTabIndex() {
      return levelValue.getTabIndex();
    }

    @Override
    public void blur() {
      levelValue.blur();
    }

    @Override
    public ShortcutRegistration addFocusShortcut(Key key, KeyModifier... keyModifiers) {
      return levelValue.addFocusShortcut(key, keyModifiers);
    }

    @Override
    public Registration addFocusListener(ComponentEventListener<FocusEvent<Component>> listener) {
      return levelValue.addFocusListener(
          it -> listener.onComponentEvent(new FocusEvent<>(it.getSource(), it.isFromClient())));
    }

    public boolean isEmpty() {
      return levelValue.isEmpty() || levelValue.getValue().isBlank();
    }

    public boolean isDeletion() {
      return isEmpty() && !initialState.levelValue().equals(levelValue.getEmptyValue());
    }

    public void setValue(String value) {
      levelValue.setValue(value);
    }

    public Optional<String> getValue() {
      return levelValue.getOptionalValue().map(String::trim);
    }

    Registration addDeleteListener(ComponentEventListener<DeleteLevelEvent> listener) {
      return addListener(DeleteLevelEvent.class, listener);
    }

    record LevelSnapshot(@NonNull String levelValue) implements
        Snapshot,
        Serializable {

      LevelSnapshot {
        Objects.requireNonNull(levelValue);
      }

    }

    @Override
    public Snapshot snapshot() {
      return new LevelSnapshot(levelValue.getValue());
    }

    @Override
    public void restore(Snapshot snapshot) throws SnapshotRestorationException {
      if (snapshot instanceof LevelSnapshot(String value)) {
        levelValue.setValue(value);
        return;
      }
      throw new SnapshotRestorationException(
          "Unknown snapshot type. Expected %s but got %s".formatted(LevelSnapshot.class,
              snapshot.getClass()));
    }

    @Override
    @NonNull
    public InputValidation validate() {
      if (!levelValue.isInvalid()) {
        return InputValidation.passed();
      }
      return InputValidation.failed();
    }

    @Override
    public boolean hasChanges() {
      return !initialState.levelValue().equals(levelValue.getValue());
    }

    static class DeleteLevelEvent extends ComponentEvent<LevelField> {

      public DeleteLevelEvent(LevelField source, boolean fromClient) {
        super(source, fromClient);
      }
    }
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
