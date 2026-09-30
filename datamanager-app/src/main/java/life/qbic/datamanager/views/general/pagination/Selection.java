package life.qbic.datamanager.views.general.pagination;

import java.util.HashSet;
import java.util.Set;

/**
 * Identifier-based selection of list items across pages (USER-R-02, ADR-0009).
 *
 * <p>Owned by a list view, not by the Vaadin grid selection model: the selection is a plain
 * {@link Set} of identifier strings that survives page, filter, and sort changes. The grid row
 * selection is only reconciled against this set when a page is rendered; bulk actions (export,
 * edit, delete) read the full set, so they apply exactly to the cross-page selection.</p>
 *
 * <p>The model is deliberately free of UI and services, so it can be unit-tested and reused by
 * the measurement, sample, and raw-dataset lists (FEAT-PAG-LIST-02/03/04).</p>
 *
 * @since 1.19.0
 */
public final class Selection {

  private final Set<String> ids = new HashSet<>();
  private final Runnable changeListener;

  /**
   * Creates an empty selection.
   *
   * @param changeListener invoked after every mutation, may be {@code null}
   */
  public Selection(Runnable changeListener) {
    this.changeListener = changeListener;
  }

  /**
   * @return an unmodifiable snapshot of the selected item identifiers
   */
  public Set<String> selectedIds() {
    return Set.copyOf(ids);
  }

  /**
   * @return the number of selected items across all pages
   */
  public int count() {
    return ids.size();
  }

  public boolean contains(String id) {
    return ids.contains(id);
  }

  /**
   * Selects the given identifiers and notifies the change listener.
   */
  public void select(Set<String> ids) {
    if (this.ids.addAll(ids)) {
      notifyChange();
    }
  }

  /**
   * Selects a single identifier and notifies the change listener.
   */
  public void select(String id) {
    if (ids.add(id)) {
      notifyChange();
    }
  }

  /**
   * Deselects the given identifiers and notifies the change listener.
   */
  public void deselect(Set<String> ids) {
    if (this.ids.removeAll(ids)) {
      notifyChange();
    }
  }

  /**
   * Deselects a single identifier and notifies the change listener.
   */
  public void deselect(String id) {
    if (ids.remove(id)) {
      notifyChange();
    }
  }

  /**
   * Removes all selection and notifies the change listener (if the selection was not already
   * empty).
   */
  public void clear() {
    if (!ids.isEmpty()) {
      ids.clear();
      notifyChange();
    }
  }

  private void notifyChange() {
    if (changeListener != null) {
      changeListener.run();
    }
  }
}