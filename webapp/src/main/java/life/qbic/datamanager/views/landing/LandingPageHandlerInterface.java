package life.qbic.datamanager.views.landing;

/**
 * <b> Interface to handle the {@link LandingPageLayout} to the {@link LandingPageHandler}. </b>
 *
 * @since 1.0.0
 */
public interface LandingPageHandlerInterface {

  /**
   * Registers the {@link LandingPageLayout} to the implementing class
   *
   * @param layout The view that is being registered
   * @since 1.0.0
   */
  void handle(LandingPageLayout layout);
}
