package life.qbic.datamanager.views.projects.project.samples.registration.batch;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;


/**
 * <b>Batch Information Layout </b>
 * <p>
 * Layout in which the user will provide the batch information necessary during sample registration
 * </p>
 */
public class BatchInformationLayout extends Div {

  public final TextField batchNameField = new TextField("Batch Name");
  public final Button cancelButton = new Button("Cancel");
  public final Button nextButton = new Button("Next");
  public final Select<Experiment> experimentSelect = new Select<>();
  private final BatchInformationLayoutHandler batchInformationLayoutHandler;

  public BatchInformationLayout() {
    initContent();
    this.addClassName("batch-content");
    batchInformationLayoutHandler = new BatchInformationLayoutHandler();
  }

  private void initContent() {
    initBatchLayout();
    initButtonLayout();
  }

  private void initBatchLayout() {
    Div batchLayout = new Div();
    Span batchInformationHeader = new Span("Batch Information");
    batchInformationHeader.addClassName("title");
    batchLayout.addClassName("batch-information");
    experimentSelect.setItemLabelGenerator(Experiment::getName);
    experimentSelect.setLabel("Experiment");
    batchLayout.add(batchInformationHeader);
    batchLayout.add(experimentSelect);
    batchLayout.add(batchNameField);
    batchNameField.setRequired(true);
    add(batchLayout);
  }

  private void initButtonLayout() {
    Span batchInformationButtons = new Span();
    batchInformationButtons.addClassName("buttons");
    nextButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    batchInformationButtons.add(cancelButton, nextButton);
    add(batchInformationButtons);
  }

  public boolean isInputValid() {
    return batchInformationLayoutHandler.isInputValid();
  }

  public void reset() {
    batchInformationLayoutHandler.reset();
  }

  private class BatchInformationLayoutHandler implements Serializable {

    @Serial
    private static final long serialVersionUID = 6406633989864983798L;
    private final List<Binder<?>> binders = new ArrayList<>();

    public BatchInformationLayoutHandler() {
      configureValidators();
    }

    private void configureValidators() {
      Binder<Container<String>> binderBatchName = new Binder<>();
      binderBatchName.forField(batchNameField)
          .withValidator(value -> !value.isBlank(), "Please provide a valid batch name")
          .bind(Container::value, Container::setValue);
      Binder<Container<Experiment>> binderExperimentSelect = new Binder<>();
      binderExperimentSelect.forField(experimentSelect)
          .asRequired("Please select an experiment")
          .bind(Container::value, Container::setValue);
      binders.addAll(List.of(binderBatchName, binderExperimentSelect));
    }

    private boolean isInputValid() {
      binders.forEach(Binder::validate);
      return binders.stream().allMatch(Binder::isValid);
    }

    private void reset() {
      resetChildValues();
      resetChildValidation();
    }

    private void resetChildValues() {
      experimentSelect.clear();
      batchNameField.clear();
    }

    private void resetChildValidation() {
      batchNameField.setInvalid(false);
      experimentSelect.setInvalid(false);
    }
  }

  static class Container<T> {

    private T value;

    T value() {
      return this.value;
    }

    void setValue(T newValue) {
      this.value = newValue;
    }

  }
}
