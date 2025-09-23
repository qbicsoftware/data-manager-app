package life.qbic.datamanager.views.projects.project.experiments.experiment.components.experimentalvariable;

import static java.util.Objects.nonNull;

import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Composite;
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
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.shared.HasValidationProperties;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.shared.Registration;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;
import life.qbic.application.commons.CanSnapshot;
import life.qbic.application.commons.Snapshot;
import life.qbic.datamanager.views.general.ButtonFactory;
import life.qbic.datamanager.views.general.DragDropList;
import life.qbic.datamanager.views.general.dialog.InputValidation;
import life.qbic.datamanager.views.general.dialog.UserInput;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.experimentalvariable.ExperimentalVariablesInput.PasteEvent;
import org.springframework.lang.NonNull;

@Tag("variable-levels-input")
class VariableLevelsInput extends Div implements UserInput, CanSnapshot,
    HasValidationProperties {

  private final DragDropList<LevelField> levelsContainer = new DragDropList<>(LevelField.class);
  private Snapshot initialState;
  private LevelField focusedLevelField = null;

  List<String> getLevels() {
    return filledLevels();
  }

  void lockLevels(Set<String> levels) {
    levelsContainer.stream()
        .filter(field -> !field.isEmpty())
        .filter(field -> levels.contains(field.getValue().orElseThrow()))
        .forEach(LevelField::lock);
  }


  public interface LevelChange {

  }

  public record LevelAdded(int position, String value) implements LevelChange {

  }

  public record LevelDeleted(int position, String value) implements LevelChange {

  }

  public record LevelMoved(int oldPosition, int newPosition) implements LevelChange {

  }

  @NonNull
  LevelField addEmptyLevel() {
    LevelField levelField = new LevelField();
    addLevel(levelField);
    levelField.focus();
    return levelField;
  }

  @NonNull
  LevelField addFilledLevel(@NonNull String value) {
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
    addLevel(levelsContainer.size(), levelField);
    afterAdd(levelField);
  }

  /**
   * Tries to add the level at the specified index. If the index is smaller than or equal to 0, the
   * level is added as first level. If the index is greater than the current number of levels, the
   * level is added as the last level.
   *
   * @param index      the index where the level field should be added
   * @param levelField the field to add
   */
  private void addLevel(int index, LevelField levelField) {
    levelsContainer.add(index, levelField);
    afterAdd(levelField);
  }

  private void afterAdd(LevelField levelField) {
    levelField.addDeleteListener(event -> removeLevelField(event.getSource()));
    // when the value changes, the validation is outdated and removed.
    levelField.addValueChangeListener(valueChangeEvent -> setInvalid(false));
    // when an empty field is added, the validation is outdated and removed.
    setInvalid(false);
    levelField.addFocusListener(event -> this.focusedLevelField = levelField);
    levelField.addBlurListener(event -> this.focusedLevelField = null);
    levelField.focus();
  }

  private void removeLevelField(LevelField levelField) {
    levelsContainer.remove(levelField);
    if (levelsContainer.isEmpty()) {
      addEmptyLevel();
    }
  }


  VariableLevelsInput() {
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

    markInitialized();

    Shortcuts.addShortcutListener(this, it -> {
      var focusIndex = focusedLevelIndex();
      int nextFocus = focusIndex + 1;
      if (levelsContainer.size() > nextFocus) {
        levelsContainer.getAt(nextFocus).focus();
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
      if (nonNull(focusedLevelField) && focusedLevelField.isEmpty()) {
        //fill the first item in the focused field
        focusedLevelField.setValue(pastedLines.getFirst());
      } else if (nonNull(focusedLevelField)) {
        focusedLevelField.setValue(
            focusedLevelField.getValue().orElseThrow() + pastedLines.getFirst());
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
        .map(levelsContainer::indexOf)
        .orElse(-1); //null leads to same result as indexOf if not a child yet
  }


  @Override
  public void setInvalid(boolean invalid) {
    HasValidationProperties.super.setInvalid(invalid);
    getElement().setAttribute("invalid", invalid);
    if (!invalid) {
      levelsContainer.stream().forEach(f -> f.setErrorMessage(null));
      levelsContainer.stream().forEach(f -> f.setInvalid(false));
    }
  }

  private Function<List<LevelField>, List<String>> extractFilledLevels() {
    return fields -> fields.stream()
        .map(LevelField::getValue)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .filter(s -> !s.isBlank())
        .toList();
  }

  private List<String> filledLevels() {
    return extractFilledLevels().apply(levelsContainer.stream().toList());
  }

  boolean isEmpty() {
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
    if (!validation.hasPassed()) {
      levelsContainer.stream()
          .findFirst()
          .ifPresentOrElse(
              levelField -> {
                levelField.setErrorMessage("Please provide at least one level.");
                levelField.setInvalid(true);
              },
              () -> {
                LevelField levelField = addEmptyLevel();
                levelField.setErrorMessage("Please provide at least one level.");
                levelField.setInvalid(true);
              });
      setInvalid(true);
      return validation;
    }
    //duplicate levels are not allowed
    var levelCounts = getLevels().stream().collect(
        Collectors.groupingBy(Function.identity(), Collectors.counting()));
    var fieldsWithDuplicateLevels = levelsContainer.stream()
        .filter(it -> it.getValue().isPresent())
        .filter(field -> levelCounts.getOrDefault(field.getValue().orElseThrow(), 0L) > 1)
        .toList();
    for (LevelField fieldsWithDuplicateLevel : fieldsWithDuplicateLevels) {
      fieldsWithDuplicateLevel.setInvalid(true);
      fieldsWithDuplicateLevel.setErrorMessage("This level already exists.");
    }
    validation = fieldsWithDuplicateLevels.isEmpty() ? validation : InputValidation.failed();
    setInvalid(!validation.hasPassed());
    return validation;
  }

  List<LevelChange> getChanges() {
    if (initialState instanceof LevelsSnapshot(List<Snapshot> levels)) {

      var previousLevels = extractFilledLevels().apply(
          levels
              .stream()
              .map(this::mapToRestoredLevelField)
              .toList()
      );
      var currentLevels = extractFilledLevels().apply(levelsContainer.stream().toList());
      //find all the deletions
      List<LevelDeleted> levelDeletions = previousLevels.stream()
          .filter(previousLevel -> !currentLevels.contains(previousLevel))
          .map(removedLevel -> new LevelDeleted(previousLevels.indexOf(removedLevel), removedLevel))
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
    return levelsContainer.stream().anyMatch(LevelField::hasChanges)
        || !getChanges().isEmpty();
  }


  void markInitialized() {
    levelsContainer.stream().forEach(LevelField::markInitialized);
    this.initialState = snapshot();
  }

  @Override
  public Snapshot snapshot() {
    return new LevelsSnapshot(levelsContainer.stream().map(LevelField::snapshot).toList());
  }

  @Override
  public void restore(Snapshot snapshot) throws SnapshotRestorationException {
    if (snapshot instanceof LevelsSnapshot(List<Snapshot> levels)) {
      List<LevelField> previousLevels = levelsContainer.stream().toList();
      levelsContainer.clear();
      try {
        levels.stream()
            .map(this::mapToRestoredLevelField)
            .forEach(this::addLevel);
        return;
      } catch (Exception e) {
        levelsContainer.clear();
        for (LevelField previousLevel : previousLevels) {
          levelsContainer.add(previousLevel);
        }
        throw e;
      }
    }
    throw new SnapshotRestorationException(
        "Unknown snapshot type. Expected %s but got %s".formatted(LevelsSnapshot.class,
            snapshot.getClass()));

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
    private final SerializablePredicate<String> levelValidator = s -> !s.isBlank();

    private LevelField() {
      var buttonFactory = new ButtonFactory();
      this.deleteLevelButton = buttonFactory.createGreyIconButton(VaadinIcon.TRASH.create());
      deleteLevelButton.addClickListener(
          clickEvent -> fireEvent(new DeleteLevelEvent(this, clickEvent.isFromClient())));
      initialState = (LevelSnapshot) this.snapshot();
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

    public void lock() {
      deleteLevelButton.setEnabled(false);
      deleteLevelButton.setVisible(false);
      Icon lockIcon = VaadinIcon.LOCK.create();
      lockIcon.setTooltipText("This level is already used in a group. Editing is not possible.");
      levelValue.setSuffixComponent(lockIcon);
      levelValue.setReadOnly(true);
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
      if (levelValidator.test(levelValue.getValue())) {
        return InputValidation.passed();
      }
      return InputValidation.failed();
    }

    @Override
    public boolean hasChanges() {
      return !initialState.levelValue().equals(levelValue.getValue());
    }

    @Override
    public String toString() {
      return new StringJoiner(", ", LevelField.class.getSimpleName() + "[", "]")
          .add("levelValue=" + levelValue.getValue())
          .add("hashCode=" + hashCode())
          .toString();
    }

    static class DeleteLevelEvent extends ComponentEvent<LevelField> {

      public DeleteLevelEvent(LevelField source, boolean fromClient) {
        super(source, fromClient);
      }
    }
  }
}
