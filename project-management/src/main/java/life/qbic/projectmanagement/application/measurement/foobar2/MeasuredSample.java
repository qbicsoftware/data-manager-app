package life.qbic.projectmanagement.application.measurement.foobar2;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public interface MeasuredSample<T extends SampleProperties> {

  String sampleId();

  String sampleCode();

  T sampleProperties();
}
