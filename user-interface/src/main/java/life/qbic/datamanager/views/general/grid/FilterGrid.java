package life.qbic.datamanager.views.general.grid;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import java.util.Objects;
import java.util.Set;
import life.qbic.datamanager.views.general.MultiSelectLazyLoadingGrid;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class FilterGrid<T> extends Div {

  private final MultiSelectLazyLoadingGrid<T> grid;

  private Filter<T> currentFilter;


  public FilterGrid(MultiSelectLazyLoadingGrid<T> grid,
      CallbackDataProvider<T, Filter<T>> callbackDataProvider,
      Filter<T> initialFilter, FilterUpdater<T> filterUpdater) {
    this.grid = Objects.requireNonNull(grid);
    this.currentFilter = Objects.requireNonNull(initialFilter);

    var dataProvider = Objects.requireNonNull(callbackDataProvider).withConfigurableFilter();
    grid.setDataProvider(dataProvider);
    dataProvider.setFilter(currentFilter);

    var textfield = new TextField();
    textfield.setPlaceholder("Filter");
    textfield.addValueChangeListener(e ->
    {
      currentFilter = filterUpdater.withSearchTerm(currentFilter, e.getValue());
      dataProvider.setFilter(currentFilter);
      dataProvider.refreshAll();
    });
    textfield.setValueChangeMode(ValueChangeMode.EAGER);

    add(textfield);
    add(grid);
  }

  public Set<T> selectedElements() {
    return grid.getSelectedItems();
  }


}
