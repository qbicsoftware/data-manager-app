package life.qbic.projectmanagement.infrastructure.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import life.qbic.projectmanagement.infrastructure.experiment.measurement.jpa.IpMeasurementJpaRepository;
import life.qbic.projectmanagement.infrastructure.experiment.measurement.jpa.NgsMeasurementJpaRepository;
import life.qbic.projectmanagement.infrastructure.experiment.measurement.jpa.PxpMeasurementJpaRepository;
import tools.jackson.databind.module.SimpleModule;

/**
 * Central Jackson configuration for Spring Boot 4 / Jackson 3.
 * <p>
 * Registers custom deserializers for measurement instrument and MS device records
 * via {@link JsonMapperBuilderCustomizer}, which is picked up by Spring Boot 4's
 * {@code JacksonAutoConfiguration} and applied to the shared {@code JsonMapper.Builder}
 * before construction of the singleton {@code JsonMapper}.
 * <p>
 * Replaces the deprecated {@code @JsonComponent}/{@code @JacksonComponent} pattern
 * from Spring Boot 3. Deserialization of instrument/MS device JSON payloads
 * (stored as JSON in database columns by {@code AttributeConverter}s) is resolved
 * transparently at the ObjectMapper level.
 *
 * @since 1.13.0
 */
@Configuration
public class JacksonConfig {

  @Bean
  public JsonMapperBuilderCustomizer measurementInstrumentModuleCustomizer() {
    return builder -> {
      SimpleModule module = new SimpleModule("MeasurementInstrumentModule");
      module.addDeserializer(
          NgsMeasurementJpaRepository.Instrument.class,
          new NgsMeasurementJpaRepository.Instrument.InstrumentJsonDeserializer());
      module.addDeserializer(
          PxpMeasurementJpaRepository.MsDevice.class,
          new PxpMeasurementJpaRepository.MsDevice.MsDeviceJsonDeserializer());
      module.addDeserializer(
          IpMeasurementJpaRepository.Instrument.class,
          new IpMeasurementJpaRepository.Instrument.InstrumentJsonDeserializer());
      builder.addModule(module);
    };
  }
}
