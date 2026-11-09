package life.qbic.datamanager.views.general;

import life.qbic.projectmanagement.application.query.DataState;

/**
 * <b>The words and colours for {@link DataState}.</b>
 *
 * <p>The application layer owns the axis; the view owns the words. Keeping the labels here rather
 * than at each call site is what stops the pages drifting back into five dialects for one
 * condition: there is exactly one place that decides that {@link DataState#WAITING} reads "Waiting
 * for primary data" and not "Not complete yet", "Not attached" or "Pending".</p>
 *
 * <p><b>Why this is no longer package-private.</b> It used to live beside the project page's
 * sections, which was true only while that page was the sole surface rendering a state. The
 * experiment overview renders the same states on the same axis, and it lives in another package;
 * leaving the vocabulary behind the project page's package boundary is exactly how the two surfaces
 * grew different words for one condition ("Registered", "Not complete yet", "Not attached") while
 * both were reading the same {@link DataState}.</p>
 *
 * @see DataState
 * @see life.qbic.datamanager.views.general.snapshot.SnapshotPanel
 * @since 1.22.0
 */
public final class DataStateLabel {

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
  public static String phrase(DataState state) {
    return switch (state) {
      case COMPLETE -> "Data available";
      case PARTIAL -> "Partially available";
      case WAITING -> "Waiting for primary data";
      case AT_RISK -> "Needs attention";
    };
  }

  /** The short word, for a table column whose header already states the axis. */
  public static String word(DataState state) {
    return switch (state) {
      case COMPLETE -> "Available";
      case PARTIAL -> "Partial";
      case WAITING -> "Waiting";
      case AT_RISK -> "At risk";
    };
  }

  /** The modifier used to build the CSS class, e.g. {@code --partial}. */
  public static String modifier(DataState state) {
    return state.name().toLowerCase(java.util.Locale.ROOT).replace('_', '-');
  }

  /**
   * The state tag's tone, from the same axis.
   *
   * <p>Complete takes the success tone, partial the informational one, and waiting the neutral one:
   * a measurement whose data has not been uploaded yet is the normal state of the process, so it
   * must not wear a warning colour. Amber stays reserved for {@link DataState#AT_RISK}.</p>
   */
  public static String tone(DataState state) {
    return switch (state) {
      case COMPLETE -> "done";
      case PARTIAL -> "partial";
      case WAITING, AT_RISK -> "waiting";
    };
  }
}
