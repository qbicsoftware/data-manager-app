package life.qbic.identity.application.mfa;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import life.qbic.logging.api.Logger;
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
 * qbic.security.mfa.grace-cutoff = ISO-8601 instant, e.g. 2026-11-20T00:00:00Z (default: unset)
 * </pre>
 *
 * <p><b>What the cut-off is for.</b> The grace window is owed to the population that was using the
 * application when the required posture came into force. An account created afterwards is not owed
 * one: nothing changed under it, so it cannot be locked out by the change ({@code MfaGate}). Setting
 * the cut-off to the instant the posture was switched on splits the two populations with a
 * configuration value instead of a column, and it is auditable — the date is in the deployment
 * record, next to the setting that made the factor mandatory.</p>
 *
 * <p>Unset means "every account is eligible", which is the behaviour of a deployment that has not
 * been flipped yet and must not change when this property is introduced.</p>
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

  private static final Logger log = logger(MfaPolicy.class);

  private final MfaPolicyMode mode;
  private final Duration grace;
  /** The instant after which a newly created account is not owed a grace window; {@code null} = all. */
  private final Instant graceCutoff;

  public MfaPolicy(@Value("${qbic.security.mfa.mode:OFF}") String mode,
      @Value("${qbic.security.mfa.grace:PT48H}") Duration grace,
      @Value("${qbic.security.mfa.grace-cutoff:}") String graceCutoff) {
    this.mode = parseMode(mode);
    this.grace = requirePositive(grace);
    this.graceCutoff = parseCutoff(graceCutoff);
    // Logged at startup on purpose: "the factor is not requested" is almost always this value being
    // OFF, or a stale application.properties in target/classes after an edit. One line removes the
    // guesswork — and it is not a secret.
    log.info("Second-factor policy: mode=%s, grace=%s, grace-cutoff=%s"
        .formatted(this.mode, this.grace,
            this.graceCutoff == null ? "unset (every account eligible)" : this.graceCutoff));
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

  /**
   * The instant from which a newly created account is no longer owed a grace window.
   *
   * <p>Read it the other way round when deciding: an account registered <em>before</em> the cut-off is
   * eligible, one registered after it must enrol at its first login.</p>
   *
   * @return the cut-off, or empty when every account is eligible (the property is unset)
   */
  public Optional<Instant> graceCutoff() {
    return Optional.ofNullable(graceCutoff);
  }

  /**
   * Whether an account registered at the given instant is owed a grace window.
   *
   * <p>A missing registration date is treated as <b>eligible</b>. That is the conservative direction:
   * an unknown date must not turn into a mandatory-enrolment decision, because the only thing this
   * method can cause is a stricter path for a real account, and a data gap is not evidence that the
   * account is new.</p>
   *
   * @param registeredAt when the account was created, or {@code null} if unknown
   * @return {@code true} unless a cut-off is configured and the account was created after it
   */
  public boolean graceEligible(Instant registeredAt) {
    if (graceCutoff == null || registeredAt == null) {
      return true;
    }
    return registeredAt.isBefore(graceCutoff);
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

  /**
   * Parses the cut-off, failing at startup rather than at the first login.
   *
   * <p>An unreadable date must not be read as "unset": unset means every account is eligible, so a
   * typo would silently restore the behaviour this setting exists to change — and it would look
   * identical to a deployment that never configured it.</p>
   *
   * @param value the property value; blank means unset
   * @return the cut-off, or {@code null} when unset
   */
  static Instant parseCutoff(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Instant.parse(value.strip());
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException(
          "qbic.security.mfa.grace-cutoff is not an ISO-8601 instant: '" + value
              + "'. Use e.g. 2026-11-20T00:00:00Z, or leave it empty to grant every account a "
              + "grace window.", e);
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
