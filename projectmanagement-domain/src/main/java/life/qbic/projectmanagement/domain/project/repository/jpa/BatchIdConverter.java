package life.qbic.projectmanagement.domain.project.repository.jpa;

import jakarta.persistence.AttributeConverter;
import life.qbic.projectmanagement.domain.project.sample.BatchId;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class BatchIdConverter implements AttributeConverter<BatchId, String> {

  @Override
  public String convertToDatabaseColumn(BatchId batchId) {
    return batchId.value();
  }

  @Override
  public BatchId convertToEntityAttribute(String s) {
    return BatchId.parse(s);
  }
}
