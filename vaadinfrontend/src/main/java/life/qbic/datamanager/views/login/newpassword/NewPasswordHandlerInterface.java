package life.qbic.datamanager.views.login.newpassword;

import com.vaadin.flow.router.BeforeEvent;

/**
 * <b>Handles the {@link NewPasswordLayout} components</b>
 *
 * <p>This class is responsible for enabling buttons or triggering other view relevant changes on
 * the view class components
 *
 * @since 1.0.0
 */
public interface NewPasswordHandlerInterface {

  /**
   * Registers a {@link NewPasswordLayout} to an implementing class
   *
   * @param registerLayout The view that is being handled
   * @since 1.0.0
   */
  void handle(NewPasswordLayout registerLayout);

  /**
   * Notifies the handler about {@link BeforeEvent }.
   *
   * @param beforeEvent a before event from the registered layout
   * @since 1.0.0
   */
  void handle(BeforeEvent beforeEvent);
}
