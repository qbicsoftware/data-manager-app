package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
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

  public ToggleDisplayEditComponent(T EditComponent, Function<U, S> displayProvider,
      S displayComponent) {
    this.displayProvider = displayProvider;
    this.editComponent = EditComponent;
    this.displayComponent = displayComponent;
    this.add(this.editComponent);
    this.addComponentAsFirst(this.displayComponent);
    EditComponent.addValueChangeListener(it -> UpdateDisplayValue());
    EditComponent.addBlurListener(it -> switchToDisplayComponent());
    this.addClickListener(it -> switchToEditComponent());
    UpdateDisplayValue();
    EditComponent.blur();
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
    if (!editComponent.isEmpty()) {
      displayComponent = displayProvider.apply(editComponent.getValue());
    }
  }
}
