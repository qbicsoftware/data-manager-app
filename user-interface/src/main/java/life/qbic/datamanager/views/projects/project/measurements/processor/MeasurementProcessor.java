package life.qbic.datamanager.views.projects.project.measurements.processor;

import java.util.List;

/**
 * <b>Measurement Processor</b>
 * <p>
 * A processor for measurement information during registration or update user stories.
 * <p>
 * Processors are meant to process classes after I/O events (e.g., user uploads of metadata), such
 * that processed data can then further be used for the interaction with application services.
 * <p>
 * It is not required to provide business logic in the processor, since this is the responsibility
 * of the domain. However, it is a good way to clean up or condense information from or for I/O
 * tasks to keep the service complexity thin and tidy.
 * <p>
 * It is also not the responsibility of services to deal with I/O related tasks.
 *
 * @since 1.11.0
 */
@FunctionalInterface
public interface MeasurementProcessor<T> {

  /**
   * Processes a {@link List<T>} of objects of a certain type {@link T}.
   *
   * @param request the elements to process
   * @return the processed elements
   * @since 1.11.0
   */
  List<T> process(List<T> request);

}
