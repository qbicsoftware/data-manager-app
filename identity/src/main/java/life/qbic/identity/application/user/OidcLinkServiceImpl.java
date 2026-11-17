package life.qbic.identity.application.user;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import life.qbic.identity.api.OidcLinkService;
import life.qbic.identity.api.OrcidIds;
import life.qbic.identity.api.UserInfo;
import life.qbic.identity.domain.model.User;
import life.qbic.identity.domain.model.UserId;
import life.qbic.identity.domain.repository.UserRepository;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <b>Reads and writes the ORCID attribute on a local account (AUTH-R-07).</b>
 *
 * <h3>What makes this safe to expose</h3>
 * <p>Nothing here can create an account, establish a session, or resolve an account <em>from</em> an iD.
 * The direction is always account → iD, never iD → account. That asymmetry is the whole point: the old
 * flow looked an account up by provider assertion and built a principal from it, which is what made
 * ORCID a login.</p>
 *
 * <p>The one place that reads in the other direction is the uniqueness check, and it only ever answers
 * "taken or not" — never which account, and never anything about that account.</p>
 *
 * @since 1.20.4
 */
@Service
public class OidcLinkServiceImpl implements OidcLinkService {

  private static final Logger log = LoggerFactory.logger(OidcLinkServiceImpl.class);

  private final UserRepository userRepository;

  public OidcLinkServiceImpl(UserRepository userRepository) {
    this.userRepository = requireNonNull(userRepository, "userRepository must not be null");
  }

  @Override
  @Transactional
  public Outcome link(String userId, String oidcId, String oidcIssuer) {
    if (!OrcidIds.isValid(oidcId) || oidcIssuer == null || oidcIssuer.isBlank()) {
      // The rejected value is logged, truncated and quoted: without it the line says only "something was
      // wrong", which is what made an earlier rejection undiagnosable. Truncated so a hostile value
      // cannot be used to flood or forge a log line.
      log.warn("Rejected an ORCID link for user %s: identifier '%s' or issuer '%s' is not in canonical form"
          .formatted(userId, abbreviate(oidcId), abbreviate(oidcIssuer)));
      return new Outcome.InvalidIdentifier();
    }

    Optional<User> maybeUser = findUser(userId);
    if (maybeUser.isEmpty()) {
      return new Outcome.UnknownAccount();
    }
    User user = maybeUser.get();

    // Already linked to exactly this? Success, not a conflict. A user must be able to re-run a link —
    // for example after the popup was closed too early — without being told something is wrong.
    if (oidcId.equals(user.oidcId()) && oidcIssuer.equals(user.oidcIssuer())) {
      // Logged even though nothing changed: "the user pressed Connect again" is the signal that tells a
      // support case apart from "the button does nothing", and it is invisible otherwise.
      log.info("ORCID iD %s (issuer %s) was already linked to user %s; nothing to change"
          .formatted(oidcId, oidcIssuer, userId));
      return new Outcome.AlreadyLinked(new Link(oidcId, oidcIssuer));
    }

    Optional<User> holder = userRepository.findByOidc(oidcId, oidcIssuer);
    if (holder.isPresent() && !holder.get().id().equals(user.id())) {
      // The iD is logged; the holding account is NOT. The iD is public researcher data, but the mapping
      // from that iD to a platform account is exactly what must not leak — including into a log that
      // support staff can read. Without the iD this line was useless for diagnosing a real rejection:
      // it could not be told apart from a rejection of a *different* iD for the same user.
      log.info("Rejected an ORCID link for user %s: iD %s (issuer %s) is held by a different account"
          .formatted(userId, oidcId, oidcIssuer));
      return new Outcome.HeldByAnotherAccount();
    }

    user.setOidcEntry(oidcIssuer, oidcId);
    userRepository.updateUser(user);
    log.info("Linked ORCID iD %s (issuer %s) to user %s".formatted(oidcId, oidcIssuer, userId));
    return new Outcome.Linked(new Link(oidcId, oidcIssuer));
  }

  @Override
  @Transactional
  public Outcome unlink(String userId) {
    Optional<User> maybeUser = findUser(userId);
    if (maybeUser.isEmpty()) {
      return new Outcome.UnknownAccount();
    }
    User user = maybeUser.get();
    if (user.oidcId() == null && user.oidcIssuer() == null) {
      return new Outcome.NotLinked();
    }
    // Remember what was removed: the audit line is the only record that this account ever held that iD
    // once the row is overwritten.
    String previousId = user.oidcId();
    String previousIssuer = user.oidcIssuer();
    user.clearOidcEntry();
    userRepository.updateUser(user);
    log.info("Unlinked ORCID iD %s (issuer %s) from user %s"
        .formatted(previousId, previousIssuer, userId));
    return new Outcome.Unlinked();
  }

  /**
   * Looks an account up, treating a malformed id as "no such account".
   *
   * <p>{@code UserId.from} throws on anything that is not a UUID. The id comes from the session here,
   * so a malformed one means a corrupted session rather than hostile input — but it must not surface as
   * an unhandled exception in the middle of a link, leaving the browser on a blank page.</p>
   */
  /**
   * Renders a value for a log line without letting it dominate or forge one.
   *
   * <p>Log-injection hygiene: regardless of where the value came from, a newline inside it would split
   * the record, and an unbounded value would drown the file.</p>
   */
  private static String abbreviate(String value) {
    if (value == null) {
      return "<null>";
    }
    String flat = value.replaceAll("\\s", "_");
    return flat.length() <= 64 ? flat : flat.substring(0, 64) + "...";
  }

  private Optional<User> findUser(String userId) {
    try {
      return userRepository.findById(UserId.from(userId));
    } catch (IllegalArgumentException malformed) {
      log.warn("ORCID link attempted for a malformed user id");
      return Optional.empty();
    }
  }

  @Override
  public Optional<Link> findLink(String userId) {
    return findUser(userId)
        .filter(user -> user.oidcId() != null && !user.oidcId().isBlank())
        .map(user -> new Link(user.oidcId(), user.oidcIssuer()));
  }
}
