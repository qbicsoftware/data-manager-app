package life.qbic.projectmanagement.domain.model.project.translation;

import jakarta.persistence.AttributeConverter;
import java.nio.charset.Charset;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class CharsetConverter implements AttributeConverter<Charset, String> {

  @Override
  public String convertToDatabaseColumn(Charset charset) {
    return charset.name();
  }

  @Override
  public Charset convertToEntityAttribute(String s) {
    return Charset.forName(s);
  }
}
