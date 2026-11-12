package life.qbic.datamanager.security;

import static java.util.Objects.requireNonNull;

import java.util.List;
import life.qbic.identity.api.UserInformationService;
import life.qbic.identity.api.UserPassword;
import life.qbic.identity.api.UserPasswordService;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.authorization.QbicUserDetails;
import life.qbic.projectmanagement.application.authorization.User;
import life.qbic.projectmanagement.application.authorization.authorities.UserAuthorityProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * <b>Loads account data for the password login.</b>
 *
 * <h3>What this class must not do: answer "does this address exist?"</h3>
 * <p>AUTH-NFR-03 forbids disclosing account existence or confirmation state to an unauthenticated
 * caller. Two separate concerns live here, and they need different answers.</p>
 *
 * <h4>1. Timing — already handled below this class, so do not "help"</h4>
 * <p>{@code DaoAuthenticationProvider} catches the {@link UsernameNotFoundException} thrown here and
 * runs {@code mitigateAgainstTimingAttack}, which performs one full password derivation against a
 * dummy hash. A wrong password for a known account performs exactly one derivation too. The two
 * paths therefore already cost the same, and adding a second dummy derivation here would make an
 * unknown address cost <em>twice</em> as much — turning this class into the oracle it is trying to
 * avoid. Likewise, {@code alwaysPerformAdditionalChecksOnUser} defaults to {@code true}, so an
 * account rejected by the pre-authentication check (e.g. not yet confirmed) still pays for a
 * derivation before the rejection surfaces.</p>
 * <p>Verified against Spring Security 7.1.2 sources, not assumed: the mitigation is in
 * {@code DaoAuthenticationProvider#retrieveUser}, applies to that one exception type, and is
 * skipped only when the configured {@code UserCache} already holds the user — and this application
 * leaves the default {@code NullUserCache} in place.</p>
 *
 * <h4>2. Outcome — this is the defect that was actually here</h4>
 * <p>An account with no stored password used to be handed to the provider as {@code null} via
 * {@code encryptedPassword.orElseGet(null)}. The provider then called
 * {@code passwordEncoder.matches(presented, null)}, and {@code PasswordEncryptionPolicy} threw a
 * {@link NullPointerException} on {@code null.split(":")}. That escaped {@code authenticate} as an
 * unhandled 500 — a third outcome, distinguishable from both "wrong password" and "no account", and
 * for exactly the accounts that have no password at all (ORCID-only accounts, see AUTH-R-07 and
 * #1683).</p>
 * <p>{@link #NO_PASSWORD} fixes that: it is a syntactically valid stored-password value whose hash
 * no password can derive, so the comparison is a real derivation that always fails. A no-password
 * account and a wrong password are then the same outcome, reached at the same cost.</p>
 *
 * @since 1.0.0
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

  private static final Logger log = LoggerFactory.logger(UserDetailsServiceImpl.class);

  /**
   * Stored-password stand-in for an account that has none.
   *
   * <p>Deliberately well-formed — {@code iterations:salt:hash} is the format
   * {@link life.qbic.identity.domain.model.PasswordEncryptionPolicy#doPasswordsMatch} parses — so the
   * comparison performs a real derivation instead of failing fast on a malformed value. The salt is
   * the expected 20 bytes and the hash 32 zero bytes; no password derives an all-zero 32-byte hash,
   * so the comparison always fails.</p>
   *
   * <p>This is not a credential. It is a value that is impossible to match, and it is never written
   * to the database.</p>
   */
  static final String NO_PASSWORD =
      "100000:0000000000000000000000000000000000000000:"
          + "0000000000000000000000000000000000000000000000000000000000000000";

  private final UserInformationService userInformationService;

  private final UserPasswordService userPasswordService;

  private final UserAuthorityProvider userAuthorityProvider;

  @Autowired
  UserDetailsServiceImpl(UserInformationService userInformationService,
      UserPasswordService userPasswordService,
      UserAuthorityProvider userAuthorityProvider) {
    this.userInformationService = requireNonNull(userInformationService,
        "userInformationService must not be null");
    this.userPasswordService = requireNonNull(userPasswordService,
        "userPasswordService must not be null");
    this.userAuthorityProvider = requireNonNull(userAuthorityProvider,
        "userAuthorityProvider must not be null");
  }

  /**
   * @param mailAddress the username identifying the user whose data is required.
   * @return existing user details
   * @throws UsernameNotFoundException if no user with this email exists
   */
  @Override
  public UserDetails loadUserByUsername(String mailAddress) throws UsernameNotFoundException {
    var userInfo = userInformationService.findByEmail(mailAddress)
        .orElseThrow(() -> {
          // Logged at debug and without the address: the log must not become the enumeration
          // oracle that the response is not. The provider turns this exception into
          // BadCredentialsException, so it does not reach the visitor as a distinct message.
          log.debug("Login attempted for an address with no account");
          return new UsernameNotFoundException("Cannot find user");
        });

    // Never null: an account without a password gets NO_PASSWORD, so the provider performs a real
    // derivation that always fails, instead of NPE-ing on a null stored password.
    var encryptedPassword = userPasswordService.findEncryptedPasswordForUser(userInfo.id())
        .map(UserPassword::encryptedPassword)
        .orElse(NO_PASSWORD);

    List<GrantedAuthority> authorities = userAuthorityProvider.getAuthoritiesByUserId(userInfo.id());
    var user = new User(userInfo.id(), userInfo.fullName(), userInfo.platformUserName(),
        userInfo.emailAddress(), encryptedPassword, userInfo.isActive(), userInfo.oidcId(),
        userInfo.oidcIssuer());
    return new QbicUserDetails(user, authorities);
  }
}
