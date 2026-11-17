package life.qbic.identity.api;

/**
 * <b>The user-facing consequence of an ORCID link attempt.</b>
 *
 * <p>Carried from the callback to the settings page, and rendered there. It is an enum rather than a
 * string for one reason: a message that travels through the application must not be able to carry text
 * chosen by whoever put it there. Sending the sentence in the redirect URL — which this replaces — let
 * anyone craft {@code /settings/profile?error=1&reason=<their text>} and have the application render it
 * in a trusted notification on a trusted domain. That is a ready-made pretext for a phishing step, so
 * the set is closed and the wording lives in the message bundle.</p>
 *
 * <p><b>One-shot by contract.</b> A consumer must remove the value when it renders it. A stale success
 * followed by a fresh failure, or the reverse, is exactly the kind of thing that makes a user think the
 * feature is stuck.</p>
 *
 * @since 1.20.4
 */
public enum OidcLinkOutcomeMessage {

  /** The iD was written to the account. */
  LINKED,

  /** The account already held this iD; nothing to do, reported as success. */
  ALREADY_LINKED,

  /** Another account holds this iD, so it could not be attached to this one. */
  HELD_BY_ANOTHER_ACCOUNT,

  /** ORCID returned something that is not a usable iD. */
  INVALID_IDENTIFIER,

  /** The link was attempted for an account that no longer exists. */
  UNKNOWN_ACCOUNT,

  /** The session no longer identifies a password-authenticated user. */
  SESSION_ENDED,

  /** The provider round trip produced no iD at all. */
  NO_IDENTIFIER;

  /**
   * @return whether this outcome means the link now exists
   */
  public boolean isSuccess() {
    return this == LINKED || this == ALREADY_LINKED;
  }

  /**
   * @return the message-bundle key suffix, used as {@code profile.oidc.link.<key>}
   */
  public String messageKey() {
    return name().toLowerCase(java.util.Locale.ROOT);
  }
}
