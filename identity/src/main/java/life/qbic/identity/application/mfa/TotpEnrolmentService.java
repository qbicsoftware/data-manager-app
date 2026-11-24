package life.qbic.identity.application.mfa;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import life.qbic.identity.domain.model.mfa.OtpauthUri;
import life.qbic.identity.domain.model.mfa.ProtectedSecret;
import life.qbic.identity.domain.model.mfa.SecondFactor;
import life.qbic.identity.domain.model.mfa.Totp;
import life.qbic.identity.domain.repository.SecondFactorRepository;
import life.qbic.logging.api.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <b>Self-service TOTP enrolment (AUTH-R-02)</b>
 *
 * <p>Two steps, and the split is deliberate: {@link #begin} issues a seed and returns it encrypted
 * plus a provisioning URI, and nothing is persisted; {@link #confirm} verifies a code derived from
 * that seed and only then creates the factor. A user who scans the QR code and never confirms leaves
 * no row behind, so "half-enrolled" is not a state the store or the login path has to understand.</p>
 *
 * <p><b>Where the plaintext seed lives.</b> Nowhere that outlives a method call. It is generated into
 * a {@code char[]}, handed to the protector (which encrypts it), embedded once in the provisioning URI,
 * and zeroed. The {@link PendingTotpEnrolment} that survives between the two steps carries the
 * <em>ciphertext</em>, so the session holding it is no more sensitive than the factor table will be —
 * except for the URI, which the UI must render once and must never log (see its record documentation).</p>
 *
 * <p><b>Audit.</b> Confirmation writes an audit line. The durable, queryable trail required by
 * AUTH-R-04 (enrolment, use, recovery, admin re-enrolment) is M5-5; this slice records the event so an
 * operator can already see enrolments in the log, and does not invent a second audit mechanism the
 * later task would have to replace.</p>
 *
 * @since 1.0.0
 */
@Service
public class TotpEnrolmentService {

  /**
   * How long a started enrolment may be confirmed. Short enough that an abandoned seed goes stale,
   * long enough for a user to install an authenticator app while the QR code is on screen.
   */
  static final Duration ENROLMENT_WINDOW = Duration.ofMinutes(15);

  private static final Logger log = logger(TotpEnrolmentService.class);

  private final SecondFactorRepository secondFactorRepository;
  private final SecondFactorSecretProtector secretProtector;

  public TotpEnrolmentService(SecondFactorRepository secondFactorRepository,
      SecondFactorSecretProtector secretProtector) {
    this.secondFactorRepository = requireNonNull(secondFactorRepository,
        "secondFactorRepository must not be null");
    this.secretProtector = requireNonNull(secretProtector, "secretProtector must not be null");
  }

  /**
   * Starts an enrolment: issues a seed, encrypts it, and returns the provisioning URI to render.
   *
   * @param userId       the account enrolling
   * @param displayName  the label the factor will carry, e.g. "iPhone (1Password)"
   * @param accountLabel what the authenticator app shows under the issuer — the user's email address
   * @param now          when the enrolment started
   * @return the pending enrolment; nothing is persisted yet
   */
  public PendingTotpEnrolment begin(String userId, String displayName, String accountLabel,
      Instant now) {
    requireNonNull(userId, "userId must not be null");
    requireNonNull(displayName, "displayName must not be null");
    requireNonNull(accountLabel, "accountLabel must not be null");
    requireNonNull(now, "now must not be null");

    String seed = Totp.newSeed();
    char[] seedChars = seed.toCharArray();
    try {
      String protectedSeed = secretProtector.protect(seedChars);
      String provisioningUri = OtpauthUri.forTotp(accountLabel, seed);
      return new PendingTotpEnrolment(userId, displayName, protectedSeed, provisioningUri, now);
    } finally {
      Arrays.fill(seedChars, '\0');
    }
  }

  /**
   * Confirms an enrolment with a code from the authenticator app and persists the factor.
   *
   * <p>Verification happens against the seed stored in the pending enrolment, decrypted on the spot.
   * A code that cannot be verified creates no factor at all — which is what keeps the second factor
   * meaningful: it is proven before it exists, not after.</p>
   *
   * @param pending the enrolment started by {@link #begin}
   * @param code    the six-digit code shown by the authenticator app
   * @param now     the instant of confirmation
   * @return the persisted, active factor
   * @throws InvalidTotpCodeException if the code does not match, for any reason
   * @throws EnrolmentExpiredException if the enrolment was started more than
   *     {@link #ENROLMENT_WINDOW} ago
   * @throws SecondFactorSecretUnavailableException if the protected seed cannot be decrypted
   */
  @Transactional
  public SecondFactor confirm(PendingTotpEnrolment pending, String code, Instant now) {
    requireNonNull(pending, "pending must not be null");
    requireNonNull(code, "code must not be null");
    requireNonNull(now, "now must not be null");

    if (now.isAfter(pending.createdAt().plus(ENROLMENT_WINDOW))) {
      throw new EnrolmentExpiredException(
          "the second-factor enrolment was started at " + pending.createdAt()
              + ", which is outside the " + ENROLMENT_WINDOW + " confirmation window");
    }

    char[] seed = secretProtector.reveal(pending.protectedSeed());
    try {
      if (!Totp.verify(new String(seed), code, now)) {
        // No code, seed or derived value in the message, and no distinction between "wrong code"
        // and "outside the tolerated clock skew": both must look the same (AUTH-NFR-03).
        log.warn("Second-factor enrolment for user %s was refused: the code did not verify"
            .formatted(pending.userId()));
        throw new InvalidTotpCodeException(
            "the presented code is not valid for this enrolment");
      }
    } finally {
      Arrays.fill(seed, '\0');
    }

    SecondFactor factor = SecondFactor.enrolTotp(pending.userId(),
        ProtectedSecret.ofBase64(pending.protectedSeed()), pending.displayName(), now);
    SecondFactor saved = secondFactorRepository.save(factor);
    log.info("Enrolled %s second factor %s for user %s"
        .formatted(saved.type(), saved.id(), saved.userId()));
    return saved;
  }
}
