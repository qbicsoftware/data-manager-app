package life.qbic.datamanager.views;

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
public class EditComponentDisplay<S extends Component, T extends Component & HasSize & HasValue<? extends HasValue.ValueChangeEvent<U>, U> & Focusable<T>, U> extends
    HorizontalLayout {

  private final Function<U, S> viewProvider;
  private final T input;

  private final S emptyView;
  private S viewComponent;

  public EditComponentDisplay(T input, Function<U, S> viewProvider, S empty) {
    this.emptyView = empty;
    this.viewProvider = viewProvider;
    this.input = input;
    this.add(this.input);
    input.addValueChangeListener(it -> updateView());
    input.addBlurListener(it -> switchToView());
    this.addClickListener(it -> switchToEdit());
    updateView();
    input.blur();
    switchToView();
  }

  private void switchToView() {
    input.setVisible(false);
    viewComponent.setVisible(true);
  }

  private void switchToEdit() {
    viewComponent.setVisible(false);
    input.setVisible(true);
    input.focus();
  }

  private void updateView() {
    S updatedView;
    if (!input.isEmpty()) {
      updatedView = viewProvider.apply(input.getValue());
    } else {
      updatedView = emptyView;
    }
    this.add(updatedView);
    if (Objects.nonNull(this.viewComponent)) {
      boolean componentVisible = viewComponent.isVisible();
      viewComponent.setVisible(false);
      this.remove(viewComponent);
      updatedView.setVisible(componentVisible);
    }
    this.addComponentAsFirst(updatedView);
    this.viewComponent = updatedView;
  }
}
