package life.qbic.projectmanagement.application.api.fair;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A schema.org <a href="https://schema.org/ContactPoint">ContactPoint</a> representation in Java.
 *
 * @since 1.6.0.
 */
public class ContactPoint {

  @JsonProperty(value = "@type")
  private String type = "ContactPoint";

  @JsonProperty(value = "name")
  private String name;

  @JsonProperty(value = "email")
  private String email;

  @JsonProperty(value = "contactType")
  private String contactType;

  public static ContactPoint from(String name, String email, String contactType) {
    ContactPoint contactPoint = new ContactPoint();
    contactPoint.name = name;
    contactPoint.email = email;
    contactPoint.contactType = contactType;
    return contactPoint;
  }

  public String name() {
    return name;
  }

  public String email() {
    return email;
  }

  public String contactType() {
    return contactType;
  }

}
