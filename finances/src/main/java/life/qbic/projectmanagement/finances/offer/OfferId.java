package life.qbic.projectmanagement.finances.offer;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class OfferId {

  private String id;

  public static OfferId of(String id) {
    return new OfferId(id);
  }

  private OfferId(String id) {
    this.id = id;
  }

  public String id() {
    return id;
  }

  private void setId(String id) {
    this.id = id;
  }

  public static class Converter implements AttributeConverter<OfferId, String> {

    @Override
    public String convertToDatabaseColumn(OfferId offerId) {
      return offerId.id();
    }

    @Override
    public OfferId convertToEntityAttribute(String s) {
      return OfferId.of(s);
    }
  }
}
