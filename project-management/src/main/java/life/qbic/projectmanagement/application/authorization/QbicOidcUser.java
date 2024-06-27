package life.qbic.projectmanagement.application.authorization;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public class QbicOidcUser extends DefaultOidcUser {

  private final QbicUserInfo qbicUserInfo;

  public record QbicUserInfo(String userId, String fullName, String email, boolean active) {

  }

  @Override
  public final boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof QbicOidcUser that)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }

    return Objects.equals(qbicUserInfo, that.qbicUserInfo);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + Objects.hashCode(qbicUserInfo);
    return result;
  }

  public QbicOidcUser(Collection<? extends GrantedAuthority> authorities, OidcIdToken idToken,
      OidcUserInfo userInfo, QbicUserInfo qbicUserInfo) {
    super(authorities, idToken, userInfo);
    this.qbicUserInfo = requireNonNull(qbicUserInfo, "qbicUserInfo must not be null");
  }

  public String getQbicUserId() {
    return qbicUserInfo.userId();
  }

  @Override
  public String getFullName() {
    return Optional.ofNullable(super.getFullName()).orElse(qbicUserInfo.fullName());
  }

  @Override
  public String getEmail() {
    return Optional.ofNullable(super.getEmail()).orElse(qbicUserInfo.email());
  }

  public boolean isActive() {
    return qbicUserInfo.active();
  }
}
