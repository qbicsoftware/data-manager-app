package life.qbic.usergroups.domain.model;

/**
 * <b>Group status</b>
 * <p>
 * Lifecycle state of a user group.
 *
 * <p>Groups are <em>soft-dissolved</em>: the {@code user_group} row is kept with status
 * {@link #DISSOLVED} and all memberships are purged. Rows are never deleted for ad-hoc
 * auto-dissolve.
 *
 * @since 1.0.0
 */
public enum GroupStatus {
  /** The group is live and can be shared, joined and managed. */
  ACTIVE,
  /** The group has been dissolved; it is hidden from directories and its memberships are gone. */
  DISSOLVED
}