package life.qbic.datamanager.views.strategy.dialog;

/**
 * <b>Dialog Closing Strategy</b>
 * <p>
 * Indicates support of a dialog closing strategy and should be used whenever a dialog that expects
 * user confirmation is used in the application.
 *
 * @since 1.6.0
 */
public interface DialogClosingStrategy {

  void execute();

}
