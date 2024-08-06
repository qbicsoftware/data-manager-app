package life.qbic.datamanager.views.projects.project.measurements;

import java.io.InputStream;
import java.util.List;
import life.qbic.projectmanagement.application.measurement.MeasurementMetadata;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public class TsvMetadataParser implements MetadataParser {

  @Override
  public List<MeasurementMetadata> parseMetadata(InputStream inputStream) {
    return List.of();
  }
}
