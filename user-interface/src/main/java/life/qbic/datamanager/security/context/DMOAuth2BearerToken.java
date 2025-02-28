package life.qbic.datamanager.security.context;

import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public record DMOAuth2BearerToken(OAuth2AccessToken accessToken, OAuth2RefreshToken refreshToken, String issuer) {

}
