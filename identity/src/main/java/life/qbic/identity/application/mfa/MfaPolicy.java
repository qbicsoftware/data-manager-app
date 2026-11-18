package life.qbic.identity.application.mfa;

import static java.util.Objects.requireNonNull;

import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import life.qbic.identity.domain.model.mfa.MfaPolicyMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * <b>The deployment's second-factor posture (ADR 0011 P1)</b>
 *
 * <p>Configuration, not database state, on purpose: the posture appears in the config diff and the
 * release note, and "who is exempt" is never data. The only per-account deviation is the dated
 * {@code MfaGrace} window, which expires by itself.</p>
 *
 * <p><b>Default is {@link MfaPolicyMode#OFF}.</b> Deploying the login challenge must not change
 * behaviour for a running installation until someone decides it should — and that decision is the
 * rollout (M8), announced weeks ahead. Setting {@code optional} protects the users who have enrolled
 * without forcing anyone else; {@code required} is the end state and needs the grace window to be
 * safe.</p>
 *
 * <p>Properties:</p>
 * <pre>
 * qbic.security.mfa.mode   = OFF | OPTIONAL | REQUIRED   (default OFF, case-insensitive)
 * qbic.security.mfa.grace  = ISO-8601 duration, e.g. PT48H (default 48 h)
 * </pre>
 *
 * <p>Misconfiguration fails at startup rather than at the first login: a typo in the mode must not be
 * read as "off" while the operator believes enforcement is on, and a zero window would be a lockout
 * dressed as a grace period.</p>
 *
 * @since 1.0.0
 */
@Component
public class MfaPolicy {

  /** Enough for a user to notice, install an authenticator app and enrol. Configurable (ADR 0011). */
  public static final Duration DEFAULT_GRACE = Duration.ofHours(48);

  private final MfaPolicyMode mode;
  private final Duration grace;

  public MfaPolicy(@Value("${qbic.security.mfa.mode:OFF}") String mode,
      @Value("${qbic.security.mfa.grace:PT48H}") Duration grace) {
    this.mode = parseMode(mode);
    this.grace = requirePositive(grace);
  }

  /**
   * @return the current posture; {@link MfaPolicyMode#OFF} unless configured otherwise
   */
  public MfaPolicyMode mode() {
    return mode;
  }

  /**
   * @return the window granted to an account that becomes subject to {@code required} without a factor
   */
  public Duration grace() {
    return grace;
  }

  static MfaPolicyMode parseMode(String value) {
    requireNonNull(value, "mode must not be null");
    String normalized = value.strip().toUpperCase(Locale.ROOT);
    try {
      return MfaPolicyMode.valueOf(normalized);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "unknown second-factor policy mode '" + value + "'; allowed values are "
              + Arrays.toString(MfaPolicyMode.values()), e);
    }
  }

  private static Duration requirePositive(Duration grace) {
    requireNonNull(grace, "grace must not be null");
    if (grace.isZero() || grace.isNegative()) {
      throw new IllegalArgumentException(
          "the second-factor grace window must be positive, but was " + grace
              + " — a zero window denies every unenrolled account the moment the policy changes");
    }
    return grace;
  }
}
