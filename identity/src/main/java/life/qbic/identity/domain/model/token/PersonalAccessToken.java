package life.qbic.identity.domain.model.token;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
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

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int id;
  private String tokenValueEncrypted;
  private String description;
  private String userId;
  private String tokenId;
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
    this.tokenId = UUID.randomUUID().toString();
  }


  public static PersonalAccessToken create(String userId, String description, Duration duration,
      String secret) {
    return new PersonalAccessToken(userId, description, duration,
        PasswordEncryptionPolicy.instance().encrypt(
            secret.toCharArray()));
  }

  public String description() {
    return description;
  }

  public Duration duration() {
    return duration;
  }

  public Instant creationDate() {
    return creationDate;
  }

  public String tokenId() {
    return tokenId;
  }
  public boolean hasExpired() {
    return Instant.now().minus(duration).isAfter(creationDate);
  }

  public boolean isValid() {
    return !hasExpired();
  }

  public Instant expirationDate() {
    return creationDate.plus(duration);
  }

  public String userId() {
    return userId;
  }
}
