package life.qbic.projectmanagement.infrastructure.dataset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Entity
public class LocalRawDatasetPxpEntry {

  @Id
  @Column(name = "measurement_id")
  private String id;


  @Column(name = "total_filesize_bytes")
  long totalFileSizeBytes;

}
