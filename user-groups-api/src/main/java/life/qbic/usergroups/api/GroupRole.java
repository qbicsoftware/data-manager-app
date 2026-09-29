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
 * @since 1.19.0
 */
public enum GroupRole {
  /** May appoint/remove managers and dissolve the group. Ad-hoc groups only. */
  OWNER("Owner",
      "Full control: can appoint/remove managers, add/remove members, edit the group "
          + "and dissolve it. Ad-hoc groups only."),
  /** May add/remove regular members and rename/describe the group. */
  MANAGER("Manager",
      "Can add/remove regular members and edit the group name and description."),
  /** Regular member; may self-remove. */
  MEMBER("Member",
      "Regular member with read access to the group; can leave the group at any time.");

  private final String label;
  private final String description;

  GroupRole(String label, String description) {
    this.label = label;
    this.description = description;
  }

  /**
   * Human-readable, user-facing label of the role (e.g. {@code "Manager"}).
   */
  public String label() {
    return label;
  }

  /**
   * One-line, user-facing description of what the role may do inside a group.
   */
  public String description() {
    return description;
  }
}
