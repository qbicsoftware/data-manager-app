package life.qbic.datamanager.views.projects.project.info;

import life.qbic.projectmanagement.application.query.DataState;

/**
 * <b>The words and colours for {@link DataState}.</b>
 *
 * <p>The application layer owns the axis; the view owns the words. Keeping the labels here rather
 * than at each call site is what stops the page drifting back into five dialects for one condition:
 * there is exactly one place that decides that {@link DataState#WAITING} reads "Waiting for primary
 * data" and not "Not complete yet", "Not attached" or "Pending".</p>
 *
 * @see DataState
 * @since 1.22.0
 */
final class DataStateLabel {

  private DataStateLabel() {
    // static holder
  }

  /**
   * The full phrase, for a surface with no column header to scope a shorter one.
   *
   * <p>"Waiting" alone reads as a loading spinner when it sits beside a list of experiments, which
   * is why the phrase names what is waited for. A table with a {@code Data} column header can say
   * the short form; a card cannot.</p>
   */
  static String phrase(DataState state) {
    return switch (state) {
      case COMPLETE -> "Data available";
      case PARTIAL -> "Partially available";
      case WAITING -> "Waiting for primary data";
      case AT_RISK -> "Needs attention";
    };
  }

  /** The short word, for a table column whose header already states the axis. */
  static String word(DataState state) {
    return switch (state) {
      case COMPLETE -> "Available";
      case PARTIAL -> "Partial";
      case WAITING -> "Waiting";
      case AT_RISK -> "At risk";
    };
  }

  /** The modifier used to build the CSS class, e.g. {@code --partial}. */
  static String modifier(DataState state) {
    return state.name().toLowerCase(java.util.Locale.ROOT).replace('_', '-');
  }
}
