package life.qbic.projectmanagement.infrastructure.experiment.measurement.jpa;

import java.util.Optional;
import java.util.Set;
import life.qbic.projectmanagement.infrastructure.jpa.JpaFilter;
import org.springframework.data.jpa.domain.Specification;

/**
 * A filter for measurements to be used by the database. A {@link Specification} for measurements of
 * type {@code <T>} can be produced by this filter.
 *
 * @param <T> the type of JPA measurement this filter applies to
 * @param <X> the concrete type of the implementation
 */
public interface MeasurementFilter<T, X extends MeasurementFilter<T, X>> extends JpaFilter<T> {

  /**
   * @return the experiment this measurement filter filters for, if any.
   */
  Optional<String> getExperimentId();

  /**
   * Searches relevant fields of this measurement for the provided searchTerm. Implementing classes
   * can choose which fields are relevant to search and how to search. There is no assumption on
   * whether this is an exact search or a fuzzy search.
   *
   * @param searchTerm the term to search for.
   * @return a filter configured with the searchTerm.
   */
  X anyContaining(String searchTerm);

  /**
   * Sets the client time zone offset. Provided values are expected in milliseconds.
   *
   * @param clientTimeZoneOffsetMillis milliseconds to be converted to client time zone offset.
   * @return a filter configured with the client time zone offset
   */
  X atClientTimeOffset(int clientTimeZoneOffsetMillis);

  /**
   * Specifies which samples must measured by accepted measurements. Successive calls to this method
   * aggregate provided ids.
   *
   * @param sampleIds the database identifiers of the samples to filter for
   * @return a configured filter that only accepts measurements that measure provided samples
   */
  X includingSamples(Set<String> sampleIds);

  /**
   * Specifies which samples must not be measured by measurements. Successive calls to this method
   * aggregate provided ids.
   *
   * @param sampleIds the database identifiers of the samples to filter for
   * @return a configured filter that only accepts measurements that do not measure provided samples
   */
  X excludingSamples(Set<String> sampleIds);

}
