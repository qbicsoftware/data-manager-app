package life.qbic.identity.domain.model.policy;

import java.io.Serializable;

/**
 * Simple implementation of a policy check report.
 *
 * @param status the policy validation status
 * @param reason the reason, if failed (no content for passing validations required)
 * @since 1.0.0
 */
public record PolicyCheckReport(PolicyStatus status, String reason) implements Serializable {

}
