package life.qbic.identity.domain.model.policy;

/**
 * Policy status enums are used in {@link PolicyCheckReport}s, to indicate whether a policy check
 * has failed or passed.
 *
 * @since 1.0.0
 */
public enum PolicyStatus {
  PASSED,
  FAILED
}
