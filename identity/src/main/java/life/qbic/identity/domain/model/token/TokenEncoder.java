package life.qbic.identity.domain.model.token;

/**
 * Encodes Access Tokens.
 */
public interface TokenEncoder {

  String encode(char[] token);

  boolean matches(char[] token, String encodedToken);
}
