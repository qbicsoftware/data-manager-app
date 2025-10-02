package life.qbic.datamanager.views.projects.project.experiments.experiment.components.experimentalvariable;

import static java.util.Objects.nonNull;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.EventData;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.dom.DisabledUpdateMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import life.qbic.application.commons.CanSnapshot;
import life.qbic.application.commons.Snapshot;
import life.qbic.datamanager.views.general.dialog.AppDialog;
import life.qbic.datamanager.views.general.dialog.DialogBody;
import life.qbic.datamanager.views.general.dialog.DialogFooter;
import life.qbic.datamanager.views.general.dialog.DialogHeader;
import life.qbic.datamanager.views.general.dialog.InputValidation;
import life.qbic.datamanager.views.general.dialog.UserInput;
import life.qbic.datamanager.views.general.icon.IconFactory;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.experimentalvariable.VariableChange.VariableDeleted;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.experimentalvariable.VariableRow.InvalidChangesException;
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
    markInitialized();
  }

  public record ExperimentalVariableInformation(String name, String unit, List<String> levels) {

  }

  public void setUsedLevels(String variableName, Set<String> levels) {
    getVariableRows().stream()
        .filter(it -> it.getVariableName().equals(variableName))
        .findFirst()
        .ifPresent(row -> row.lockLevels(levels));
  }

  public void setAddVariablesEnabled(boolean enabled) {
    addVariableButton.setEnabled(enabled);
    addVariableButton.setVisible(enabled);
  }

  public List<VariableChange> getChanges() {
    return Stream.concat(
        deletedVariables.stream()
            .map(this::toRestoredVariableRow)
            .map(VariableRow::getVariableName)
            .map(VariableDeleted::new),
        this.getVariableRows().stream()
            .flatMap((VariableRow variableRow) -> {
              try {
                List<VariableChange> changes = variableRow.getChanges();
                return changes.stream();
              } catch (InvalidChangesException ex) {
                return Stream.of();
              }
            })).toList();
  }

  public void markInitialized() {
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
    body.addClassNames("dialog-section");
    variablesInput.addClassNames("flex-vertical gap-04");
    addVariableButton.addClassNames(
        "margin-bottom-04 flex-vertical width-max-content justify-self-start button-color-primary");
    variablesInput.add(addVariableButton);
    body.add(variablesInput);
    return body;
  }

  public void addVariable() {
    VariableRow variableRow = VariableRow.empty();
    addRow(variableRow);
  }

  public void addVariable(ExperimentalVariableInformation variableInformation) {
    VariableRow variableRow = VariableRow.filled(variableInformation);
    addRow(variableRow);
  }

  private void addRow(VariableRow variableRow) {
    variableRow.addDeleteVariableListener(it -> {
      if (it.getSource().isEmpty()) {
        removeVariable(it.getSource());
        return;
      }
      AppDialog confirmDialog = confirmVariableDeletionDialog(it.getSource().getVariableName());
      confirmDialog.registerConfirmAction(() -> {
        removeVariable(it.getSource());
        confirmDialog.close();
      });
      confirmDialog.registerCancelAction(confirmDialog::close);
      confirmDialog.open();
    });
    variablesInput.addComponentAtIndex(
        Math.max(0, (int) (variablesInput.getChildren().count()) - 1),
        variableRow);
    variableRow.focus();
  }

  private static AppDialog confirmVariableDeletionDialog(String variableName) {
    AppDialog confirmDialog = AppDialog.small();
    DialogHeader.withIcon(confirmDialog, "Delete experimental variable?",
        IconFactory.warningIcon());
    Span variableNameText = new Span(variableName);
    variableNameText.addClassNames("bold padding-horizontal-02");
    DialogBody.withoutUserInput(confirmDialog,
        new Div(new Span("The variable"), variableNameText,
            new Span("will be deleted.")));
    DialogFooter.withDangerousConfirm(confirmDialog, "Cancel",
        "Delete Variable \"%s\"".formatted(variableName));
    return confirmDialog;
  }

  private void removeVariable(@NonNull VariableRow variableRow) {
    if (!variableRow.isEmpty()) {
      var initialComponentState = toRestoredVariablesInput(this.initialState);
      var initialRowInformation = toRestoredVariableRow(variableRow.getInitialState());
      if (initialComponentState.getVariableRows().stream()
          .anyMatch(row -> row.getVariableName().equals(initialRowInformation.getVariableName()))) {
        //only record deletion of variable rows that were there initially
        var snapshotOfVariableRow = variableRow.snapshot();
        deletedVariables.add(snapshotOfVariableRow);
      }
    }
    variableRow.removeFromParent();
    if (getVariableRows().isEmpty()) {
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
    //the order of the calls matters as both methods display the error.
    var individualRowValidations = validateIndividualRows();
    var overallRulesValidation = validateOverallRules();
    return individualRowValidations.and(overallRulesValidation);

  }

  /**
   * Checks rules that cannot be checked on an individual row basis. Modifies the userinput to
   * communicate the validation result.
   *
   * @return the resulting validation
   */
  private InputValidation validateOverallRules() {
    return checkUniqueVariableNames();
  }

  private InputValidation checkUniqueVariableNames() {
    List<VariableRow> variableRows = getVariableRows();
    Map<String, Long> countsByName = variableRows.stream()
        .collect(Collectors.groupingBy(VariableRow::getVariableName, Collectors.counting()));

    List<VariableRow> rowsWithDuplicateName = variableRows.stream()
        .filter(it -> !it.isEmpty())
        .filter(it -> !it.getVariableName().isBlank())
        .filter(row -> countsByName.getOrDefault(row.getVariableName(), 0L) > 1)
        .toList();
    rowsWithDuplicateName.forEach(row -> {
      // be careful not to overwrite previously set invalid state as marking as valid is done on an individual level only.
      row.setNameInvalid("Please provide unique variable names.");
    });
    return !rowsWithDuplicateName.isEmpty() ? InputValidation.failed() : InputValidation.passed();
  }

  /**
   * Triggers the validation of all variable rows.
   *
   * @return the resulting validation
   */
  private InputValidation validateIndividualRows() {
    //we cannot use Stream#allMatch as this will not guarantee a call to VariableRow#validate on each row.
    return getVariableRows().stream()
        .map(VariableRow::validate)
        .reduce(InputValidation.passed(), InputValidation::and);
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
    var dummy = VariableRow.empty();
    try {
      dummy.restore(snapshot);
      return dummy;
    } catch (SnapshotRestorationException e) {
      throw new IllegalStateException("Initial state not correctly set.", e);
    }
  }

  @Override
  public void restore(Snapshot snapshot) throws SnapshotRestorationException {
    if (snapshot instanceof VariablesSnapshot(List<Snapshot> variableSnapshots, Snapshot state)) {
      if (nonNull(state)
          && state instanceof VariablesSnapshot) {
        this.initialState = state;
      }
      var previousChildren = getVariableRows();
      previousChildren.forEach(Component::removeFromParent);
      try {
        variableSnapshots
            .stream()
            .map(this::toRestoredVariableRow)
            .forEach(this::addRow);
        return;
      } catch (Exception e) {
        getVariableRows().forEach(Component::removeFromParent);
        previousChildren.forEach(this::addRow);
        throw e;
      }
    }
    throw new SnapshotRestorationException(
        "Unknown snapshot type. Expected %s but got %s".formatted(VariablesSnapshot.class,
            snapshot.getClass()));
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
