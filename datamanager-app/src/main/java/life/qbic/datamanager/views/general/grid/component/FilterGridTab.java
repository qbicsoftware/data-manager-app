package life.qbic.datamanager.views.general.grid.component;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.tabs.Tab;
import java.util.Objects;
import life.qbic.datamanager.views.general.Tag;
import life.qbic.datamanager.views.general.Tag.TagColor;

/**
 * A specialized tab with tab label, count badge and a filter grid component.
 *
 * @since 1.12.0
 */
public final class FilterGridTab<T> extends Tab {

  private final FilterGrid<T, ?> grid;

  private final Tag badge;

  private int itemCount = 0;

  public FilterGridTab(String label, FilterGrid<T, ?> filterGrid) {
    super(new Span(label));
    this.badge = new Tag("");
    badge.setTagColor(TagColor.CONTRAST);
    this.grid = Objects.requireNonNull(filterGrid);
    grid.addItemCountListener(it -> setItemCount(it.getItemCount()));
    add(badge);
    addClassNames("flex-horizontal", "gap-02", "self-align-end");
  }

  /**
   * Sets the item count that is displayed next to the label as indicator about the total amount of
   * items available.
   *
   * @param itemCount the current item count
   * @since 1.12.0
   */
  public void setItemCount(int itemCount) {
    this.badge.setText(String.valueOf(itemCount));
    this.itemCount = itemCount;
  }

  /**
   * Retrieves the currently set item count value
   *
   * @return the item count value
   * @since 1.12.0
   */
  public int getItemCount() {
    return itemCount;
  }

  /**
   * Returns the contained filter grid.
   *
   * @return the filter grid that is assigned to the tab.
   * @since 1.12.0
   */
  public FilterGrid<T, ?> filterGrid() {
    return grid;
  }

  /**
   * Convenience API for the grid's type.
   *
   * @return the {@link Class} of the assigned grid's type
   * @since 1.12.0
   */
  public Class<T> modelType() {
    return grid.itemType();
  }

}
