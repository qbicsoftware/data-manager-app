package life.qbic.datamanager.views.projects.project.samples.registration.batch;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import life.qbic.controlling.domain.model.experiment.Experiment;


/**
 * <b>Batch Information Layout </b>
 * <p>
 * Layout in which the user will provide the batch information necessary during sample registration
 * </p>
 */
public class BatchInformationLayout extends Div {

  public final TextField batchNameField = new TextField("Batch Name");
  public final RadioButtonGroup<MetadataType> dataTypeSelection = new RadioButtonGroup<>();
  public final Checkbox prefillSelection = new Checkbox("Prefill complete sample batch");
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
    initDataTypeLayout();
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
    add(batchLayout);
  }

  private void initDataTypeLayout() {
    Div dataTypeLayout = new Div();
    dataTypeLayout.addClassName("prefill-information");
    Span dataTypeHeader = new Span("Type of Data");
    dataTypeHeader.addClassName("title");
    dataTypeLayout.add(dataTypeHeader);
    Div dataTypeDescription = new Div();
    dataTypeDescription.add(
        "There is a minimum amount of information required. All samples must conform the expected metadata values. The most suitable checklist for sample registration depends on the type of the sample.");
    dataTypeLayout.add(dataTypeDescription);
    initDataTypeSelection();
    dataTypeLayout.add(dataTypeSelection);

    Div prefillDescription = new Div();
    prefillDescription.add(
        "If you want to register a complete batch (all possible permutations of conditions and replicates), some information can be prefilled.");
    dataTypeLayout.add(prefillDescription);
    dataTypeLayout.add(prefillSelection);
    add(dataTypeLayout);
  }

  private void initDataTypeSelection() {
    dataTypeSelection.setItems(MetadataType.values());
    dataTypeSelection.setReadOnly(true);
    dataTypeSelection.setValue(MetadataType.TRANSCRIPTOMICS_GENOMICS);
    dataTypeSelection.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
    dataTypeSelection.setRenderer(new ComponentRenderer<>(metadataType -> {
      Span metadataTypeSpan = new Span(metadataType.label);
      Icon infoIcon = new Icon(VaadinIcon.INFO_CIRCLE);
      infoIcon.addClassName("info-icon");
      infoIcon.setTooltipText(metadataType.description);
      return new HorizontalLayout(metadataTypeSpan, infoIcon);
    }));
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
      dataTypeSelection.setValue(MetadataType.TRANSCRIPTOMICS_GENOMICS);
      prefillSelection.setValue(false);
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
