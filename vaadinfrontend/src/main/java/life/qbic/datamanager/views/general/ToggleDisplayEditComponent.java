package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.customfield.CustomField;
import java.util.Objects;
import java.util.function.Function;

/**
 * @param <S> the component to display when in viewing mode
 * @param <T> the component to use for editing
 * @param <U> the value of the editing component
 * @since <version tag>
 */
public class ToggleDisplayEditComponent<S extends Component, T extends Component & HasSize & HasValue<? extends HasValue.ValueChangeEvent<U>, U> & HasValidation & Focusable<T>, U> extends
    CustomField<U> {

  private final Function<U, S> displayProvider;
  private final T editComponent;
  private S displayComponent;
  private final S emptyDisplayComponent;

  public ToggleDisplayEditComponent(T editComponent, Function<U, S> displayProvider,
      S emptyDisplayComponent) {
    this.displayComponent = emptyDisplayComponent;
    this.emptyDisplayComponent = emptyDisplayComponent;
    this.displayProvider = displayProvider;
    this.editComponent = editComponent;
    this.add(this.editComponent);
    addListeners();
    setPresentationValue(generateModelValue());
    editComponent.blur();
    switchToDisplayComponent();
    //Removes the space between elements in the contact element
    this.getStyle().set("--lumo-text-field-size", "0");
  }

  private void switchToDisplayComponent() {
    if (!editComponent.isInvalid()) {
      displayComponent.setVisible(true);
      editComponent.setVisible(false);
    }
  }

  private void switchToEditComponent() {
    displayComponent.setVisible(false);
    editComponent.setVisible(true);
    editComponent.focus();
  }

  private void addListeners() {
    editComponent.addValueChangeListener(it -> setPresentationValue(generateModelValue()));
    editComponent.addBlurListener(it -> switchToDisplayComponent());
    this.getElement().addEventListener("click", e -> switchToEditComponent());
  }

  @Override
  protected U generateModelValue() {
    return editComponent.getValue();
  }

  @Override
  protected void setPresentationValue(U u) {
    S updatedDisplayComponent;
    //Empty value is dependent on input component ("" in TextField contrary to null for object selection)
    if (!Objects.equals(editComponent.getEmptyValue(), u)) {
      updatedDisplayComponent = displayProvider.apply(u);
    } else {
      updatedDisplayComponent = emptyDisplayComponent;
    }
    this.add(updatedDisplayComponent);
    if (Objects.nonNull(displayComponent)) {
      boolean componentVisible = displayComponent.isVisible();
      this.remove(displayComponent);
      updatedDisplayComponent.setVisible(componentVisible);
    }
    add(updatedDisplayComponent);
    this.displayComponent = updatedDisplayComponent;
  }
}
