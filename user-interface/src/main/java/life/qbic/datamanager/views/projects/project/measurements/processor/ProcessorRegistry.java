package life.qbic.datamanager.views.projects.project.measurements.processor;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementRegistrationInformationNGS;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementRegistrationInformationPxP;

/**
 * <b>Processor Registry</b>
 * <p>
 * A simple registry to enable access to available {@link MeasurementProcessor} depending on the
 * class to be processed.
 *
 * @since 1.11.0
 */
public class ProcessorRegistry {


  // Registry with suppliers
  private static final Map<Class<?>, Supplier<? extends MeasurementProcessor<?>>> registry = new HashMap();

  static {
    // Registration of matching classes and suppliers (e.g., constructors)
    registry.put(MeasurementRegistrationInformationNGS.class,
        MeasurementRegistrationProcessorNGS::new);
    registry.put(MeasurementRegistrationInformationPxP.class,
        MeasurementRegistrationProcessorPxP::new);
    // Add more mappings ...
  }

  private ProcessorRegistry() {
  }

  /**
   * Provides a {@link MeasurementProcessor} for the given class, if available.
   *
   * @param clazz the class for which a converter should be created. Must be registered in the
   *              {@link ProcessorRegistry#registry} during compile time.
   * @param <T>   the type of the class for which a converter should be created.
   * @return a converter for the given class.
   * @throws IllegalArgumentException if no converter is registered for the given class.
   * @since 1.10.0
   */
  public static <T> MeasurementProcessor<T> processorFor(Class<T> clazz)
      throws IllegalArgumentException {
    Supplier<? extends MeasurementProcessor<?>> supplier = registry.get(clazz);

    if (supplier == null) {
      throw new IllegalArgumentException("No converter registered for class " + clazz.getName());
    }
    return (MeasurementProcessor<T>) supplier.get();
  }
}
