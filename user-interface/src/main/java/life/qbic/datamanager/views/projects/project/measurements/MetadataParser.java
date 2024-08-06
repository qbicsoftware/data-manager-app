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
public interface MetadataParser {

  List<MeasurementMetadata> parseMetadata(InputStream inputStream);

  class NoHeaderRowException extends RuntimeException {

  }

  class NoMatchingDomainFoundException extends RuntimeException {

    public NoMatchingDomainFoundException(String message) {
      super(message);
    }
  }
}
