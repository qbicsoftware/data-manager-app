package life.qbic.datamanager.views.general.grid.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.tabs.Tab;
import java.util.Objects;
import life.qbic.datamanager.views.general.Tag;
import life.qbic.datamanager.views.general.Tag.TagColor;
import life.qbic.datamanager.views.general.grid.FilterGrid;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public final class FilterGridTab extends Tab {

  private final FilterGrid<?> grid;

  private final Tag badge;

  public FilterGridTab(String label, FilterGrid<?> filterGrid) {
    super(new Span(label));
    this.badge = new Tag("");
    badge.setTagColor(TagColor.CONTRAST);
    this.grid = Objects.requireNonNull(filterGrid);
    add(badge);
  }

  public void setItemCount(int itemCount) {
    this.badge.setText(String.valueOf(itemCount));
  }

  public Component getComponent() {
    return grid;
  }

}
