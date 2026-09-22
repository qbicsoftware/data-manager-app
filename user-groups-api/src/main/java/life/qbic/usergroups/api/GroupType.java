package life.qbic.usergroups.api;

/**
 * <b>Group type</b>
 *
 * <p>API-local copy of the user groups domain concept, kept self-contained on purpose so that
 * {@code user-groups-api} never depends on the {@code user-groups} module (mirrors how
 * {@code identity-api} keeps its DTOs self-contained).</p>
 *
 * <p>Distinguishes administrator-managed groups (org groups, e.g. NGS labs) from self-service
 * ad-hoc groups created by regular users.</p>
 *
 * @since 1.19.0
 */
public enum GroupType {
  /** Administrator-managed group (e.g. QBiC internal labs). */
  ORG,
  /** Self-service group created by a regular user. */
  ADHOC
}