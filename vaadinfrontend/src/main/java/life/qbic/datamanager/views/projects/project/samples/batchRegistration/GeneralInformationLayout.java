package life.qbic.datamanager.views.projects.project.samples.batchRegistration;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.theme.lumo.LumoUtility.IconSize;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin.Left;
import java.util.ArrayList;
import java.util.List;


/**
 * <b>General Information Layout </b>
 * <p>
 * Layout in which the user will provide the general information necessary during sample
 * registration
 * </p>
 */
class GeneralInformationLayout extends VerticalLayout {

  public final TextField batchNameField = new TextField("Batch Name");
  public final RadioButtonGroup<MetadataType> dataTypeSelection = new RadioButtonGroup<>();
  public final Button cancelButton = new Button("Cancel");
  public final Button nextButton = new Button("Next");
  private final GeneralInformationLayoutHandler generalInformationLayoutHandler;

  public GeneralInformationLayout() {
    initContent();
    this.setSizeFull();
    generalInformationLayoutHandler = new GeneralInformationLayoutHandler();
  }

  private void initContent() {
    Span generalInformationHeader = new Span("General Information");
    generalInformationHeader.addClassNames("text-xl", "font-bold", "text-secondary");
    add(generalInformationHeader);
    initBatchLayout();
    initDataTypeLayout();
    initButtonLayout();
  }

  private void initBatchLayout() {
    HorizontalLayout batchLayout = new HorizontalLayout();
    batchLayout.add(batchNameField);
    add(batchLayout);
  }

  private void initDataTypeLayout() {
    VerticalLayout dataTypeLayout = new VerticalLayout();
    dataTypeLayout.setMargin(false);
    dataTypeLayout.setPadding(false);
    Span dataTypeHeader = new Span("Type of Data");
    dataTypeHeader.addClassNames("text-l", "font-bold", "text-secondary");
    dataTypeLayout.add(dataTypeHeader);
    Span dataTypeDescription = new Span(
        "There is a minimum amount of information required. All samples must conform the expected metadata values. The most suitable checklist for sample registration depends on the type of the sample.");
    dataTypeLayout.add(dataTypeDescription);
    initDataTypeSelection();
    dataTypeLayout.add(dataTypeSelection);
    dataTypeSelection.setClassName(Left.MEDIUM);
    dataTypeLayout.setSizeFull();
    add(dataTypeLayout);
  }

  private void initDataTypeSelection() {
    dataTypeSelection.setItems(MetadataType.values());
    dataTypeSelection.setValue(dataTypeSelection.getListDataView().getItem(0));
    dataTypeSelection.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
    dataTypeSelection.setRenderer(new ComponentRenderer<>(MetadataType -> {
      Span metaDataType = new Span(MetadataType.metadataType);
      Icon infoIcon = new Icon(VaadinIcon.INFO_CIRCLE);
      infoIcon.addClassNames(IconSize.SMALL);
      infoIcon.setColor("#77828f");
      infoIcon.setTooltipText(MetadataType.metadataDescription);
      return new HorizontalLayout(metaDataType, infoIcon);
    }));
  }

  private void initButtonLayout() {
    HorizontalLayout generalInformationButtons = new HorizontalLayout();
    nextButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    generalInformationButtons.add(cancelButton, nextButton);
    this.setAlignSelf(Alignment.END, generalInformationButtons);
    add(generalInformationButtons);
  }

  public boolean isInputValid() {
    return generalInformationLayoutHandler.isInputValid();
  }

  public void reset() {
    generalInformationLayoutHandler.reset();
  }

  private class GeneralInformationLayoutHandler {

    private final List<Binder<?>> binders = new ArrayList<>();

    public GeneralInformationLayoutHandler() {
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
