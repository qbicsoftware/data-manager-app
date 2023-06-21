package life.qbic.datamanager.views.projects.project.samples.registration.batch;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


/**
 * <b>Batch Information Layout </b>
 * <p>
 * Layout in which the user will provide the batch information necessary during sample registration
 * </p>
 */
public class BatchInformationLayout extends Div {

  public final TextField batchNameField = new TextField("Batch Name");
  public final RadioButtonGroup<MetadataType> dataTypeSelection = new RadioButtonGroup<>();
  public final Button cancelButton = new Button("Cancel");
  public final Button nextButton = new Button("Next");
  private final BatchInformationLayoutHandler batchInformationLayoutHandler;

  public BatchInformationLayout() {
    addClassName("batch-information");
    initContent();
    this.setSizeFull();
    batchInformationLayoutHandler = new BatchInformationLayoutHandler();
  }

  private void initContent() {
    Span batchInformationHeader = new Span("Batch Information");
    batchInformationHeader.addClassNames("header");
    add(batchInformationHeader);
    initBatchLayout();
    initDataTypeLayout();
    initButtonLayout();
  }

  private void initBatchLayout() {
    Span batchLayout = new Span();
    batchLayout.add(batchNameField);
    add(batchLayout);
  }

  private void initDataTypeLayout() {
    Div dataTypeLayout = new Div();
    dataTypeLayout.addClassName("data-type-layout");
    Span dataTypeHeader = new Span("Type of Data");
    dataTypeHeader.addClassName("header");
    dataTypeLayout.add(dataTypeHeader);
    Span dataTypeDescription = new Span(
        "There is a minimum amount of information required. All samples must conform the expected metadata values. The most suitable checklist for sample registration depends on the type of the sample.");
    dataTypeLayout.add(dataTypeDescription);
    initDataTypeSelection();
    dataTypeLayout.add(dataTypeSelection);
    add(dataTypeLayout);
  }

  private void initDataTypeSelection() {
    dataTypeSelection.addClassName("radio-group");
    dataTypeSelection.setItems(MetadataType.values());
    dataTypeSelection.setValue(dataTypeSelection.getListDataView().getItem(0));
    dataTypeSelection.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
    dataTypeSelection.setRenderer(new ComponentRenderer<>(metadataType -> {
      Span metadataTypeSpan = new Span(metadataType.label);
      Icon infoIcon = new Icon(VaadinIcon.INFO_CIRCLE);
      infoIcon.addClassName("info-icon");
      infoIcon.setTooltipText(metadataType.description);
      Span optionRow = new Span(metadataTypeSpan, infoIcon);
      optionRow.addClassName("option-row");
      return optionRow;
    }));
  }

  private void initButtonLayout() {
    Span batchInformationButtons = new Span();
    batchInformationButtons.addClassName("button-layout");
    nextButton.addClassName("confirm-button");
    batchInformationButtons.add(cancelButton, nextButton);
    // todo? this.setAlignSelf(Alignment.END, batchInformationButtons);
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
      binders.add(binderBatchName);
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
      dataTypeSelection.setValue(dataTypeSelection.getListDataView().getItem(0));
      batchNameField.clear();
    }

    private void resetChildValidation() {
      batchNameField.setInvalid(false);
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
