package life.qbic.datamanager.views.projects.project.experiments.experiment.components.experimentalvariable;

import static java.util.Objects.nonNull;

import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasValue.ValueChangeListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.shared.HasValidationProperties;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.shared.Registration;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import life.qbic.application.commons.CanSnapshot;
import life.qbic.application.commons.Snapshot;
import life.qbic.datamanager.views.general.ButtonFactory;
import life.qbic.datamanager.views.general.dialog.InputValidation;
import life.qbic.datamanager.views.general.dialog.UserInput;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.experimentalvariable.ExperimentalVariablesInput.ExperimentalVariableInformation;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.experimentalvariable.VariableChange.VariableAdded;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.experimentalvariable.VariableChange.VariableLevelsChanged;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.experimentalvariable.VariableChange.VariableRenamed;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.experimentalvariable.VariableChange.VariableUnitChanged;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.experimentalvariable.validators.NonEmptyStringValidator;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.experimentalvariable.validators.ValidatableTextField;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * A row for variable information manipulation.
 */
public class VariableRow extends Composite<Div> implements UserInput, CanSnapshot {

  private final ValidatableTextField name = new ValidatableTextField();
  private final TextField unit = new TextField();
  private final VariableLevelsInput variableLevels = new VariableLevelsInput();
  private final Button deleteVariable = new ButtonFactory()
      .createTertirayButton("Delete Variable", VaadinIcon.TRASH.create());
  private Snapshot initialState;

  static VariableRow empty() {
    var row = new VariableRow();
    row.variableLevels.addEmptyLevel();
    row.markInitialized();
    return row;
  }

  static VariableRow filled(ExperimentalVariableInformation variableInformation) {
    var row = new VariableRow();
    row.setValue(variableInformation);
    row.markInitialized();
    return row;
  }

  private VariableRow() {
    deleteVariable.addClickListener(
        e -> fireEvent(new DeleteVariableEvent(this, e.isFromClient())));
    markInitialized();
    name.addValidator(new NonEmptyStringValidator("Please provide a variable name."));
  }

  public void addNameValidator(Validator<String> nameValidator) {
    name.addValidator(nameValidator);
  }


  void setValue(ExperimentalVariableInformation variableInformation) {
    name.setValue(variableInformation.name());
    Optional.ofNullable(variableInformation.unit()).ifPresent(unit::setValue);
    variableInformation.levels().forEach(variableLevels::addFilledLevel);
  }

  void markInitialized() {
    variableLevels.markInitialized();
    this.initialState = snapshot();
  }

  Registration addDeleteVariableListener(
      ComponentEventListener<DeleteVariableEvent> listener) {
    return addListener(DeleteVariableEvent.class, listener);
  }


  @Override
  protected Div initContent() {
    var root = new Div();
    root.addClassNames(
        "border rounded-02 padding-04 gap-04 column-gap-05 grid-experimental-variable-input");
    var fields = new Div();
    fields.getStyle().set("grid-area", "nameunit");
    fields.addClassNames("flex-horizontal gap-05");
    name.addClassNames("flex-grow-1");
    name.setLabel("Variable Name");
    name.setRequired(true);
    unit.addClassNames("flex-grow-1");
    unit.setLabel("Unit (optional)");
    unit.setRequired(false);
    variableLevels.getStyle().set("grid-area", "levels");
    deleteVariable.getStyle().set("grid-area", "delete");
    deleteVariable.addClassNames("width-max-content");
    fields.add(name, unit);
    root.add(fields, variableLevels, deleteVariable);

    return root;
  }

  void setDeletable(boolean deletable) {
    deleteVariable.setEnabled(deletable);
    deleteVariable.setVisible(deletable);
  }

  /**
   * locked levels can be reordered but not deleted or renamed
   *
   * @param levels
   */
  void lockLevels(Set<String> levels) {
    variableLevels.lockLevels(levels);
    if (variableLevels.getLevels().stream().anyMatch(levels::contains)) {
      setDeletable(false);
      lock(unit);
    }
  }

  private void lock(TextField textField) {
    textField.setSuffixComponent(VaadinIcon.LOCK.create());
    textField.setReadOnly(true);
  }

  @Override
  @NonNull
  public InputValidation validate() {
    if (this.isEmpty()) {
      setValid();
      return InputValidation.passed();
    }
    name.refreshValidation();
    if (name.isInvalid()) {
      setInvalid();
      return InputValidation.failed();
    }

    InputValidation variableLevelsValidation = variableLevels.validate();
    return variableLevelsValidation;
  }

  private void setInvalid() {
    getElement().setProperty("invalid", true);
    getElement().setAttribute("invalid", true);
  }


  private void setValid() {
    getElement().setProperty("invalid", false);
    getElement().setAttribute("invalid", false);
    setChildrenValid();
  }

  private void setChildrenValid() {
    setChildValid(name);
    setChildValid(unit);
    setChildValid(variableLevels);
  }

  private void setChildValid(HasValidationProperties name) {
    name.setErrorMessage(null);
    name.setInvalid(false);
  }

  /**
   * Sets the variable name to be invalid and shows a given error message. If the error message is not shown yet, it is appended to existing error messages.
   *
   * @param errorMessage the error message to show.
   */
  void setNameInvalid(@NonNull String errorMessage) {
    this.setInvalid();
    if (errorMessageNotShownYet(errorMessage)) {
      String composedErrorMessage = Optional.ofNullable(name.getErrorMessage())
          .map(m -> m + "\n" + errorMessage)
          .orElse(errorMessage);
      name.setErrorMessage(composedErrorMessage);
    }
    name.setInvalid(true);
  }

  private boolean errorMessageNotShownYet(String errorMessage) {
    return Optional.ofNullable(name.getErrorMessage())
        .map(it -> !it.contains(errorMessage))
        .orElse(true);
  }

  /**
   * Sets the name to be valid
   */
  void setNameValid() {
    name.setInvalid(false);
    name.setErrorMessage(null);
  }

  Registration addNameChangeListener(
      ValueChangeListener<? super ComponentValueChangeEvent<TextField, String>> listener) {
    return name.addValueChangeListener(listener);
  }

  /**
   * Sets the unit to be invalid and shows a given error message
   * @param errorMessage
   */
  void setUnitInvalid(@NonNull String errorMessage) {
    this.setInvalid();
    unit.setErrorMessage(errorMessage);
    unit.setInvalid(true);
  }

  boolean isEmpty() {
    return name.isEmpty() && unit.isEmpty() && variableLevels.isEmpty();
  }

  String getVariableName() {
    return name.getValue().trim();
  }

  Optional<String> getUnit() {
    return unit.getOptionalValue();
  }

  public static class InvalidChangesException extends RuntimeException {

  }

  /**
   * Validates the component and returns all changes. Throws InvalidChangesException in case of
   * invalid input.
   *
   * @return an unmodifiable list of variable changes
   * @throws InvalidChangesException in case of invalid user input
   */
  List<VariableChange> getChanges() throws InvalidChangesException {
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
      String originalVariableName = previousRepresentation.getVariableName();
      if (originalVariableName.isBlank() && !this.getVariableName()
          .isBlank()) {
        return Collections.singletonList(
            new VariableAdded(getVariableName(), getUnit().orElse(null),
                variableLevels.getLevels()));
      }
      if (!originalVariableName.equals(this.getVariableName())) {
        changes.add(new VariableRenamed(originalVariableName,
            this.getVariableName()));
      }
      if (!previousRepresentation.getUnit().equals(this.getUnit())) {
        changes.add(new VariableUnitChanged(originalVariableName,
            previousRepresentation.getUnit().orElse(null),
            this.getUnit().orElse(null)));
      }
      if (this.variableLevels.hasChanges()) {
        changes.add(new VariableLevelsChanged(originalVariableName, variableLevels.getValue()));
      }
      return Collections.unmodifiableList(changes);
    }
    throw new IllegalStateException();
  }

  record ExperimentalVariableSnapshot(String name, String unit,
                                      Snapshot levels, @Nullable Snapshot initialState) implements
      Snapshot, Serializable {


  }

  @Override
  public boolean hasChanges() {
    if (initialState == null) {
      return !isEmpty();
    }
    if (initialState instanceof ExperimentalVariableSnapshot initialVariableState) {
      return !initialVariableState.name().equals(getVariableName())
          || !initialVariableState.unit().equals(unit.getValue())
          || variableLevels.hasChanges();
    }
    throw new IllegalStateException("Could not check for changes of " + this);
  }

  @Override
  public Snapshot snapshot() {
    return new ExperimentalVariableSnapshot(name.getValue(), unit.getValue(),
        variableLevels.snapshot(), initialState);
  }

  Snapshot getInitialState() {
    return initialState;
  }

  @Override
  public void restore(@NonNull Snapshot snapshot) throws SnapshotRestorationException {
    if (snapshot instanceof ExperimentalVariableSnapshot(
        String name1, String unit1, Snapshot levels, Snapshot state
    )) {
      if (nonNull(state)) {
        initialState = state;
      }
      variableLevels.restore(levels);
      name.setValue(name1);
      unit.setValue(unit1);
      return;
    }
    throw new SnapshotRestorationException("Cannot restore snapshot");
  }

  void focus() {
    name.focus();
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
