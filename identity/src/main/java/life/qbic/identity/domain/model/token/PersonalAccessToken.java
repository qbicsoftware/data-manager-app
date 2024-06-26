package life.qbic.identity.domain.model.token;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * <b>Personal Access Token</b>
 *
 * <p>A user's personal access token to interact with QBiC's data API.</p>
 * <p>
 * The personal access token contains a small description for the user being able to describe the
 * purpose of the token.
 * <p>
 * Every token has a creation date, which is set automatically at token creation. The duration of a
 * token can be set by the client, policies are not enforced on object level.
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "personal_access_tokens")
public class PersonalAccessToken {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int id;
  @Column(name = "tokenValueEncrypted", unique = true)
  private String tokenValueEncrypted;
  @Column(name = "description")
  private String description;
  @Column(name = "userId")
  private String userId;
  @Column(name = "tokenId")
  private String tokenId;
  @Column(name = "creationDate")
  private Instant creationDate;
  @Column(name = "duration")
  private Duration duration;

  protected PersonalAccessToken() {
  }

  public PersonalAccessToken(String userId, String description, Duration duration,
      String encryptedSecret) {
    this.userId = userId;
    this.description = description;
    this.duration = duration;
    this.creationDate = Instant.now();
    this.tokenValueEncrypted = encryptedSecret;
    this.tokenId = UUID.randomUUID().toString();
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PersonalAccessToken that = (PersonalAccessToken) o;
    return Objects.equals(tokenId, that.tokenId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tokenId);
  }
}
