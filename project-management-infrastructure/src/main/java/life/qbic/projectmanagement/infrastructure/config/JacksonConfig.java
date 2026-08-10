package life.qbic.projectmanagement.infrastructure.config;

import life.qbic.projectmanagement.infrastructure.experiment.measurement.jpa.IpMeasurementJpaRepository;
import life.qbic.projectmanagement.infrastructure.experiment.measurement.jpa.NgsMeasurementJpaRepository;
import life.qbic.projectmanagement.infrastructure.experiment.measurement.jpa.PxpMeasurementJpaRepository;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;
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

  @Bean(name = "nullableFieldsObjectMapper")
  public JsonMapper nullableFieldsObjectMapper(JsonMapper.Builder builder) {
    builder.disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
    return builder.build();
  }

  /**
   * This is usually provided by the
   * {@link org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration}. As we provide
   * an additional JsonMapper and the default is only provided
   * {@link org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean}, we need to
   * provide it explicitly
   *
   * @param builder
   * @return a JsonMapper
   */
  @Bean(name = "jacksonJsonMapper")
  @Primary
  JsonMapper jacksonJsonMapper(JsonMapper.Builder builder) {
    return builder.build();
  }

  /**
   * Adds a builder customizer to the default {@link JsonMapper.Builder} provided by
   * {@link org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration}
   *
   * @return a customizer providing measurement related DTO deserializers
   */
  @Bean
  public JsonMapperBuilderCustomizer measurementDtoBuilderCustomizer() {
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
