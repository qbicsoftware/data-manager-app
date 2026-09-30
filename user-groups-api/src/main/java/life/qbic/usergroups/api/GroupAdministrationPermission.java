package life.qbic.usergroups.api;

/**
 * <b>Group administration permission</b>
 *
 * <p>Authorization port of the user groups context for admin-governed operations (org groups).</p>
 *
 * <p>The {@code user-groups} module deliberately carries no Spring Security dependency: this port
 * declares <em>what</em> the context needs ("is this user a QBiC administrator?") while the
 * composition root provides <em>how</em> (the adapter resolves the user's authorities and checks
 * the system role {@code ROLE_ADMIN}). Enforcing the admin gate at the application boundary is
 * the only enforceable option for Vaadin {@code @Route} views — view-level annotations are not
 * authoritative for the Vaadin servlet.</p>
 *
 * <p>Org groups have no OWNER membership; the QBiC administrator acts as owner-equivalent at the
 * application layer, which is exactly what this port enables (user-groups strategy §3/§4.2).</p>
 *
 * @since 1.21.0
 */
public interface GroupAdministrationPermission {

  /**
   * Queries whether the given user is a QBiC administrator.
   *
   * @param userId the user id to check
   * @return {@code true} if the user holds the system role {@code ROLE_ADMIN}, {@code false}
   * otherwise (including unknown user ids)
   */
  boolean isAdmin(String userId);
}