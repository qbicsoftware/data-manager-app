package life.qbic.datamanager.files.parsing.converters;

import java.util.Map;
import java.util.function.Supplier;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementRegistrationInformationNGS;
import life.qbic.projectmanagement.application.api.AsyncProjectService.MeasurementRegistrationInformationPxP;
import life.qbic.projectmanagement.application.api.AsyncProjectService.SampleRegistrationInformation;
import life.qbic.projectmanagement.application.api.AsyncProjectService.SampleUpdateInformation;
import org.apache.commons.collections.map.HashedMap;

/**
 * Converter Factory for creating {@link MetadataConverterV2} instances.
 * <p>
 * The converter factory is a registry for known converter implementation of the
 * {@link MetadataConverterV2} interface.
 * <p>
 * It currently supports the following converters:
 *
 * <ul>
 *   <li>{@link SampleRegistrationMetadataConverter}</li>
 *   <li>{@link SampleUpdateMetadataConverter}</li>
 *   <li>{@link MeasurementRegistrationMetadataConverterNGS}</li>
 *   <li>{@link MeasurementRegistrationMetadataConverterPxP}</li>
 *   <li>{@link MeasurementUpdateMetadataConverterNGS}</li>
 *   <li>{@link MeasurementUpdateMetadataConverterPxP}</li>
 * </ul>
 *
 * @since 1.10.0
 */
public class ConverterFactory {

  // Registry with suppliers
  private static final Map<Class<?>, Supplier<? extends MetadataConverterV2<?>>> registry = new HashedMap();

  static {
    // Registration of matching classes and suppliers (e.g., constructors)
    registry.put(SampleRegistrationInformation.class,
        SampleRegistrationMetadataConverter::new);
    registry.put(SampleUpdateInformation.class,
        SampleUpdateMetadataConverter::new);
    registry.put(MeasurementRegistrationInformationNGS.class,
        MeasurementRegistrationMetadataConverterNGS::new);
    registry.put(MeasurementRegistrationInformationPxP.class,
        MeasurementRegistrationMetadataConverterPxP::new);
    registry.put(MeasurementUpdateMetadataConverterNGS.class,
        MeasurementUpdateMetadataConverterNGS::new);
    registry.put(MeasurementUpdateMetadataConverterPxP.class,
        MeasurementUpdateMetadataConverterPxP::new);
    // Add more mappings ...
  }

  private ConverterFactory() {
  }

  /**
   * Creates a {@link MetadataConverterV2} for the given class.
   *
   * @param clazz the class for which a converter should be created. Must be registered in the
   *              {@link ConverterFactory#registry} during compile time.
   * @param <T>   the type of the class for which a converter should be created.
   * @return a converter for the given class.
   * @throws IllegalArgumentException if no converter is registered for the given class.
   * @since 1.10.0
   */
  public static <T> MetadataConverterV2<T> create(Class<T> clazz)
      throws IllegalArgumentException {
    Supplier<? extends MetadataConverterV2<?>> supplier = registry.get(clazz);

    if (supplier == null) {
      throw new IllegalArgumentException("No converter registered for class " + clazz.getName());
    }
    return (MetadataConverterV2<T>) supplier.get();
  }

}
