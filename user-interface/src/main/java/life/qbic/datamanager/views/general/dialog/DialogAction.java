package life.qbic.datamanager.views.general.dialog;

/**
 * <b>Dialog Action Interface</b>
 *
 * <p>Some action that shall be executed after user interactions with dialogs.</p>
 *
 * @since 1.7.0
 */
@FunctionalInterface
public interface DialogAction {

  /**
   * Execute the action.
   *
   * @since 1.7.0
   */
  void execute();

}
