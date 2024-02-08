package life.qbic.identity.domain.model.token;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import life.qbic.identity.domain.model.PasswordEncryptionPolicy;

/**
 * <b>Personal Access Token</b>
 *
 * <p></p>
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "personal_access_tokens")
public class PersonalAccessToken {

  private String tokenValueEncrypted;
  private String description;
  private String userId;
  @Id
  private int id;

  private Instant creationDate;

  private Duration duration;

  protected PersonalAccessToken() {
  }

  private PersonalAccessToken(String userId, String description, Duration duration,
      String encryptedSecret) {
    this.userId = userId;
    this.description = description;
    this.duration = duration;
    this.creationDate = Instant.now();
    this.tokenValueEncrypted = encryptedSecret;
  }


  public static PersonalAccessToken create(String userId, String description, Duration duration,
      String secret) {
    return new PersonalAccessToken(userId, description, duration,
        PasswordEncryptionPolicy.instance().encrypt(
            secret.toCharArray()));
  }

  public boolean hasExpired() {
    return Instant.now().minus(duration).isAfter(creationDate);
  }

  public boolean isValid() {
    return !hasExpired();
  }

}
