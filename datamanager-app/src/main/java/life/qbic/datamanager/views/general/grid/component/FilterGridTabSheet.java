package life.qbic.datamanager.views.general.grid.component;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.shared.Registration;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.lang.NonNull;

/**
 * A specialised {@link TabSheet} that contains one or more {@link FilterGridTab}.
 * <p>
 * The tabs can be assigned to different types, which makes the sheet flexible and clients can
 * compose multiple filter grids if needed.
 *
 * @since 1.12.0
 */
public final class FilterGridTabSheet extends Composite<TabSheet> {

  private final TabSheet delegate;
  private final PrimaryActionButtonGroup primaryActionGroup;

  private final Map<FilterGridTab, Set<TabAction>> primaryActions = new ConcurrentHashMap<>();
  private final Map<FilterGridTab, Set<TabAction>> featureActions = new ConcurrentHashMap<>();

  public FilterGridTabSheet() {
    this.primaryActionGroup = new PrimaryActionButtonGroup(
        primaryActionButton("Primary Action"),
        featureActionButton("Main Feature"));
    delegate = getContent();

    delegate.setSuffixComponent(primaryActionGroup);
    delegate.addClassNames("filter-grid-tabsheet", "width-full", "height-full");

    primaryActionGroup.addClickListenerPrimaryAction(
        ignored -> primaryActions.forEach((key, value) -> value.forEach(it -> it.execute(key))));
    primaryActionGroup.addClickListenerFeature(
        ignored -> featureActions.forEach((key, value) -> value.forEach(it -> it.execute(key))));
  }


  public <T> void addTab(FilterGridTab<T> tab) {
    delegate.add(tab, tab.filterGrid());
  }

  public <T> void addTab(int index, FilterGridTab<T> tab) {
    try {
      delegate.getTabAt(index);
      delegate.add(tab, tab.filterGrid(), index);
    } catch (IllegalArgumentException e) {
      //index less than 0 or greater available tabs, -> set to negative value and add to end
      delegate.add(tab, tab.filterGrid(), -1);
    }
  }


  public void removeTab(FilterGridTab<?> tab) {
    var tabIndex = delegate.getIndexOf(tab);
    removeTab(tabIndex);
  }

  private boolean removeTab(int tabIndex) {
    if (tabIndex < 0) {
      return false;
    }
    Tab tab;
    try {
      tab = delegate.getTabAt(tabIndex);
    } catch (IllegalArgumentException tabNotFound) {
      return false;
    }
    if (!(tab instanceof FilterGridTab<?> filterGridTab)) {
      return false;
    }
    primaryActions.remove(filterGridTab);
    featureActions.remove(filterGridTab);
    delegate.remove(filterGridTab);
    return true;
  }

  public void removeAllTabs() {
    var maxLoopCount = 100;
    var currentLoopCount = 0;
    while (currentLoopCount < maxLoopCount) {
      boolean aTabWasRemoved = removeTab(0);
      if (!aTabWasRemoved) {
        break; // nothing was removed so we exit
      }
      currentLoopCount++;
    }
  }

  /**
   * Adds a primary action to be performed for the provided tab when the user clicks on the primary
   * action controls.
   *
   * @param tab    the tab for which this action should be performed
   * @param action the action to execute
   * @param <T>    the item type of the {@link FilterGridTab}
   * @return a registration with which the registered action can be removed
   */
  public <T> Registration addPrimaryAction(@NonNull FilterGridTab<T> tab,
      @NonNull TabAction<FilterGridTab<T>, T> action) {
    Objects.requireNonNull(tab);
    Objects.requireNonNull(action);
    primaryActions.merge(tab, new HashSet<>(Set.of(action)), (existing, provided) -> {
      existing.addAll(provided);
      return existing;
    });

    return () -> primaryActions.merge(tab, new HashSet<>(Set.of(action)), (existing, provided) -> {
      existing.removeAll(provided);
      if (existing.isEmpty()) {
        return null; // remove map entry
      }
      return existing;
    });
  }

  /**
   * Adds a primary action to be performed for the provided tab when the user clicks on the feature
   * controls.
   *
   * @param tab    the tab for which this action should be performed
   * @param action the action to execute
   * @param <T>    the item type of the {@link FilterGridTab}
   * @return a registration with which the registered action can be removed
   */
  public <T> Registration addFeatureAction(@NonNull FilterGridTab<T> tab,
      @NonNull TabAction<FilterGridTab<T>, T> action) {
    Objects.requireNonNull(tab);
    Objects.requireNonNull(action);
    featureActions.merge(tab, new HashSet<>(Set.of(action)), (existing, provided) -> {
      existing.addAll(provided);
      return existing;
    });

    return () -> featureActions.merge(tab, new HashSet<>(Set.of(action)), (existing, provided) -> {
      existing.removeAll(provided);
      if (existing.isEmpty()) {
        return null; // remove map entry
      }
      return existing;
    });
  }


  @FunctionalInterface
  public interface TabAction<T extends FilterGridTab<S>, S> {

    void execute(T item);
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

  public void hidePrimaryFeatureButton() {
    primaryActionGroup.featureButton.setVisible(false);
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
   * Shows the primary feature button
   */
  public void showPrimaryFeatureButton() {
    primaryActionGroup.featureButton.setVisible(true);
  }

  /**
   * Returns the currently selected {@link FilterGridTab}, if the selected tab is of the expected
   * type. Else the returned value is {@link Optional#empty()}.
   *
   * @param expectedType the expected item type of the {@link FilterGridTab}
   * @param <T>          the item type
   * @return the selected tab assigned to the expected type if it is assignable, else
   * {@link Optional#empty()}
   * @since 1.12.0
   */
  public <T> Optional<FilterGridTab<T>> getSelectedTab(Class<T> expectedType) {
    var optionalSelectedTab = Optional.ofNullable(delegate.getSelectedTab());
    if (optionalSelectedTab.isEmpty()) {
      return Optional.empty();
    }
    var selectedTab = optionalSelectedTab.orElseThrow();
    if (selectedTab instanceof FilterGridTab<?> tab
        && tab.filterGrid() instanceof FilterGrid<?, ?> filterGrid
        && filterGrid.itemType().isAssignableFrom(expectedType)) {
      //noinspection unchecked - checked with assignable check
      return Optional.of((FilterGridTab<T>) tab);
    } else {
      return Optional.empty();
    }
  }

  private static Button primaryActionButton(String caption) {
    var button = new Button(caption);
    button.addClassName("button-color-primary");
    return button;
  }

  private static Button featureActionButton(String caption) {
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
