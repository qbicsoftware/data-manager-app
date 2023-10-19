package life.qbic.identity.infrastructure;

/**
 * Indirection layer to persistence
 */
public interface SidDataStorage {

  /**
   * adds an entry for sid
   *
   * @param sid       the username or role
   * @param principal whether the sid is for a principal or a role
   */
  void addSid(String sid, boolean principal);

}
