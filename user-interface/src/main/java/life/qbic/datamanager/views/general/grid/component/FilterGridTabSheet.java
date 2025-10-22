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
import org.springframework.lang.NonNull;

/**
 * A specialised {@link TabSheet} that contains one or more {@link FilterGridTab}.
 * <p>
 * The tabs can be assigned to different types, which makes the sheet flexible and clients can
 * compose multiple filter grids if needed.
 *
 * @since 1.12.0
 */
public final class FilterGridTabSheet extends TabSheet {

  private final PrimaryActionButtonGroup primaryActionGroup;

  public FilterGridTabSheet(FilterGridTab<?>... tabs) {
    super();
    Arrays.stream(tabs).forEach(tab -> add(tab, tab.filterGrid()));
    this.primaryActionGroup = new PrimaryActionButtonGroup(
        mainActionButton("Primary Action"),
        mainFeatureButton("Main Feature"));

    setSuffixComponent(primaryActionGroup);
    setSizeFull();
    addClassName("filter-grid-tabsheet");
  }

  /**
   * Registers a
   * {@link ComponentEventListener that listens to {@link ClickEvent} of the primary action
   * button.}
   *
   * @param listener the listener to register to the click event
   * @return a {@link Registration} with the current subscription that the client can release once
   * it is not required anymore.
   * @since 1.12.0
   */
  public Registration addPrimaryActionButtonListener(
      ComponentEventListener<ClickEvent<Button>> listener
  ) {
    return primaryActionGroup.addClickListenerPrimaryAction(listener);
  }

  /**
   * Registers a
   * {@link ComponentEventListener that listens to {@link ClickEvent} of the primary feature
   * button.}
   *
   * @param listener the listener to register to the click event of the primary feature button
   * @return a {@link Registration} with the current subscription that the client can release once
   * it is not required anymore.
   * @since 1.12.0
   */
  public Registration addPrimaryFeatureButtonListener(
      ComponentEventListener<ClickEvent<Button>> listener
  ) {
    return primaryActionGroup.addClickListenerFeature(listener);
  }

  /**
   * Sets the caption of the primary action button.
   *
   * @param caption the label for the primary action button.
   * @since 1.12.0
   */
  public void setCaptionPrimaryAction(String caption) {
    primaryActionGroup.actionButton.setText(caption);
  }

  /**
   * Sets the caption of the primary feature button.
   *
   * @param caption the label for the primary feature button.
   * @since 1.12.0
   */
  public void setCaptionFeatureAction(String caption) {
    primaryActionGroup.featureButton.setText(caption);
  }

  /**
   * Hides the primary action button.
   *
   * @since 1.12.0
   */
  public void hidePrimaryActionButton() {
    primaryActionGroup.actionButton.setVisible(false);
  }

  /**
   * Shows the primary action button.
   *
   * @since 1.12.0
   */
  public void showPrimaryActionButton() {
    primaryActionGroup.actionButton.setVisible(true);
  }

  /**
   * Untyped getter: returns the grid of the currently selected tab, if any.
   */
  private Optional<FilterGrid<?>> getSelectedFilterGrid() {
    var selectedTab = getSelectedTab();
    return (selectedTab instanceof FilterGridTab<?> tab)
        ? java.util.Optional.of(tab.filterGrid())
        : java.util.Optional.empty();
  }

  /**
   * Returns the currently selected filter grid, if the selected filter grid is of the expected
   * type. Else the returned value is {@link Optional#empty()}.
   *
   * @param expectedType the expected class type of the filter grid
   * @param <T>          the type of the class
   * @return the selected grid assigned to the expected type if it is assignable, else will return
   * {@link Optional#empty()}
   * @since 1.12.0
   */
  public <T> Optional<FilterGrid<T>> getSelectedFilterGrid(Class<T> expectedType) {
    return getSelectedFilterGrid()
        .filter(filterGrid -> expectedType.isAssignableFrom(filterGrid.type()))
        .map(grid -> grid.as(expectedType));
  }

  /**
   * Can be used to register type-based consumers that accept the selected filter grid,  if the
   * passed type is assignable from the filter grid's class type
   *
   * @param type   the type that serves as condition for the consumer to get accepted
   * @param action the action that will accept the filter grid, if the passed type is assignable
   *               from the filter grid
   * @param <T>    the type of the grid's element
   * @return {@code true}, if the consumer got the filter grid, else returns {@code false}
   * @since 1.12.0
   */
  public <T> boolean whenSelectedGrid(Class<T> type,
      Consumer<FilterGrid<T>> action) {
    return getSelectedFilterGrid(type)
        .map(filterGrid -> {
          action.accept(filterGrid);
          return true;
        })
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
      visualSeparator.addClassNames("border", "border-color-light", "height-07");

      add(actionButton, visualSeparator, featureButton);
      addClassNames("flex-horizontal", "gap-04", "padding-03", "flex-align-items-center");
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
