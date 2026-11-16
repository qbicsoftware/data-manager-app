package life.qbic.identity.api;

import java.util.regex.Pattern;

/**
 * <b>The ORCID iD format, in one place.</b>
 *
 * <p>An ORCID iD looks like {@code 0000-0002-1825-0097}: four groups of four hexadecimal digits, the
 * last of which may end in {@code X} (the ISO 7064 MOD 11-2 check character).</p>
 *
 * <h3>Why this is a guard and not cosmetic validation</h3>
 * <p>The iD is stored on the account and then rendered — as a link on the profile, in project contacts,
 * and in the person picker that matches a searched researcher to a platform account. Nothing validated
 * it before this class existed: whatever the provider put in {@code sub} was written verbatim, and a
 * value with a {@code /} or a {@code ?} in it would end up inside a URL.</p>
 *
 * <h3>What it deliberately does not do</h3>
 * <p>It does not verify the check digit. That was considered and rejected: the value arrives over TLS
 * from ORCID in a token exchange we perform with our own client secret, and a caller that can forge
 * {@code sub} can forge a well-formed iD just as easily. A checksum therefore adds no protection against
 * the threat we actually face, while adding a second rule that can disagree with the first. Revisit only
 * if an iD is ever accepted from a source that is not ORCID.</p>
 *
 * <p>It also does not normalise. {@code 0000-0002-1825-009x} is rejected rather than upper-cased: a
 * value that is not what the provider returned is a value we should not be storing, and silently
 * repairing input is how two representations of the same researcher end up in the database.</p>
 *
 * @since 1.20.4
 */
public final class OrcidIds {

  /**
   * The canonical form. No whitespace, no URL, no prefix — the bare iD as ORCID returns it in {@code sub}.
   */
  private static final Pattern PATTERN = Pattern.compile("^\\d{4}-\\d{4}-\\d{4}-\\d{3}[\\dX]$");

  private OrcidIds() {
    // utility class
  }

  /**
   * @param candidate the value to check; may be {@code null}
   * @return {@code true} only for a value in canonical ORCID iD form
   */
  public static boolean isValid(String candidate) {
    return candidate != null && PATTERN.matcher(candidate).matches();
  }

  /**
   * The canonical form of an iD as it appears in a URL path.
   *
   * <p>Only the iD is built here; the host is deliberately not. Production and sandbox are different
   * registries with different hosts, and this module has no business knowing which one a deployment
   * talks to. The caller resolves the host from the stored issuer — see the application's
   * {@code OidcType}, which is the one place that mapping exists.</p>
   *
   * @param oidcId the iD; must be valid
   * @return the iD, unchanged, for concatenation onto a registry base URL
   * @throws IllegalArgumentException if the iD is not in canonical form
   */
  public static String recordPath(String oidcId) {
    if (!isValid(oidcId)) {
      throw new IllegalArgumentException("Not an ORCID iD: " + oidcId);
    }
    return oidcId;
  }
}
