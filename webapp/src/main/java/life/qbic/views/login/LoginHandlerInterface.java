package life.qbic.views.login;

import com.vaadin.flow.router.BeforeEvent;

/**
 * <b> Interface to handle the {@link LoginLayout} to the {@link LoginHandler}. </b>
 *
 * @since 1.0.0
 */
public interface LoginHandlerInterface {

  /**
   * Register the {@link LoginLayout} to the implementing class
   *
   * @param loginView The view that is being registered
   * @since 1.0.0
   */
  void handle(LoginLayout loginView);

  /**
   * Passes a before event to the handler, so that the handler can decide how to react and orchestrate
   * the layout properly.
   *
   * @param beforeEvent an event triggered before the router navigates to the view
   * @since 1.0.0
   */
  void handle(BeforeEvent beforeEvent);
}
