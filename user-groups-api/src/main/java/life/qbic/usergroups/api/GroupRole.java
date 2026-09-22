package life.qbic.usergroups.api;

/**
 * <b>Group role</b>
 *
 * <p>API-local copy of the user groups domain concept, kept self-contained on purpose so that
 * {@code user-groups-api} never depends on the {@code user-groups} module.</p>
 *
 * <p>Role a user holds <em>inside</em> a group (what you may do to the group), unrelated to
 * project access roles (ACL OWNER/ADMIN/WRITE/READ) and unrelated to system roles
 * ({@code ROLE_*}).</p>
 *
 * <p>{@link #OWNER} applies to ad-hoc groups only (the creator). Org groups have <b>no</b> OWNER
 * membership row; the QBiC admin acts as owner-equivalent at the application layer.</p>
 *
 * @since 1.17.0
 */
public enum GroupRole {
  /** May appoint/remove managers and dissolve the group. Ad-hoc groups only. */
  OWNER,
  /** May add/remove regular members and rename/describe the group. */
  MANAGER,
  /** Regular member; may self-remove. */
  MEMBER
}