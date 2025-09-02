package life.qbic.projectmanagement.infrastructure.dataset;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Set;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Entity
@Table(name = "remote_measurement_data")
public class LocalRawDatasetEntry {
  @Id
  @Column(name = "measurement_id")
  private String measurementId;

  @Column(name = "file_count")
  private int fileCount;

  @Column(name = "file_types")
  @Convert(converter = FileTypesConverter.class)
  private Set<String> fileTypes;

}
