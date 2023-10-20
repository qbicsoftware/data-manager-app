package life.qbic.projectmanagement.domain.model.project;

import jakarta.persistence.Embeddable;
import java.util.Objects;

/**
 * A small business value object representing information about funding.
 * <p>
 * Funding is described by a free-text label and a funding body's reference identifier.
 * <p>
 * For example:
 * <ul>
 *   <li>SFB (label)</li>
 *   <li>SFB 1101 (reference id)</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Embeddable
public class Funding {

  private String grant;

  private String grantId;

  protected Funding() {

  }

  private Funding(String grant, String grantId) {
    this.grant = grant;
    this.grantId = grantId;
  }

  public static Funding of(String grant, String grantId) {
    return new Funding(grant, grantId);
  }

  public String grant() {
    return this.grant;
  }

  public String grantId() {
    return this.grantId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Funding funding = (Funding) o;
    return Objects.equals(grant, funding.grant) && Objects.equals(grantId,
        funding.grantId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(grant, grantId);
  }
}
