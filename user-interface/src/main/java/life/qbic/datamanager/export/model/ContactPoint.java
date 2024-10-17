package life.qbic.datamanager.export.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A schema.org <a href="https://schema.org/ContactPoint">ContactPoint</a> representation in Java.
 *
 * @since 1.6.0.
 */
public class ContactPoint {

  @JsonProperty(value = "@type", defaultValue = "ContactPoint")
  private String type;

  @JsonProperty(value = "name")
  private String name;

  @JsonProperty(value = "email")
  private String email;

  public static ContactPoint from(String name, String email) {
    ContactPoint contactPoint = new ContactPoint();
    contactPoint.name = name;
    contactPoint.email = email;
    return contactPoint;
  }

}
