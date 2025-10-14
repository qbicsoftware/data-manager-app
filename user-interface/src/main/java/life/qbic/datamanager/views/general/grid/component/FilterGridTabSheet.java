package life.qbic.datamanager.views.general.grid.component;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.shared.Registration;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import life.qbic.datamanager.views.general.grid.FilterGrid;
import org.springframework.lang.NonNull;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public final class FilterGridTabSheet extends TabSheet {

  private final PrimaryActionButtonGroup primaryActionGroup;

  public FilterGridTabSheet(FilterGridTab<?>... tabs) {
    super();
    Arrays.stream(tabs).forEach(tab -> {
      add(tab, tab.filterGrid());
    });
    this.primaryActionGroup = new PrimaryActionButtonGroup(
        mainActionButton("Primary Action"),
        mainFeatureButton("Main Feature"));

    setSuffixComponent(primaryActionGroup);
  }

  public Registration addPrimaryActionButtonListener(
      ComponentEventListener<ClickEvent<Button>> listener
  ) {
    return primaryActionGroup.addClickListenerPrimaryAction(listener);
  }

  public Registration addPrimaryFeatureButtonListener(
      ComponentEventListener<ClickEvent<Button>> listener
  ) {
    return primaryActionGroup.addClickListenerFeature(listener);
  }

  public void setCaptionPrimaryAction(String caption) {
    primaryActionGroup.actionButton.setText(caption);
  }

  public void setCaptionFeatureAction(String caption) {
    primaryActionGroup.featureButton.setText(caption);
  }

  public void hidePrimaryActionButton() {
    primaryActionGroup.actionButton.setVisible(false);
  }

  public void showPrimaryActionButton() {
    primaryActionGroup.actionButton.setVisible(true);
  }

  /** Untyped getter: returns the grid of the currently selected tab, if any. */
  private Optional<FilterGrid<?>> getSelectedFilterGrid() {
    var sel = getSelectedTab();
    return (sel instanceof FilterGridTab<?> ft)
        ? java.util.Optional.of(ft.filterGrid())
        : java.util.Optional.empty();
  }

  public <T> Optional<FilterGrid<T>> getSelectedFilterGrid(Class<T> expectedType) {
    return getSelectedFilterGrid()
        .filter(filterGrid -> expectedType.isAssignableFrom(filterGrid.type()))
        .map(g -> g.as(expectedType));
  }

  /** Convenience: run action if the selected grid matches the type. Returns true if executed. */
  public <T> boolean whenSelectedGrid(Class<T> type,
      Consumer<FilterGrid<T>> action) {
    return getSelectedFilterGrid(type)
        .map(filterGrid -> { action.accept(filterGrid); return true; })
        .orElse(false);
  }

  private static Button mainActionButton(String caption) {
    var button = new Button(caption);
    button.addClassName("button-color-primary");
    return button;
  }

  private static Button mainFeatureButton(String caption) {
    return new Button(caption);
  }

  private static class PrimaryActionButtonGroup extends Div {

    private final Button actionButton;
    private final Button featureButton;

    public PrimaryActionButtonGroup(@NonNull Button actionButton, @NonNull Button featureButton) {
      this.actionButton = Objects.requireNonNull(actionButton);
      this.featureButton = Objects.requireNonNull(featureButton);
      var visualSeparator = new Div();
      visualSeparator.addClassNames("border", "border-color-light");

      add(actionButton, visualSeparator, featureButton);
      addClassNames("flex-horizontal", "gap-04");
    }

    Registration addClickListenerPrimaryAction(
        ComponentEventListener<ClickEvent<Button>> listener) {
      return actionButton.addClickListener(listener);
    }

    Registration addClickListenerFeature(
        ComponentEventListener<ClickEvent<Button>> listener) {
      return featureButton.addClickListener(listener);
    }
  }
}
