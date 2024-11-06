package life.qbic.datamanager.views.general.section;

/**
 * <b>Control Elements Interface</b>
 *
 * <p>A component that contains control elements that can be enabled or disabled.</p>
 *
 * @since 1.6.0
 */
public interface Controllable {

  /**
   * Enable control elements of the implementing class. Current UI/UX requirements dictate to
   * disable and hide the elements, to improve the user experience.
   *
   * @since 1.6.0
   */
  void enableControls();

  /**
   * Enable control elements of the implementing class. Current UI/UX requirements dictate to
   * enable and show the elements, to improve the user experience.
   *
   * @since 1.6.0
   */
  void disableControls();

}
