package life.qbic.datamanager.views.general.section;

/**
 * <b>Control Elements Interface</b>
 *
 * <p>A component that contains control elements such as 'Edit', can expose
 * this behaviour to the client by implementing this interface.</p>
 *
 * @since 1.6.0
 */
public interface ControlElements {

  void enableControls();

  void disableControls();

  boolean controlsEnabled();

  boolean controlsDisabled();
}
