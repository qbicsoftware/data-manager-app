package life.qbic.datamanager.views.projects.project.measurements.pagination;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Identifier-based selection of measurements (ADR-0008, USER-R-02).
 *
 * <p>Owned by the measurement view, not by the Vaadin grid selection model: the selection is a
 * plain {@link Set} of measurement ID strings that survives page, filter, and sort changes. The
 * grid row selection is only reconciled against this set when a page is rendered; bulk actions
 * (export, edit, delete) read the full set, so they apply exactly to the cross-page selection.</p>
 *
 * <p>The model is deliberately free of UI and services, so it can be unit-tested and reused by
 * the raw-data and sample lists (FEAT-PAG-LIST-02/04).</p>
 *
 * @since 1.12.0
 */
public final class MeasurementSelection {

  private final Set<String> measurementIds = new HashSet<>();
  private final Runnable changeListener;

  /**
   * Creates an empty selection.
   *
   * @param changeListener invoked after every mutation, may be {@code null}
   */
  public MeasurementSelection(Runnable changeListener) {
    this.changeListener = changeListener;
  }

  /**
   * @return an unmodifiable snapshot of the selected measurement IDs
   */
  public Set<String> selectedIds() {
    return Set.copyOf(measurementIds);
  }

  /**
   * @return the number of selected measurements across all pages
   */
  public int count() {
    return measurementIds.size();
  }

  public boolean contains(String measurementId) {
    return measurementIds.contains(measurementId);
  }

  /**
   * Selects the given measurement IDs and notifies the change listener.
   */
  public void select(Set<String> ids) {
    if (measurementIds.addAll(ids)) {
      notifyChange();
    }
  }

  /**
   * Selects a single measurement ID and notifies the change listener.
   */
  public void select(String measurementId) {
    if (measurementIds.add(measurementId)) {
      notifyChange();
    }
  }

  /**
   * Deselects the given measurement IDs and notifies the change listener.
   */
  public void deselect(Set<String> ids) {
    if (measurementIds.removeAll(ids)) {
      notifyChange();
    }
  }

  /**
   * Deselects a single measurement ID and notifies the change listener.
   */
  public void deselect(String measurementId) {
    if (measurementIds.remove(measurementId)) {
      notifyChange();
    }
  }

  /**
   * Removes all selection and notifies the change listener (if the selection was not already
   * empty).
   */
  public void clear() {
    if (!measurementIds.isEmpty()) {
      measurementIds.clear();
      notifyChange();
    }
  }

  private void notifyChange() {
    if (changeListener != null) {
      Objects.requireNonNull(changeListener);
      changeListener.run();
    }
  }
}