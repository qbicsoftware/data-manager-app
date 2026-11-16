package life.qbic.identity.api;

import java.util.Optional;

/**
 * <b>The ORCID link on a local account (AUTH-R-07).</b>
 *
 * <p>ORCID is not an identity provider any more: it is an attribute of the account, in the same
 * category as the full name and the email address. This port is therefore the <em>only</em> way the
 * application writes that attribute, and it deliberately cannot do anything else — it cannot
 * authenticate, cannot create an account, and cannot read back anything except the stored pair.</p>
 *
 * <p>It lives in {@code identity-api} so the UI and the security layer can depend on the contract
 * without reaching into the identity domain model, per the module rules.</p>
 *
 * @since 1.20.4
 */
public interface OidcLinkService {

  /**
   * Links an ORCID iD to an existing account.
   *
   * <p><b>Idempotent for the caller's own account.</b> Linking the iD an account already holds is a
   * success, not a conflict. This is not a nicety: the previous implementation answered "OpenID already
   * in use" in that case, which made a link impossible to repair — a user whose stored iD was wrong, or
   * recorded against the wrong registry host, could never correct it.</p>
   *
   * <p>The caller must be authenticated and must have proved it with a password. This port does not
   * check that — it has no access to a request — so the obligation sits with the caller, which is why
   * the only caller is a filter that refuses to run otherwise. Stated here because it is the property
   * the whole design rests on.</p>
   *
   * @param userId     the account to link, never derived from the provider assertion
   * @param oidcId     the ORCID iD exactly as the provider returned it
   * @param oidcIssuer the issuer the iD was issued by
   * @return what happened; never throws for the expected outcomes
   */
  Outcome link(String userId, String oidcId, String oidcIssuer);

  /**
   * Removes the link from an account.
   *
   * <p>Required, not optional. Without it a link is permanent unless someone edits the database, so a
   * mistyped or stale iD could never be replaced — the account would be stuck with it forever.</p>
   *
   * @param userId the account to unlink
   * @return whether a link was removed
   */
  Outcome unlink(String userId);

  /**
   * @param userId the account to inspect
   * @return the stored {@code (id, issuer)} pair, or empty when the account holds no link
   */
  Optional<Link> findLink(String userId);

  /**
   * The stored attribute.
   *
   * @param oidcId     the ORCID iD
   * @param oidcIssuer the issuer the iD was issued by
   */
  record Link(String oidcId, String oidcIssuer) {

  }

  /**
   * The outcome of a link or unlink attempt.
   *
   * <p>A closed set rather than exceptions, because every one of these is an expected answer that the
   * caller must render — and the wording differs: "already linked to your account" is good news, "linked
   * to a different account" is a refusal, and neither is an error.</p>
   */
  sealed interface Outcome {

    /**
     * The link was written.
     */
    record Linked(Link link) implements Outcome {

    }

    /**
     * The account already held exactly this link. Treated as success by the caller.
     */
    record AlreadyLinked(Link link) implements Outcome {

    }

    /**
     * Another account holds this iD. The caller must refuse and must not disclose which account.
     */
    record HeldByAnotherAccount() implements Outcome {

    }

    /**
     * The link was removed.
     */
    record Unlinked() implements Outcome {

    }

    /**
     * There was no link to remove.
     */
    record NotLinked() implements Outcome {

    }

    /**
     * No account with that id exists.
     */
    record UnknownAccount() implements Outcome {

    }

    /**
     * The iD does not have the shape of an ORCID iD, or the issuer is blank. Recorded separately from
     * {@link UnknownAccount} so the caller can say "that is not an ORCID iD" without implying anything
     * about accounts.
     */
    record InvalidIdentifier() implements Outcome {

    }
  }
}
