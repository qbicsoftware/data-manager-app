package life.qbic.usergroups.domain.model;

/**
 * <b>Group type</b>
 * <p>
 * Distinguishes administrator-managed groups (org groups, e.g. NGS labs) from self-service
 * ad-hoc groups created by users.
 *
 * @since 1.19.0
 */
public enum GroupType {
  /** Administrator-managed group (e.g. QBiC internal labs). */
  ORG,
  /** Self-service group created by a regular user. */
  ADHOC
}