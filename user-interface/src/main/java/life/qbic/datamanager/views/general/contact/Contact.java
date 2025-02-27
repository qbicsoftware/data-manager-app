package life.qbic.datamanager.views.general.contact;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import life.qbic.application.commons.ApplicationException;

/**
 * <b>A contact person</b>
 *
 * <p>Describes a contact person with a name and email.</p>
 *
 * @since 1.0.0
 */
public final class Contact implements Serializable {

  @Serial
  private static final long serialVersionUID = 8233688580418521324L;
  private String fullName;
  private String email;
  private String oidc;
  private String oidcIssuer;

  public Contact(String fullName, String email, String oidc, String oidcIssuer) {
    this.fullName = fullName;
    this.email = email;
    this.oidc = oidc;
    this.oidcIssuer = oidcIssuer;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public void setOidc(String oidc) {
    this.oidc = oidc;
  }

  public void setOidcIssuer(String oidcIssuer) {
    this.oidcIssuer = oidcIssuer;
  }

  public String fullName() {
    return fullName;
  }

  public String email() {
    return email;
  }

  public String oidc() {
    return oidc;
  }

  public String oidcIssuer() {
    return oidcIssuer;
  }

  public boolean isEmpty() {
    return (fullName == null || fullName.isBlank()) && (email == null
        || email.isBlank() && oidc == null || oidc.isBlank() && oidcIssuer.isBlank()
        || oidcIssuer == null);
  }

  public boolean isComplete() {
    return fullName != null && !fullName.isBlank() && email != null && !email.isBlank() && oidc
        != null && !oidc.isBlank() && oidcIssuer != null && !oidcIssuer.isBlank();
  }

  public boolean hasMinimalInformation() {
    return fullName != null && !fullName.isBlank() && email != null && !email.isBlank();
  }

  public life.qbic.projectmanagement.domain.model.project.Contact toDomainContact() {
    if (!hasMinimalInformation()) {
      throw new ApplicationException("Contact is not complete and cannot be converted: " + this);
    }
    return new life.qbic.projectmanagement.domain.model.project.Contact(fullName(), email(), oidc,
        oidcIssuer);
  }


  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    var that = (Contact) obj;
    return Objects.equals(this.fullName, that.fullName) &&
        Objects.equals(this.email, that.email);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fullName, email);
  }

  @Override
  public String toString() {
    return "Contact[" +
        "fullName=" + fullName + ", " +
        "email=" + email + ']';
  }


}
