package life.qbic.identity.application.throttle;

import static java.util.Objects.requireNonNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import life.qbic.identity.domain.repository.AuthAttemptCounter;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <b>Server-side throttling for the unauthenticated authentication flows.</b>
 *
 * <p>Implements the throttling clause of AUTH-NFR-03 for login, resend-confirmation and
 * resend-reset. Replaces the client-side 30-second {@code Timer} in the confirmation view, which a
 * page reload reset and which therefore was not a control at all.</p>
 *
 * <h3>Rule 1 — the counter is keyed on a hash, and there is no account lookup</h3>
 * <p>{@link #recordAttempt(Scope, String, Instant)} never asks whether the address exists. It cannot: the
 * key is derived from the string the visitor typed. That is deliberate, because a throttle that
 * behaves differently for a registered and an unregistered address becomes the account-enumeration
 * oracle AUTH-NFR-03 forbids — and the throttle is consulted on every request, including requests
 * whose account is unknown.</p>
 *
 * <h3>Rule 2 — a throttled outcome must be indistinguishable from a normal one</h3>
 * <p>{@link #recordAttempt} returns a {@link Decision}, and <b>every caller must ignore which one it got
 * when composing its response.</b> A caller that renders "too many attempts" tells an attacker that
 * their guess reached the threshold, and — because the threshold is per account — lets them confirm
 * that an address they are spamming is real. The decision exists to decide whether to <em>perform the
 * work</em>, not what to say.</p>
 *
 * <p>This is a sharp edge, so it is stated in the type: {@link Decision#isThrottled()} is for control
 * flow inside the server, never for the response.</p>
 *
 * <h3>Rule 3 — count per account <em>and</em> per origin, and combine them so the victim survives</h3>
 * <p>Per-account-only throttling hands an attacker a denial of service against a chosen victim: spam
 * their address and they cannot log in. AUTH-NFR-03 therefore asks for account <em>and</em> request
 * origin, and both buckets are written here.</p>
 *
 * <p>The two acceptance criteria pull in opposite directions, and "either bucket over its threshold
 * blocks" satisfies only the first of them:</p>
 * <ol>
 *   <li><i>many accounts hammered from one origin → that origin is throttled</i></li>
 *   <li><i>an attacker fills a victim's account bucket from their own origin → the victim logging in
 *       from a different origin is <b>not</b> blocked</i></li>
 * </ol>
 * <p>Criterion 2 cannot hold while "the account bucket is full" is by itself sufficient to refuse,
 * because the attacker's spam is exactly what filled it. The rule implemented here is therefore:</p>
 *
 * <pre>
 * refuse ⇔ origin bucket is blocked
 *        ∨ (account bucket is blocked ∧ this origin is itself implicated)
 *        ∨ (account bucket is blocked ∧ no trusted origin is available)
 * </pre>
 *
   * <p>"Implicated" means the requesting origin has accumulated the same number of failures that
 * would fill a whole account bucket, {@code origin.max-attempts + 1}, in the current window. The
 * threshold is <b>derived, not configured</b>, and that is a safety property rather than tidiness:
 * filling an account bucket requires exactly that many failures from one origin, so an attacker who
 * does the damage necessarily crosses it, while a legitimate user's own fumbling is capped at
 * {@code login.max-attempts} by the account bucket itself and therefore never does. A separately
 * tunable number would allow the pair to be set such that the attacker stays below it — silently
 * restoring the lockout this clause exists to remove.</p>
 *
 * <p><b>The cost, stated rather than hidden:</b> while an account is blocked, an origin that has never
 * been seen gets {@code login.max-attempts} attempts. That is the deliberate price of criterion 2. It
 * is bounded by the origin threshold as well, so the total an unseen origin can spend remains finite.</p>
 *
 * <h3>Rule 3a — the origin bucket counts <em>failures</em>, and a success never clears it</h3>
 * <p>The login bucket is written from {@code AuthAttemptEventListener} on a failed authentication
 * only, so an origin bucket fed from the same call counts failures — which is the attack signature
 * (credential stuffing produces failures, not successes). Counting every attempt would let one busy
 * institution behind a NAT block itself on legitimate traffic.</p>
 *
 * <p><b>Clearing the origin bucket on success is forbidden.</b> An attacker with any account of their
 * own could otherwise reset their origin's tally between guesses — the bucket would exist, be
 * maintained, and never reach its threshold. {@link #onSuccess} therefore clears the account bucket
 * only.</p>
 *
 * <h3>Rule 3b — origin scope is inert unless a trusted origin exists</h3>
 * <p>{@link ClientOrigin#unknown()} means "no address this deployment can attribute". It is not a
 * fallback to the connecting peer: behind a proxy that does not forward the client address, the peer
 * is the proxy, and a bucket keyed on it would be global — one attacker would lock out every user,
 * which is worse than having no origin scope. When the origin is unknown this class falls back to
 * account scope, i.e. to the pre-origin behaviour, and says so once in the log rather than silently
 * appearing to work.</p>
 *
 * <p>Whether a given deployment has a trustworthy origin is not decided here: it follows from
 * {@code server.forward-headers-strategy=native} plus a proxy that <em>replaces</em>
 * {@code X-Forwarded-For}. See {@code docs/operations/installation-and-administration.md} §1.5/§1.7
 * and {@link ClientOrigin}.</p>
 *
 * <h3>Rule 4 — thresholds are deployment configuration</h3>
 * <p>Nothing here is a constant. {@code qbic.auth-throttle.*} supplies the limits, because they are
 * tuned from telemetry and differ per environment. The origin threshold is deliberately separate from
 * the account threshold and must be <em>higher</em>: one origin may legitimately be a whole
 * institution behind a NAT.</p>
 *
 * <h3>Rule 5 — every write runs inside a transaction, and this class is what provides it</h3>
 * <p>The counter uses native SQL on a container-managed {@code EntityManager}; without an active
 * transaction an {@code INSERT}/{@code UPDATE} throws, and because the login counter is written from
 * a Spring authentication <em>event listener</em>, that exception escapes through the event publisher
 * and surfaces as an HTTP 500 — after the credentials have already been rejected.</p>
 *
 * <p>{@code @Transactional} is therefore on the public methods here, not on the repository. The
 * repository must stay transaction-agnostic: the token-consumption adapter deliberately joins the
 * caller's transaction so consumption and the state change it authorises commit together, and the
 * same rule applies here.</p>
 *
 * <p>This was a genuine production bug that the concurrency specs could not catch: every one of them
 * wrapped the counter in its own transaction, so they supplied the boundary that production never
 * opened.</p>
 *
 * @since 1.0.0
 */
@Service
public class AuthAttemptThrottle {

  private static final Logger log = LoggerFactory.logger(AuthAttemptThrottle.class);

  /**
   * The throttled flows. Each is an independent bucket, so exhausting the confirmation-resend limit
   * does not lock the visitor out of login.
   *
   * <p>An open set rather than a closed enum of the whole auth surface, so second-factor throttling
   * (MILESTONE-MFA M5-4) reuses this mechanism instead of adding a second table.</p>
   */
  public enum Scope {
    LOGIN,
    /**
     * Login attempts seen from one request origin.
     *
     * <p>A second bucket for the same flow, keyed on the grouped client network instead of the
     * submitted address. It exists as its own scope so it needs no schema change
     * ({@code auth_attempt.scope} is an open {@code varchar}) and so the two buckets cannot collide:
     * the key already includes the scope.</p>
     *
     * <p>Never returned to a caller and never rendered. It is written by {@link #recordAttempt} alongside
     * {@link #LOGIN} and consulted by the combined decision described in the class javadoc.</p>
     */
    LOGIN_ORIGIN,
    /**
     * Link-resend requests seen from one request origin, shared by {@link #EMAIL_CONFIRMATION_RESEND}
     * and {@link #PASSWORD_RESET_RESEND}.
     *
     * <p><b>Shared deliberately.</b> Both flows send exactly one mail on behalf of an arbitrary
     * address, so they have the same cost to us and the same abuse profile. Giving each its own bucket
     * would hand an attacker a doubled allowance for free by alternating between them.</p>
     *
     * <p>This bucket exists because account scope alone cannot bound this abuse at all: an attacker who
     * varies the address never reuses a bucket, so account-scope throttling is invisible to them while
     * the application sends mail on their behalf. See Rule 3c.</p>
     */
    MAIL_ORIGIN,
    /**
     * Registration submissions seen from one request origin.
     *
     * <p><b>Separate from {@link #MAIL_ORIGIN}, and with a much higher limit, because registration is
     * legitimately bursty.</b> Hundreds of students can create accounts from one campus network in a
     * short window. Sharing the resend bucket would either throttle a whole institution at the start of
     * a semester or force the resend limit high enough to be useless.</p>
     */
    REGISTRATION_ORIGIN,
    EMAIL_CONFIRMATION_RESEND,
    PASSWORD_RESET_RESEND,
    /**
     * Submissions of the registration form.
     *
     * <p>Added with FEAT-AUTH-FLOWS-03 because the unauthenticated registration form is a request
     * surface like the others: without a throttle, an attacker can mass-submit addresses to make the
     * application send notification mail, and can repeatedly invalidate a victim's outstanding reset
     * link, since each newly issued token supersedes the previous one.</p>
     */
    REGISTRATION,
    /**
     * Submissions of a second-factor code at the login challenge or on the mandatory-enrolment page.
     *
     * <p>The flow MILESTONE-MFA M5-4 names, and the reason {@link Scope} was made an open set: a
     * six-digit code is one guess in a million per thirty-second step, which holds only while an
     * attacker gets <em>one</em> guess per step. Without this bucket the same million guesses are a
     * few minutes of scripted submissions and the second factor is worth no more than the password
     * (MFA-68).</p>
     *
     * <p>Keyed on the account id rather than an address: the password step already established which
     * account is mid-login, and one account's failures must not spend another's allowance.</p>
     */
    SECOND_FACTOR
  }

  /** Whether the caller may perform the work. Never render this; see Rule 2. */
  public enum Decision {
    ALLOWED,
    THROTTLED
  }

  /**
   * The outcome of one attempt.
   *
   * <p>Carries the decision plus what a view needs to describe the state honestly:</p>
   * <ul>
   *   <li>{@link #attemptsUsed()} — how many attempts are counted in the current window;</li>
   *   <li>{@link #attemptsRemainingInWindow()} — how many more will succeed before the block.</li>
   * </ul>
   *
   * <p>Both describe <em>configured policy</em> and the caller's own attempt count, never whether an
   * account exists. A view may render them on a page the visitor has already reached legitimately
   * (e.g. after proving they know the user id), but must NOT render them where the audience is
   * anonymous — see {@code ResetPasswordMain} for that case.</p>
   *
   * <p>The counts always describe the <em>account</em> bucket, even when the decision was made by the
   * origin bucket. Publishing origin counts would tell one visitor how much traffic everyone behind
   * the same NAT has generated.</p>
   *
   * @param decision    whether the work may proceed
   * @param attemptsUsed attempts counted in the window after this one
   * @param maxAttempts  the configured limit for the flow
   */
  public record Outcome(Decision decision, int attemptsUsed, int maxAttempts) {

    /** @return how many further attempts will still be allowed in this window */
    public int attemptsRemainingInWindow() {
      return Math.max(0, maxAttempts - attemptsUsed);
    }

    public boolean isAllowed() {
      return decision == Decision.ALLOWED;
    }
  }

  private final AuthAttemptCounter counter;
  private final Policy loginPolicy;
  private final Policy resendPolicy;
  private final Policy secondFactorPolicy;

  /**

   * Convenience constructor with the default second-factor policy.
   *
   * <p>Exists so the many specs and the two flows that never touch a second factor do not have to
   * repeat three values. Spring uses the annotated constructor below.</p>
   */
  public AuthAttemptThrottle(AuthAttemptCounter counter, int loginMaxAttempts,
      Duration loginWindow, Duration loginBlockDuration, int resendMaxAttempts,
      Duration resendWindow, Duration resendBlockDuration) {
    this(counter, loginMaxAttempts, loginWindow, loginBlockDuration, resendMaxAttempts,
        resendWindow, resendBlockDuration, 5, Duration.ofMinutes(15), Duration.ofMinutes(15));
  }

  /**
   * Spring's constructor.
   *
   * <p>Annotated explicitly because the class now has two constructors; without it Spring cannot
   * choose. Same reason as {@code MfaLoginSuccessHandler}.</p>
   * Per-flow origin policies, or {@code null} when origin scope is switched off.
   *
   * <p>Null rather than a disabled {@link Policy} because "off" and "allowed zero attempts" must not
   * be representable as the same value: the latter would throttle every request.</p>
   *
   * <p>Three policies rather than one because the flows are not comparable. A failed login costs the
   * attacker a guess and costs us nothing; a registration or a resend costs us an email and the
   * recipient a notification. And registration is legitimately bursty where a resend is not, so it
   * needs an order of magnitude more headroom.</p>
   */
  private final OriginPolicy loginOriginPolicy;
  private final OriginPolicy mailOriginPolicy;
  private final OriginPolicy registrationOriginPolicy;

  /** Emits the "enabled but no trusted origin" warning once per process, not once per login. */
  private final AtomicBoolean unknownOriginReported = new AtomicBoolean(false);

  /**
   * Account-scope-only configuration: origin throttling is switched off.
   *
   * <p>This is a legitimate production configuration (a deployment behind a proxy that cannot supply
   * a trustworthy client address — see the class javadoc), not a test seam. Spring uses the wider
   * constructor below.</p>
   *
   * @param counter           persistence port
   * @param loginMaxAttempts  failures per account before the block
   * @param loginWindow       counting window for the login bucket
   * @param loginBlockDuration how long the login block lasts
   * @param resendMaxAttempts failures per account before the resend block
   * @param resendWindow      counting window for the resend buckets
   * @param resendBlockDuration how long a resend block lasts
   */
  /**
   * Account-scoped configuration with an explicit second-factor policy.
   *
   * <p>For specs that exercise the throttle's second-factor budget without caring about origin
   * scoping. Production uses the full constructor.</p>
   *
   * @param counter                 persistence port
   * @param loginMaxAttempts        failures per account before the login block
   * @param loginWindow             counting window for the login bucket
   * @param loginBlockDuration      how long the login block lasts
   * @param resendMaxAttempts       failures per account before the resend block
   * @param resendWindow            counting window for the resend buckets
   * @param resendBlockDuration     how long a resend block lasts
   * @param secondFactorMaxAttempts code submissions per account before the second-factor block
   * @param secondFactorWindow      counting window for the second-factor bucket
   * @param secondFactorBlockDuration how long the second-factor block lasts
   */
  public AuthAttemptThrottle(AuthAttemptCounter counter,
      int loginMaxAttempts, Duration loginWindow, Duration loginBlockDuration,
      int resendMaxAttempts, Duration resendWindow, Duration resendBlockDuration,
      int secondFactorMaxAttempts, Duration secondFactorWindow,
      Duration secondFactorBlockDuration) {
    this(counter,
        loginMaxAttempts, loginWindow, loginBlockDuration,
        resendMaxAttempts, resendWindow, resendBlockDuration,
        false,
        0, Duration.ofMinutes(15), Duration.ofMinutes(10),
        0, Duration.ofMinutes(15), Duration.ofMinutes(15),
        0, Duration.ofMinutes(15), Duration.ofMinutes(10),
        secondFactorMaxAttempts, secondFactorWindow, secondFactorBlockDuration);
  }

  /**
   * Login-focused configuration: origin scope on, with the mail and registration buckets sharing the
   * login limits.
   *
   * <p>For callers and specs that only exercise the login bucket. Production uses the full constructor
   * below, which configures each flow's limits separately; here they are simply equal, because a caller
   * that does not care about mail must not have to invent a mail threshold.</p>
   *
   * @param counter                  persistence port
   * @param loginMaxAttempts         failures per account before the account block
   * @param loginWindow              counting window for the account bucket
   * @param loginBlockDuration       how long the account block lasts
   * @param resendMaxAttempts        failures per account before the resend block
   * @param resendWindow             counting window for the resend buckets
   * @param resendBlockDuration      how long a resend block lasts
   * @param originEnabled            whether origin buckets are written and consulted at all
   * @param originMaxAttempts        failures per origin before an origin block
   * @param originWindow             counting window for the origin buckets
   * @param originBlockDuration      how long an origin block lasts
   */
  public AuthAttemptThrottle(AuthAttemptCounter counter,
      int loginMaxAttempts, Duration loginWindow, Duration loginBlockDuration,
      int resendMaxAttempts, Duration resendWindow, Duration resendBlockDuration,
      boolean originEnabled, int originMaxAttempts, Duration originWindow,
      Duration originBlockDuration) {
    this(counter, loginMaxAttempts, loginWindow, loginBlockDuration,
        resendMaxAttempts, resendWindow, resendBlockDuration, originEnabled,
        originMaxAttempts, originWindow, originBlockDuration,
        originMaxAttempts, originWindow, originBlockDuration,
        originMaxAttempts, originWindow, originBlockDuration);
  }

  /**
   * Origin-scoped configuration with each flow's origin limit given separately.
   *
   * <p>For callers and specs that exercise the mail or registration origin buckets but do not care
   * about the second-factor policy, which falls back to its defaults here.</p>
   *
   * @param counter                          persistence port
   * @param loginMaxAttempts                 failures per account before the account block
   * @param loginWindow                      counting window for the account bucket
   * @param loginBlockDuration               how long the account block lasts
   * @param resendMaxAttempts                failures per account before the resend block
   * @param resendWindow                     counting window for the resend buckets
   * @param resendBlockDuration              how long a resend block lasts
   * @param originEnabled                    whether origin buckets are written and consulted at all
   * @param loginOriginMaxAttempts           failed logins per origin before the origin block
   * @param loginOriginWindow                counting window for the login origin bucket
   * @param loginOriginBlockDuration         how long a login origin block lasts
   * @param mailOriginMaxAttempts            link resends per origin before the origin block
   * @param mailOriginWindow                 counting window for the mail origin bucket
   * @param mailOriginBlockDuration          how long a mail origin block lasts
   * @param registrationOriginMaxAttempts    registrations per origin before the origin block
   * @param registrationOriginWindow         counting window for the registration origin bucket
   * @param registrationOriginBlockDuration  how long a registration origin block lasts
   */
  public AuthAttemptThrottle(AuthAttemptCounter counter,
      int loginMaxAttempts, Duration loginWindow, Duration loginBlockDuration,
      int resendMaxAttempts, Duration resendWindow, Duration resendBlockDuration,
      boolean originEnabled,
      int loginOriginMaxAttempts, Duration loginOriginWindow,
      Duration loginOriginBlockDuration,
      int mailOriginMaxAttempts, Duration mailOriginWindow, Duration mailOriginBlockDuration,
      int registrationOriginMaxAttempts, Duration registrationOriginWindow,
      Duration registrationOriginBlockDuration) {
    this(counter, loginMaxAttempts, loginWindow, loginBlockDuration,
        resendMaxAttempts, resendWindow, resendBlockDuration, originEnabled,
        loginOriginMaxAttempts, loginOriginWindow, loginOriginBlockDuration,
        mailOriginMaxAttempts, mailOriginWindow, mailOriginBlockDuration,
        registrationOriginMaxAttempts, registrationOriginWindow,
        registrationOriginBlockDuration,
        5, Duration.ofMinutes(15), Duration.ofMinutes(15));
  }

  /**
   * Full configuration, including origin scope.
   *
   * @param counter                  persistence port
   * @param loginMaxAttempts         failures per account before the account block
   * @param loginWindow              counting window for the account bucket
   * @param loginBlockDuration       how long the account block lasts
   * @param resendMaxAttempts        failures per account before the resend block
   * @param resendWindow             counting window for the resend buckets
   * @param resendBlockDuration      how long a resend block lasts
   * @param originEnabled            whether origin buckets are written and consulted at all; this is
   *                                 the single switch for every flow, so origin scope cannot be on for
   *                                 login and silently off for the mail flows
   * @param loginOriginMaxAttempts   failed logins per origin before the login origin block; must
   *                                 exceed the account threshold, because one origin may be an
   *                                 institution
   * @param loginOriginWindow        counting window for the login origin bucket
   * @param loginOriginBlockDuration how long the login origin block lasts
   * @param mailOriginMaxAttempts    link resends per origin before the mail origin block
   * @param mailOriginWindow         counting window for the mail origin bucket
   * @param mailOriginBlockDuration  how long the mail origin block lasts
   * @param registrationOriginMaxAttempts   registrations per origin before the registration origin
   *                                        block; much higher than the resend limit, because a
   *                                        semester start is a legitimate burst
   * @param registrationOriginWindow        counting window for the registration origin bucket
   * @param registrationOriginBlockDuration how long the registration origin block lasts

   */
  @Autowired
  public AuthAttemptThrottle(AuthAttemptCounter counter,
      @Value("${qbic.auth-throttle.login.max-attempts:10}") int loginMaxAttempts,
      @Value("${qbic.auth-throttle.login.window:PT15M}") Duration loginWindow,
      @Value("${qbic.auth-throttle.login.block-duration:PT15M}") Duration loginBlockDuration,
      @Value("${qbic.auth-throttle.resend.max-attempts:3}") int resendMaxAttempts,
      @Value("${qbic.auth-throttle.resend.window:PT15M}") Duration resendWindow,
      @Value("${qbic.auth-throttle.resend.block-duration:PT15M}") Duration resendBlockDuration,

      // One switch, named after login because that is where it started. It governs every origin
      // bucket: a deployment that enabled it for login gets the mail buckets too, which is the safe
      // direction. Keeping it a single flag is deliberate — a per-flow flag would let origin scope be
      // on for login and silently off for the flows that actually send mail.
      @Value("${qbic.auth-throttle.login.origin.enabled:false}") boolean originEnabled,
      @Value("${qbic.auth-throttle.login.origin.max-attempts:60}") int loginOriginMaxAttempts,
      @Value("${qbic.auth-throttle.login.origin.window:PT15M}") Duration loginOriginWindow,
      @Value("${qbic.auth-throttle.login.origin.block-duration:PT10M}")
      Duration loginOriginBlockDuration,
      @Value("${qbic.auth-throttle.mail-origin.max-attempts:20}") int mailOriginMaxAttempts,
      @Value("${qbic.auth-throttle.mail-origin.window:PT15M}") Duration mailOriginWindow,
      @Value("${qbic.auth-throttle.mail-origin.block-duration:PT15M}")
      Duration mailOriginBlockDuration,
      @Value("${qbic.auth-throttle.registration-origin.max-attempts:100}")
      int registrationOriginMaxAttempts,
      @Value("${qbic.auth-throttle.registration-origin.window:PT15M}")
      Duration registrationOriginWindow,
      @Value("${qbic.auth-throttle.registration-origin.block-duration:PT10M}")
      Duration registrationOriginBlockDuration,
      // Second-factor code attempts (MFA-68). Last, so the origin parameters above keep the order the
      // deployment template mirrors. Account-scoped only: the origin buckets cover the flows that are
      // reachable anonymously, and this one is behind a password.
      @Value("${qbic.auth-throttle.second-factor.max-attempts:5}") int secondFactorMaxAttempts,
      @Value("${qbic.auth-throttle.second-factor.window:PT15M}") Duration secondFactorWindow,
      @Value("${qbic.auth-throttle.second-factor.block-duration:PT15M}")
      Duration secondFactorBlockDuration) {
    this.counter = requireNonNull(counter, "counter must not be null");
    this.loginPolicy = new Policy(loginMaxAttempts, loginWindow, loginBlockDuration, "login");
    this.resendPolicy = new Policy(resendMaxAttempts, resendWindow, resendBlockDuration, "resend");
    // Tighter than the password on purpose. A password guess is one of an unbounded space, so ten per
    // window is already generous; a TOTP guess is one of a million per thirty-second step and a
    // recovery code one of about 2^60 that never rotates, so a handful of submissions per window is
    // what keeps an online brute force uninteresting (MFA-68).
    this.secondFactorPolicy = new Policy(secondFactorMaxAttempts, secondFactorWindow,
        secondFactorBlockDuration, "second-factor");
    // +1, because the account bucket blocks on attempt maxAttempts + 1 (see Policy: the comparison is
    // a strict `>`). Filling an account bucket therefore costs an attacker exactly maxAttempts + 1
    // failures, and that is the number that must implicate their origin.
    this.loginOriginPolicy = originEnabled
        ? new OriginPolicy(loginOriginMaxAttempts, loginOriginWindow, loginOriginBlockDuration,
        loginMaxAttempts + 1)
        : null;
    this.mailOriginPolicy = originEnabled
        ? new OriginPolicy(mailOriginMaxAttempts, mailOriginWindow, mailOriginBlockDuration,
        resendMaxAttempts + 1)
        : null;
    // Registration shares the resend *account* limits (see policyFor), so its attribution threshold
    // derives from the same number.
    this.registrationOriginPolicy = originEnabled
        ? new OriginPolicy(registrationOriginMaxAttempts, registrationOriginWindow,
        registrationOriginBlockDuration, resendMaxAttempts + 1)
        : null;
  }

  /**
   * Records an attempt and reports the outcome.
   *
   * <p>Preferred over {@link #recordAttempt(Scope, String, Instant)}, which loses the counts callers need to
   * tell a visitor how many attempts remain.</p>
   *
   * @param scope      the flow
   * @param identifier the address as typed; normalised and hashed here, never stored raw
   * @param now        the attempt instant
   * @return the outcome; see Rule 2 about what may be rendered
   */
  @Transactional
  public Outcome recordAndReport(Scope scope, String identifier, Instant now) {
    return doRecordAndReport(scope, identifier, ClientOrigin.unknown(), now);
  }

  /**
   * Records an attempt from a known request origin and reports the outcome.
   *
   * <p>For the login flow this writes <em>both</em> the account bucket and — when the origin is known
   * and origin scope is enabled — the origin bucket, and decides with the combined rule from the class
   * javadoc. For every other flow it behaves exactly like
   * {@link #recordAndReport(Scope, String, Instant)}: those flows have no origin bucket, because their
   * thresholds would have to be calibrated against the population behind one NAT before they can be
   * switched on, and inventing that number here would either throttle institutions or do nothing.</p>
   *
   * @param scope      the flow
   * @param identifier the address as typed; never stored raw
   * @param origin     the request origin, or {@link ClientOrigin#unknown()}
   * @param now        the attempt instant
   * @return the outcome, describing the account bucket
   */
  @Transactional
  public Outcome recordAndReport(Scope scope, String identifier, ClientOrigin origin, Instant now) {
    return doRecordAndReport(scope, identifier, origin, now);
  }

  /**
   * The write path, without a transaction annotation.
   *
   * <p><b>Why the public methods do not call each other.</b> A self-invocation bypasses the Spring
   * proxy, so the callee's {@code @Transactional} would be ignored and only the caller's would apply.
   * Here the two happen to be compatible today, which is precisely the trap: a later change to either
   * annotation would silently take effect in one entry point and not the other. Every public method
   * therefore opens its own transaction and delegates here, so there is exactly one place where the
   * transaction boundary is decided (Rule 5).</p>
   */
  private Outcome doRecordAndReport(Scope scope, String identifier, ClientOrigin origin,
      Instant now) {
    requireNonNull(scope, "scope must not be null");
    requireNonNull(origin, "origin must not be null");
    requireNonNull(now, "now must not be null");

    Policy policy = policyFor(scope);
    if (identifier == null || identifier.isBlank()) {
      // No bucket work at all — not even the origin bucket. Consistent with LoginThrottleFilter,
      // which passes a submission with no username straight through: Spring Security rejects the
      // malformed form before any password comparison, so there is no guess to spend and nothing to
      // throttle. Counting it would only let a client inflate its own origin's tally.
      return new Outcome(Decision.ALLOWED, 0, policy.maxAttempts());
    }

    AuthAttemptCounter.Counter account = counter.increment(scope.name(),
        attemptKey(scope, identifier), now, policy.window());
    boolean accountBlocked = isBlocked(account, now);
    if (!accountBlocked && account.attemptCount() > policy.maxAttempts()) {
      counter.block(scope.name(), attemptKey(scope, identifier), now.plus(policy.blockDuration()));
      log.info("Throttling %s attempts after %d attempts in the window"
          .formatted(scope, account.attemptCount()));
      accountBlocked = true;
    }

    Optional<AuthAttemptCounter.Counter> originState = recordOriginAttempt(scope, origin, now);
    Decision decision = combine(originBindingFor(scope), accountBlocked, originState, now);
    return new Outcome(decision, account.attemptCount(), policy.maxAttempts());
  }

  /**
   * Records an attempt and reports whether the caller may perform the work.
   *
   * <p>The attempt is recorded even when it is already blocked, so a client that keeps hammering
   * does not shorten its own block by noticing that the count stopped moving.</p>
   *
   * <p>{@code @Transactional} is not decoration: without it the native upsert throws because there is
   * no active transaction, and the login path surfaces that as a 500. See Rule 5 in the class
   * javadoc.</p>
   *
   * @param scope the flow
   * @param identifier the address as typed; normalised and hashed here, never stored raw
   * @param now   the attempt instant
   * @return whether to perform the work. <b>Do not render this.</b>
   */
  @Transactional
  public Decision recordAttempt(Scope scope, String identifier, Instant now) {
    return recordAndReport(scope, identifier, now).decision();
  }

  /**
   * Records an attempt from a known origin.
   *
   * @param scope      the flow
   * @param identifier the address as typed
   * @param origin     the request origin, or {@link ClientOrigin#unknown()}
   * @param now        the attempt instant
   * @return whether to perform the work. <b>Do not render this.</b>
   */
  @Transactional
  public Decision recordAttempt(Scope scope, String identifier, ClientOrigin origin,
      Instant now) {
    return doRecordAndReport(scope, identifier, origin, now).decision();
  }

  /**
   * Reports whether this identifier is currently blocked, <b>without recording an attempt</b>.
   *
   * <p>Called before authentication is attempted. The result is for control flow only — the caller
   * must produce the <em>same</em> generic failure it would have produced anyway (see Rule 2).</p>
   *
   * <p>Read-only, but still needs a transaction: the native query runs on the container-managed
   * {@code EntityManager}.</p>
   *
   * @param scope      the flow
   * @param identifier the address as typed
   * @param now        the instant to evaluate against
   * @return {@code true} if the caller should skip the work
   */
  @Transactional(readOnly = true)
  public boolean isThrottled(Scope scope, String identifier, Instant now) {
    return doIsThrottled(scope, identifier, ClientOrigin.unknown(), now);
  }

  /**
   * Reports whether this identifier is blocked <b>for this origin</b>, without recording an attempt.
   *
   * <p>Applies the same combined rule as {@link #recordAndReport(Scope, String, ClientOrigin,
   * Instant)}, but as a pure read: a request rejected by the throttle is not an attempt, and counting
   * it would let a blocked client extend its own block by retrying.</p>
   *
   * <p>An implementation detail worth stating, because it is the whole point of the method: when the
   * account bucket is blocked but the requesting origin is clean, this returns {@code false} so the
   * victim's legitimate login proceeds.</p>
   *
   * @param scope      the flow
   * @param identifier the address as typed
   * @param origin     the request origin, or {@link ClientOrigin#unknown()}
   * @param now        the instant to evaluate against
   * @return {@code true} if the caller should skip the work
   */
  @Transactional(readOnly = true)
  public boolean isThrottled(Scope scope, String identifier, ClientOrigin origin, Instant now) {
    return doIsThrottled(scope, identifier, origin, now);
  }

  /** The read path, without a transaction annotation. See {@link #doRecordAndReport} for why. */
  private boolean doIsThrottled(Scope scope, String identifier, ClientOrigin origin, Instant now) {
    requireNonNull(scope, "scope must not be null");
    requireNonNull(origin, "origin must not be null");
    requireNonNull(now, "now must not be null");
    if (identifier == null || identifier.isBlank()) {
      return false;
    }
    AuthAttemptCounter.Counter account = counter.peek(scope.name(), attemptKey(scope, identifier));
    boolean accountBlocked = isBlocked(account, now);
    Optional<AuthAttemptCounter.Counter> originState = peekOrigin(scope, origin);
    return combine(originBindingFor(scope), accountBlocked, originState, now)
        == Decision.THROTTLED;
  }

  /**
   * Clears the bucket after a successful authentication.
   *
   * <p>Only ever called for a success. Clearing on failure would make the throttle a no-op.</p>
   *
   * <p><b>Clears the account bucket only.</b> Clearing the origin bucket here would let an attacker
   * with any account of their own reset their origin's failure tally between guesses, which would
   * leave the origin bucket permanently full of rows and permanently below its threshold — a control
   * that exists and does nothing. See Rule 3a.</p>
   *
   * @param scope      the flow
   * @param identifier the address as typed
   */
  @Transactional
  public void onSuccess(Scope scope, String identifier) {
    requireNonNull(scope, "scope must not be null");
    if (identifier == null || identifier.isBlank()) {
      return;
    }
    counter.clear(scope.name(), attemptKey(scope, identifier));
  }

  /**
   * Writes the origin bucket for one attempt, returning its state after the increment.
   *
   * <p>Returns empty when the flow has no origin bucket, when origin scope is switched off, or when
   * no trusted origin is available — in which case nothing is written at all and the decision falls
   * back to account scope.</p>
   */
  private Optional<AuthAttemptCounter.Counter> recordOriginAttempt(Scope scope, ClientOrigin origin,
      Instant now) {
    Optional<OriginBinding> binding = originBindingFor(scope);
    if (binding.isEmpty() || !origin.isKnown()) {
      reportIfDegraded(scope, origin);
      return Optional.empty();
    }
    OriginBinding bound = binding.get();
    String network = origin.network().orElseThrow();
    Instant windowEnd = now.plus(bound.policy().blockDuration());
    AuthAttemptCounter.Counter state = counter.increment(bound.scope().name(),
        attemptKey(bound.scope(), network), now, bound.policy().window());
    // A block is set once and never extended: extending it on every attempt while it is active would
    // let a client that keeps hammering hold itself — and everyone else behind the same NAT —
    // blocked indefinitely. Same rule as the account bucket.
    if (!isBlocked(state, now) && state.attemptCount() > bound.policy().maxAttempts()) {
      counter.block(bound.scope().name(), attemptKey(bound.scope(), network), windowEnd);
      log.info("Throttling %s attempts from one origin after %d attempts in the window"
          .formatted(bound.scope(), state.attemptCount()));
      return Optional.of(new AuthAttemptCounter.Counter(state.attemptCount(), windowEnd));
    }
    return Optional.of(state);
  }

  /** Reads the origin bucket without writing, or empty when the flow has none. */
  private Optional<AuthAttemptCounter.Counter> peekOrigin(Scope scope, ClientOrigin origin) {
    Optional<OriginBinding> binding = originBindingFor(scope);
    if (binding.isEmpty() || !origin.isKnown()) {
      reportIfDegraded(scope, origin);
      return Optional.empty();
    }
    OriginBinding bound = binding.get();
    return Optional.of(counter.peek(bound.scope().name(),
        attemptKey(bound.scope(), origin.network().orElseThrow())));
  }

  /**
   * The combined rule from the class javadoc (Rule 3).
   *
   * <p>Written once and used by both the write path and the read path, so a rejection and a
   * pre-check can never disagree about the same state.</p>
   *
   * <p>Takes only what it reads: whether the account bucket is blocked (the count decides the outcome,
   * not the state object) and the origin state it may attribute the damage to.</p>
   */
  private Decision combine(Optional<OriginBinding> binding, boolean accountBlocked,
      Optional<AuthAttemptCounter.Counter> originState, Instant now) {
    // Criterion 1: an origin that has overrun its own threshold is throttled regardless of which
    // accounts it is hitting.
    if (originState.isPresent() && isBlocked(originState.get(), now)) {
      return Decision.THROTTLED;
    }
    if (!accountBlocked) {
      return Decision.ALLOWED;
    }
    // The account bucket is full. Refuse unless this origin is provably not the one that filled it.
    if (originState.isEmpty() || binding.isEmpty()) {
      // No trusted origin (or origin scope off): the pre-origin behaviour, which cannot distinguish
      // the victim from the attacker. Documented as the residual risk in ops §1.8.
      return Decision.THROTTLED;
    }
    // Criterion 2: the victim's own origin has not contributed the damage, so let them through.
    // This matters as much for a resend as for a login: an attacker who fills a victim's resend bucket
    // must not be able to stop that victim from requesting a reset from their own network.
    return originState.get().attemptCount() >= binding.get().policy().accountAttributionThreshold()
        ? Decision.THROTTLED
        : Decision.ALLOWED;
  }

  private static boolean isBlocked(AuthAttemptCounter.Counter state, Instant now) {
    return state.blockedUntil() != null && now.isBefore(state.blockedUntil());
  }

  /**
   * The origin bucket and policy that belong to a flow.
   *
   * <p>Empty when origin scope is switched off, when the flow has no origin bucket, or when the flow
   * is itself an origin bucket (those are derived, never passed in by a caller).</p>
   *
   * <p><b>Every mail-sending flow is covered.</b> Account scope cannot bound these at all: the abuse
   * is "send one mail to a thousand different addresses", and an attacker who varies the address never
   * reuses an account bucket, so per-account throttling never sees them. That is why this mapping is
   * not optional for the resend and registration paths — see Rule 3c.</p>
   */
  private Optional<OriginBinding> originBindingFor(Scope scope) {
    return switch (scope) {
      case LOGIN -> binding(Scope.LOGIN_ORIGIN, loginOriginPolicy);
      case EMAIL_CONFIRMATION_RESEND, PASSWORD_RESET_RESEND ->
          binding(Scope.MAIL_ORIGIN, mailOriginPolicy);
      case REGISTRATION -> binding(Scope.REGISTRATION_ORIGIN, registrationOriginPolicy);
      // No origin bucket for the second factor, and that is deliberate rather than an oversight: the
      // flow is only reachable after a correct password for a specific account, so an attacker has to
      // spend the account's budget to reach it and a per-origin bucket would mostly punish NAT'd
      // institutions. Adding one later is a policy decision, not a wiring change (MFA-68).
      case SECOND_FACTOR -> Optional.empty();
      case LOGIN_ORIGIN, MAIL_ORIGIN, REGISTRATION_ORIGIN -> Optional.empty();
    };
  }

  private static Optional<OriginBinding> binding(Scope originScope, OriginPolicy policy) {
    return policy == null
        ? Optional.empty()
        : Optional.of(new OriginBinding(originScope, policy));
  }

  /** One flow's origin bucket and the thresholds that govern it. */
  private record OriginBinding(Scope scope, OriginPolicy policy) {

  }

  /**
   * Says once, in the log, that origin scope is configured but cannot be applied.
   *
   * <p>Without this a deployment whose proxy stopped sending the header would keep working, keep
   * reporting itself as origin-scoped, and silently be account-scoped only — the "dead control beside
   * a live feature" failure this codebase has already been bitten by twice.</p>
   */
  private void reportIfDegraded(Scope scope, ClientOrigin origin) {
    if (originBindingFor(scope).isEmpty() || origin.isKnown()) {
      return;
    }
    if (unknownOriginReported.compareAndSet(false, true)) {
      log.warn("Origin-scope throttling is enabled, but no trusted client origin could be resolved. "
          + "Falling back to account scope for this and every later request. Check that HAProxy "
          + "sets X-Forwarded-For and that server.forward-headers-strategy=native; see "
          + "docs/operations/installation-and-administration.md 1.2/1.5.");
    }
  }

  private Policy policyFor(Scope scope) {
    return switch (scope) {
      case LOGIN -> loginPolicy;
      // LOGIN_ORIGIN is never passed in by a caller: it is derived from LOGIN inside this class. Its
      // policy is the origin policy, kept out of this switch so the account limits cannot be applied
      // to it by accident.
      case LOGIN_ORIGIN, MAIL_ORIGIN, REGISTRATION_ORIGIN -> throw new IllegalArgumentException(
          "%s is derived from a caller-facing scope and must not be used directly"
              .formatted(scope));
      case EMAIL_CONFIRMATION_RESEND, PASSWORD_RESET_RESEND, REGISTRATION -> resendPolicy;
      case SECOND_FACTOR -> secondFactorPolicy;
    };
  }

  /**
   * How long a block lasts for this flow.
   *
   * <p>Exposed so a view can tell the visitor when to come back, instead of leaving them to guess
   * whether anything happened. It reveals a configured duration, not account state.</p>
   *
   * @param scope the flow
   * @return the configured block duration
   */
  public Duration blockDuration(Scope scope) {
    return policyFor(requireNonNull(scope, "scope must not be null")).blockDuration();
  }

  /**
   * How many attempts are permitted in one window for this flow.
   *
   * <p>Exposed so a view can say "you can request N more" instead of leaving the visitor to discover
   * the limit by hitting it. This is configured policy, not account state.</p>
   *
   * <p>Semantics: exactly {@code maxAttempts} attempts succeed per window. {@link #recordAttempt} compares the
   * count <em>after</em> the increment against this value with a strict {@code >}, so the
   * {@code maxAttempts + 1}-th attempt is the first one refused.</p>
   *
   * @param scope the flow
   * @return the configured maximum attempts per window
   */
  public int maxAttempts(Scope scope) {
    return policyFor(requireNonNull(scope, "scope must not be null")).maxAttempts();
  }

  /**
   * Whether origin buckets are written and consulted at all.
   *
   * <p>Exposed so the startup gate and the ops documentation can state the effective mode instead of
   * an operator inferring it from a property that may not have taken effect.</p>
   *
   * @return whether origin scope is enabled
   */
  public boolean originScopeEnabled() {
    // The login policy is representative: one switch creates all three, so they are either all present
    // or all null. Asserted by a spec rather than left as an assumption.
    return loginOriginPolicy != null;
  }

  /**
   * Derives the stored key: SHA-256 over {@code scope NUL normalised-identifier}.
   *
   * <p>Plain SHA-256, not a stretching function. The input is an email address — low entropy, so a
   * hash is not a secret — but it must not be <em>readable</em> in the table, because reading it
   * would turn a rate-limiting table into a list of addresses under attack. The scope is included so
   * one bucket cannot be replayed across flows.</p>
   *
   * <p>The same function keys origin buckets, with the grouped network in place of the address. That
   * is not cosmetic: an origin bucket holds the shared identity of everyone behind one NAT, so
   * storing it as a readable address would make the table a list of the institutions whose users are
   * failing to log in.</p>
   */
  static String attemptKey(Scope scope, String identifier) {
    return sha256Hex(scope.name() + "\u0000" + normalise(identifier));
  }

  /**
   * Trims and lower-cases with {@link Locale#ROOT}.
   *
   * <p>{@code Locale.ROOT} is not pedantry: {@code "I".toLowerCase()} yields a dotless {@code ı} in
   * a Turkish locale, which would silently split the bucket for any address containing an I.</p>
   *
   * <p>{@code EmailAddress} performs no case folding or stripping, so without this
   * {@code Alice@x.de}, {@code alice@x.de} and {@code alice@x.de } are three buckets pointing at one
   * account — and varying capitalisation grants unlimited attempts per victim.</p>
   *
   * <p>For a network the same folding is harmless (IPv4 dotted quads and the expanded IPv6 form carry
   * no letters but {@code a}–{@code f}), and it keeps one normalisation rule for all key material.</p>
   */
  static String normalise(String identifier) {
    return identifier.strip().toLowerCase(Locale.ROOT);
  }

  private static String sha256Hex(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      // SHA-256 is mandatory in every JRE. Failing closed here would be safer than continuing
      // without throttling, but there is no way to recover, so this is unreachable-by-contract.
      throw new IllegalStateException("SHA-256 is not available", e);
    }
  }

  /** Validated thresholds for one class of flow. */
  private record Policy(int maxAttempts, Duration window, Duration blockDuration, String name) {

    Policy {
      if (maxAttempts <= 0) {
        throw new IllegalArgumentException(
            "auth-throttle %s max-attempts must be positive, but was %d".formatted(name, maxAttempts));
      }
      requirePositive(window, name + " window");
      requirePositive(blockDuration, name + " block-duration");
    }

    private static void requirePositive(Duration duration, String what) {
      requireNonNull(duration, what + " must not be null");
      if (duration.isZero() || duration.isNegative()) {
        throw new IllegalArgumentException(what + " must be positive, but was " + duration);
      }
    }
  }

  /**
   * Validated origin thresholds.
   *
   * <p>Separate from {@link Policy} because it carries the <em>derived</em> attribution threshold, and
   * because the origin limits must not be confusable with the account limits by a copy-paste in the
   * constructor.</p>
   *
   * @param accountAttributionThreshold failures one origin must accumulate before it is treated as the
   *                                    source of a locked-out account's damage. Always
   *                                    {@code login.max-attempts + 1} — the number of failures that
   *                                    fills an account bucket — so an attacker who does the damage
   *                                    necessarily crosses it, while a legitimate user's own fumbling
   *                                    cannot, because the account bucket stops them at
   *                                    {@code login.max-attempts}. A separately tunable value would let
   *                                    the two be set such that the attacker stays below it, which
   *                                    silently restores the very lockout this clause removes.
   */
  private record OriginPolicy(int maxAttempts, Duration window, Duration blockDuration,
                              int accountAttributionThreshold) {

    OriginPolicy {
      if (maxAttempts <= 0) {
        throw new IllegalArgumentException(
            "auth-throttle login origin max-attempts must be positive, but was %d"
                .formatted(maxAttempts));
      }
      Policy.requirePositive(window, "login origin window");
      Policy.requirePositive(blockDuration, "login origin block-duration");
      if (accountAttributionThreshold <= 0) {
        throw new IllegalArgumentException(
            "the account threshold the origin attribution derives from must be positive, but was %d"
                .formatted(accountAttributionThreshold));
      }
    }
  }
}
