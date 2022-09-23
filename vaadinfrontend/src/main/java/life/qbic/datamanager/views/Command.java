package life.qbic.datamanager.views;

/**
 * Command interface
 * <p>
 * Represents a simple interface to describe commands that can be invoked by one method
 * {@link Command#execute()}.
 * <p>
 * This interface is suitable for example in UI components, where you want to abstract the detailed
 * implementation of an action that should be performed on an user interaction with the component
 * (i.e. button click).
 *
 * @since 1.0.0
 */
@FunctionalInterface
public interface Command {

  /**
   * Executes the command.
   *
   * @since 1.0.0
   */
  void execute();

}
