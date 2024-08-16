package life.qbic.datamanager.views.notifications;

/**
 * This class tracks whether it was modified.
 */
public interface TracksModification {

  /**
   * Was this instance modified?
   *
   * @return true if modified; false otherwise
   */
  boolean wasModified();

  /**
   * Set the modification state.
   *
   * @param modified was this instance modified?
   */
  void setModified(boolean modified);
}
