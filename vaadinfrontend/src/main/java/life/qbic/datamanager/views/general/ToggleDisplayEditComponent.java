package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import java.util.Objects;
import java.util.function.Function;

/**
 * @param <S> the component to display when in viewing mode
 * @param <T> the component to use for editing
 * @param <U> the value of the editing component
 * @since <version tag>
 */
public class ToggleDisplayEditComponent<S extends Component, T extends Component & HasSize & HasValue<? extends HasValue.ValueChangeEvent<U>, U> & Focusable<T>, U> extends
    HorizontalLayout {

  private final Function<U, S> displayProvider;
  private final T editComponent;
  private S displayComponent;

  public ToggleDisplayEditComponent(T editComponent, Function<U, S> displayProvider,
      S emptyDisplayComponent) {
    this.displayComponent = emptyDisplayComponent;
    this.displayProvider = displayProvider;
    this.editComponent = editComponent;
    this.add(this.editComponent);
    editComponent.addValueChangeListener(it -> UpdateDisplayValue());
    editComponent.addBlurListener(it -> switchToDisplayComponent());
    this.addClickListener(it -> switchToEditComponent());
    UpdateDisplayValue();
    editComponent.blur();
    switchToDisplayComponent();
  }

  private void switchToDisplayComponent() {
    editComponent.setVisible(false);
    displayComponent.setVisible(true);
  }

  private void switchToEditComponent() {
    displayComponent.setVisible(false);
    editComponent.setVisible(true);
    editComponent.focus();
  }

  private void UpdateDisplayValue() {
    S updatedDisplayComponent;
    if (!editComponent.isEmpty()) {
      updatedDisplayComponent = displayProvider.apply(editComponent.getValue());
    } else {
      updatedDisplayComponent = displayComponent;
    }
    this.add(updatedDisplayComponent);
    if (Objects.nonNull(displayComponent)) {
      boolean componentVisible = displayComponent.isVisible();
      this.remove(displayComponent);
      updatedDisplayComponent.setVisible(componentVisible);
    }
    this.addComponentAsFirst(updatedDisplayComponent);
    this.displayComponent = updatedDisplayComponent;
  }
}
