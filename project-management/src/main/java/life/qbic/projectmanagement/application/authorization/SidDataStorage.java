package life.qbic.projectmanagement.application.authorization;

/**
 * Indirection layer to persistence
 */
public interface SidDataStorage {

  /**
   * Adds an entry for sid
   *
   * @param sid       the user id or role
   * @param principal whether the sid is for a principal or a role
   */
  void addSid(String sid, boolean principal);

}
