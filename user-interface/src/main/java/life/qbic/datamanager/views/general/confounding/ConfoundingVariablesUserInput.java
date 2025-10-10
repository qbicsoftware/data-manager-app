package life.qbic.datamanager.views.general.confounding;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.shared.HasClientValidation;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.BinderValidationStatus;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.function.SerializableConsumer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.views.general.HasBoundField;
import life.qbic.datamanager.views.general.dialog.InputValidation;
import life.qbic.datamanager.views.general.dialog.UserInput;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.VariableReference;
import org.springframework.lang.NonNull;

public class ConfoundingVariablesUserInput extends Div implements UserInput {


  private static final String CSS_CLICKABLE = "clickable";
  private final List<BoundConfoundingVariableField> fields = new ArrayList<>();
  private Set<String> forbiddenNames;
  private final Div addActionContainer;

  public ConfoundingVariablesUserInput() {
    this.forbiddenNames = Collections.emptySet();
    var infoText = new Div(
        "Add confounding variables here. You'll be able to define their values while registering samples.");
    addActionContainer = createAddActionContainer();
    add(infoText, addActionContainer);
    addClassNames("flex-vertical", "width-full", "confounding-variables", "gap-04");
  }

  private Div createAddActionContainer() {
    final Div container;
    var addVariableIcon = new Icon(VaadinIcon.PLUS);
    Span addVariableText = new Span("Add Confounding Variable");
    addVariableText.addClassName(CSS_CLICKABLE);
    addVariableIcon.addClassName(CSS_CLICKABLE);
    container = new Div();
    container.setId("add-action-container");
    container.addClassNames("flex-horizontal", "width-full", "color-primary-text", "gap-02");
    container.add(addVariableIcon, addVariableText);
    addVariableIcon.addClickListener(e -> addVariable(new ConfoundingVariable(null, "")));
    addVariableText.addClickListener(e -> addVariable(new ConfoundingVariable(null, "")));
    return container;
  }

  public void setVariables(List<ConfoundingVariable> variables) {
    List<BoundConfoundingVariableField> listCopy = fields.stream().toList();
    listCopy.forEach(this::removeField);
    variables.forEach(this::addVariable);
  }

  public void setForbiddenNames(Set<String> forbiddenNames) {
    requireNonNull(forbiddenNames);
    this.forbiddenNames = forbiddenNames;
  }

  private boolean isUniquelyNamed(ConfoundingVariable confoundingVariable) {
    var counts = Stream.concat(forbiddenNames.stream(), fields.stream()
            .map(it -> it.variableField.getValue().name()))
        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    return counts.get(confoundingVariable.name()) <= 1;
  }

  private void addVariable(ConfoundingVariable variable) {
    BoundConfoundingVariableField field = new BoundConfoundingVariableField(this::isUniquelyNamed);
    field.setValue(variable);
    fields.add(field);
    addComponentAtIndex(getElement().indexOfChild(addActionContainer.getElement()),
        field.getField());
    field.setDeleteAction(it -> {
      remove(it);
      fields.remove(field);
    });
  }

  private void removeField(@NonNull final BoundConfoundingVariableField field) {
    remove(field.getField());
    fields.remove(field);
  }

  public List<ConfoundingVariable> values() {
    return fields.stream()
        .map(
            it -> {
              try {
                return it.getValue();
              } catch (ValidationException e) {
                throw new ApplicationException(e.getMessage(), e);
              }
            }
        )
        .toList();
  }

  @Override
  @NonNull
  public InputValidation validate() {
    return fields.stream()
        .map(BoundConfoundingVariableField::validate)
        .reduce(InputValidation.passed(),
            (v1, v2) ->
                v1.hasPassed() && v2.hasPassed() ? InputValidation.passed()
                    : InputValidation.failed());
  }

  @Override
  public boolean hasChanges() {
    return fields.stream()
        .anyMatch(BoundConfoundingVariableField::hasChanges);
  }

  private static class BoundConfoundingVariableField implements UserInput,
      HasBoundField<ConfoundingVariableField, ConfoundingVariable> {

    private ConfoundingVariable originalValue;
    private final Binder<ConfoundingVariableContainer> binder;
    private final ConfoundingVariableField variableField;

    private BoundConfoundingVariableField(
        Predicate<ConfoundingVariable> isUniquelyNamed) {
      variableField = new ConfoundingVariableField();
      binder = new Binder<>(ConfoundingVariableContainer.class);
      binder.setBean(new ConfoundingVariableContainer());
      binder.forField(variableField)
          .withValidator(vari -> nonNull(vari) && !vari.name().isBlank(),
              "Please provide a name for the variable")
          .withValidator(isUniquelyNamed::test,
              "Please provide unique variable names")
          .asRequired()
          .bind(ConfoundingVariableContainer::getVariable,
              ConfoundingVariableContainer::setVariable);
    }

    private static final class ConfoundingVariableContainer {

      private ConfoundingVariable variable;

      void setVariable(ConfoundingVariable variable) {
        this.variable = variable;
      }

      ConfoundingVariable getVariable() {
        return variable;
      }

      @Override
      public boolean equals(Object o) {
        if (!(o instanceof ConfoundingVariableContainer that)) {
          return false;
        }

        return Objects.equals(variable, that.variable);
      }

      @Override
      public int hashCode() {
        return Objects.hashCode(variable);
      }
    }

    @Override
    public ConfoundingVariableField getField() {
      return variableField;
    }

    @Override
    public ConfoundingVariable getValue() throws ValidationException {
      var container = new ConfoundingVariableContainer();
      binder.writeBean(container);
      return container.getVariable();
    }

    @Override
    public void setValue(ConfoundingVariable value) {
      var container = new ConfoundingVariableContainer();
      container.setVariable(value);
      binder.readBean(container);
      originalValue = value;
    }

    public void setDeleteAction(SerializableConsumer<ConfoundingVariableField> deleteAction) {
      variableField.setDeleteAction(deleteAction);
    }

    @Override
    public boolean isValid() {
      BinderValidationStatus<ConfoundingVariableContainer> validation = binder.validate();
      if (validation.hasErrors()) {
        String errorMessage = validation.getValidationErrors().stream()
            .map(ValidationResult::getErrorMessage)
            .findFirst().orElse("Invalid input");
        variableField.setErrorMessage(errorMessage);
      }
      variableField.setInvalid(validation.hasErrors());

      return validation.isOk();
    }

    @Override
    public boolean hasChanged() {
      return binder.hasChanges() || !Objects.equals(originalValue, variableField.getValue());
    }

    @Override
    @NonNull
    public InputValidation validate() {
      return isValid() ? InputValidation.passed() : InputValidation.failed();
    }

    @Override
    public boolean hasChanges() {
      return hasChanged();
    }
  }

  private static class ConfoundingVariableField extends CustomField<ConfoundingVariable> implements
      HasClientValidation {

    private VariableReference variableReference;
    private final TextField variableName;
    private SerializableConsumer<ConfoundingVariableField> deleteAction;

    private ConfoundingVariableField() {
      Div layout = new Div();
      variableName = new TextField();
      variableName.setLabel("Confounding Variable");
      variableName.addValueChangeListener(it -> updateValue());
      Icon deleteIcon = new Icon(VaadinIcon.CLOSE_SMALL);
      deleteIcon.addClassNames("color-primary-text", CSS_CLICKABLE);
      deleteIcon.addClickListener(clicked -> deleteAction.accept(this));
      layout.add(variableName, new Span(deleteIcon));
      this.deleteAction = ignored -> {
      };
      layout.addClassNames("flex-horizontal", "gap-04", "flex-align-items-baseline");
      add(layout);
    }

    @Override
    protected ConfoundingVariable generateModelValue() {
      return new ConfoundingVariable(variableReference, variableName.getValue());
    }

    @Override
    protected void setPresentationValue(ConfoundingVariable newPresentationValue) {
      this.variableReference = newPresentationValue.variableReference();
      variableName.setValue(newPresentationValue.name());
    }

    @Override
    public void setInvalid(boolean invalid) {
      variableName.setInvalid(invalid);
      super.setInvalid(invalid);
    }


    @Override
    public void setRequiredIndicatorVisible(boolean requiredIndicatorVisible) {
      super.setRequiredIndicatorVisible(requiredIndicatorVisible);
      variableName.setRequiredIndicatorVisible(requiredIndicatorVisible);
    }

    @Override
    public boolean isInvalid() {
      return variableName.isInvalid();
    }

    public void setDeleteAction(SerializableConsumer<ConfoundingVariableField> deleteAction) {
      this.deleteAction = deleteAction;
    }
  }

}
