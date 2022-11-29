package life.qbic.datamanager.views.general;

import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.Synchronize;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.dom.Element;
import java.util.Objects;
import java.util.function.Function;
import org.slf4j.Logger;

/**
 * @param <S> the component to display when in viewing mode
 * @param <T> the component to use for editing
 * @param <U> the value of the editing component
 * @since <version tag>
 */
@Tag("div")
public class ToggleDisplayEditComponent<
    S extends Component,
    T extends Component & HasSize & HasValue<? extends HasValue.ValueChangeEvent<U>, U> & HasValidation & Focusable<T>,
    U
    > extends AbstractField<ToggleDisplayEditComponent<S, T, U>, U> implements HasValidation,
    HasSize {


  private static final Logger log = getLogger(ToggleDisplayEditComponent.class);

  private final Function<U, S> displayProvider;
  private final T inputComponent;
  private S displayComponent;
  private final S emptyDisplayComponent;

  public ToggleDisplayEditComponent(Function<U, S> displayProvider,
      T inputComponent,
      S emptyDisplayComponent) {
    super(inputComponent.getEmptyValue());
    requireNonNull(displayProvider, "Display provider must not be null");
    requireNonNull(inputComponent, "Input component must not be null");
    requireNonNull(emptyDisplayComponent, "empty display component must not be null");
    this.displayProvider = displayProvider;
    this.inputComponent = inputComponent;
    this.emptyDisplayComponent = emptyDisplayComponent;
    this.displayComponent = emptyDisplayComponent;
    // attach input component as child to this element
    add(inputComponent);
    add(displayComponent);
    // initially update representation with input value
    setPresentationValue(inputComponent.getValue());
    setValue(inputComponent.getValue());

    inputComponent.addValueChangeListener(it -> this.setInvalid(inputComponent.isInvalid()));
    switchToDisplay();
    registerClientExitActions();
    registerClientEnterActions();


  }

  private void registerClientEnterActions() {
    // client enter is defined as a click on this component
    this.getElement().addEventListener("click", it -> {
      switchToEdit();
      inputComponent.focus();
    });
  }

  private void registerClientExitActions() {
    // client exit is defined as a blur event on the input element
    inputComponent.addBlurListener(it -> {
      updateValue(it.isFromClient());
      if (!isInvalid()) {
        switchToDisplay();
      }
    });
  }

  private void switchToDisplay() {
    inputComponent.setVisible(false);
    displayComponent.setVisible(true);
  }

  private void switchToEdit() {
    displayComponent.setVisible(false);
    inputComponent.setVisible(true);
  }

  public T getInputComponent() {
    return inputComponent;
  }

  /**
   * <p>
   * This property is set to true when the control value is invalid.
   * <p>
   * This property is synchronized automatically from client side when a 'invalid-changed' event
   * happens.
   * </p>
   *
   * @return the {@code invalid} property from the webcomponent
   */
  @Synchronize(property = "invalid", value = "invalid-changed")
  @Override
  public boolean isInvalid() {
    return getElement().getProperty("invalid", false);
  }

  /**
   * <p>
   * This property is set to true when the control value is invalid.
   * </p>
   *
   * @param invalid the boolean value to set
   */
  @Override
  public void setInvalid(boolean invalid) {
    getElement().setProperty("invalid", invalid);
  }

  @Override
  public void setErrorMessage(String errorMessage) {
    getElement().setProperty("errorMessage", errorMessage);
  }

  @Override
  public String getErrorMessage() {
    return getElement().getProperty("errorMessage");
  }

  private void updateValue(boolean fromClient) {
    setModelValue(generateModelValue(), fromClient);
    setPresentationValue(getValue());
  }

  @Override
  public void setValue(U value) {
    super.setValue(value);
    this.inputComponent.setValue(value);
  }

  /**
   * TODO
   *
   * @return
   */
  private U generateModelValue() {
    if (inputComponent.isInvalid()) {
//      this.setErrorMessage(inputComponent.getErrorMessage());
//      this.setInvalid(true);
      return this.getValue();
    }
    if (inputComponent.isEmpty()) {
      return this.getEmptyValue();
    }
    return inputComponent.getValue();
  }

  @Override
  protected void setPresentationValue(U u) {
    S updatedDisplayComponent = generateUpdatedDisplayComponent(u);
    replaceDisplayComponent(updatedDisplayComponent);
  }

  private S generateUpdatedDisplayComponent(U newValue) {
    S updatedDisplayComponent;
    //Empty value is dependent on input component ("") in TextField contrary to null for object selection)
    if (!Objects.equals(getEmptyValue(), newValue)) {
      updatedDisplayComponent = displayProvider.apply(newValue);
    } else {
      updatedDisplayComponent = emptyDisplayComponent;
    }
    return updatedDisplayComponent;
  }

  private void replaceDisplayComponent(S updatedDisplayComponent) {
    if (Objects.isNull(displayComponent)) {
      throw new RuntimeException("Display component is null. This is not expected.");
    }
    boolean componentVisible = displayComponent.isVisible();
    updatedDisplayComponent.setVisible(componentVisible);
    remove(displayComponent);
    add(updatedDisplayComponent);
    this.displayComponent = updatedDisplayComponent;
  }

  private void add(Component component) {
    Objects.requireNonNull(component, "Component to add cannot be null");
    getElement().appendChild(component.getElement());
  }

  private void remove(Component component) {
    Element parent = component.getElement().getParent();
    if (parent == null) {
      log.debug("Remove of a component with no parent does nothing.");
      return;
    }
    if (getElement().equals(parent)) {
      getElement().removeChild(component.getElement());
    } else {
      throw new IllegalArgumentException(
          "The given component (" + component + ") is not a child of this component");
    }
  }
}
