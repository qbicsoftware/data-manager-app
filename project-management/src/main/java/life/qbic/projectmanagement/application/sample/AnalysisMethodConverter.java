package life.qbic.projectmanagement.application.sample;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import life.qbic.projectmanagement.domain.model.sample.AnalysisMethod;

@Converter(autoApply = true)
public class AnalysisMethodConverter implements AttributeConverter<AnalysisMethod, String> {

  @Override
  public String convertToDatabaseColumn(AnalysisMethod attribute) {
    if (attribute == null) {
      return null;
    }
    return attribute.getAbbreviation();
  }

  @Override
  public AnalysisMethod convertToEntityAttribute(String dbData) {
    if (dbData == null) {
      return null;
    }
    return AnalysisMethod.forAbbreviation(dbData).orElse(null);
  }

}
