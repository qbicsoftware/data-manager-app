package life.qbic.projectmanagement.application.measurement.foobar;

import java.util.List;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public interface HasSampleSpecificMetadata<T extends SampleSpecificMeasurementMetadata> {

  List<T> getSampleSpecificMetadata();
}
