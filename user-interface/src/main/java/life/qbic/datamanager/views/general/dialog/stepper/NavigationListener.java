package life.qbic.datamanager.views.general.dialog.stepper;

/**
 * <b>Navigation Listener</b>
 *
 * <p>Used in the context of {@link StepperDialog} navigation changes.</p>
 * <p>
 * The {@link StepperDialog} informs all subscribed {@link NavigationListener} on navigation
 * changes.
 *
 * @since 1.7.0
 */
public interface NavigationListener {

  /**
   * Informs the listener about a navigation change in the subscribed {@link StepperDialog}
   * instance.
   *
   * @param navigationInformation about the current new step and total steps available
   * @since 1.7.0
   */
  void onNavigationChange(NavigationInformation navigationInformation);

}
