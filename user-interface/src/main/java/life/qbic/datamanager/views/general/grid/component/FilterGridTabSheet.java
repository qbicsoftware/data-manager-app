package life.qbic.datamanager.views.general.grid.component;

import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import java.util.Arrays;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public final class FilterGridTabSheet extends TabSheet {

  public FilterGridTabSheet(FilterGridTab... tabs) {
    super();
    Arrays.stream(tabs).forEach(tab -> {
      add(tab, tab.getComponent());
    });
  }

}
