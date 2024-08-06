package life.qbic.projectmanagement.application.measurement.foobar2;

import java.util.Set;
import org.springframework.data.jdbc.core.mapping.AggregateReference;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public class SamplePool<T extends SampleProperties> {

  private String poolName;
  private Set<AggregateReference<MeasuredSample<T>, String>> containedSamples;
}
