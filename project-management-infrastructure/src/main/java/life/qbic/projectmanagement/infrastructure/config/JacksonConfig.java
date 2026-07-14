package life.qbic.projectmanagement.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Central Jackson configuration for Spring Boot 4 / Jackson 3.
 * <p>
 * Replaces {@code @JsonComponent} which was removed in Spring Boot 4. Custom
 * deserializers are now explicitly registered here so that the single shared
 * {@link tools.jackson.databind.ObjectMapper} bean knows about them.
 *
 * @since 1.12.10
 */
@Configuration
public class JacksonConfig {

    @Bean
    public tools.jackson.databind.ObjectMapper objectMapper() {
        tools.jackson.databind.ObjectMapper mapper = new tools.jackson.databind.ObjectMapper();
        // Add modules that need to be registered for time/date support
        mapper.registerModule(new tools.jackson.datatype.jsr310.JavaTimeModule());
        // Deserializers must be registered before reading JPA entities to ensure
        // Jackson can parse Instrument/MsDevice from their JSON payloads
        mapper.registerModule(buildInstrumentModule());
        mapper.registerModule(buildMsDeviceModule());
        return mapper;
    }

    /**
     * Builds a Jackson module with custom deserializers for Instrument records
     * (used in NGS and Immunopeptidomics measurement metadata).
     * <p>
     * These Instrument types are stored as JSON in the database and are read
     * back via {@code AttributeConverter} which relies on Jackson deserialization.
     */
    private tools.jackson.databind.Module buildInstrumentModule() {
        return new tools.jackson.databind.Module.Base() {
            @Override
            public void setupModule(tools.jackson.databind.Module.SetupContext context) {
                // IP (Immunopeptidomics) Instrument deserializer
                context.addDeserializers(new IpInstrumentDeserializer());
                // NGS Instrument deserializer
                context.addDeserializers(new NgsInstrumentDeserializer());
            }
        };
    }

    /**
     * Builds a Jackson module with the custom deserializer for MsDevice records
     * (used in PXP measurement metadata).
     */
    private tools.jackson.databind.Module buildMsDeviceModule() {
        return new tools.jackson.databind.Module.Base() {
            @Override
            public void setupModule(tools.jackson.databind.Module.SetupContext context) {
                context.addDeserializers(new MsDeviceDeserializer());
            }
        };
    }

    /**
     * JSON deserializer for {@code Instrument} record from IpMeasurementJpaRepository.
     */
    private static class IpInstrumentDeserializer extends tools.jackson.databind.JsonDeserializer<life.qbic.projectmanagement.infrastructure.experiment.measurement.jpa.IpMeasurementJpaRepository.Instrument> {
        @Override
        public life.qbic.projectmanagement.infrastructure.experiment.measurement.jpa.IpMeasurementJpaRepository.Instrument deserialize(
                tools.jackson.core.JsonParser jsonParser,
                tools.jackson.databind.DeserializationContext ctxt
        ) throws java.io.IOException {
            tools.jackson.databind.JsonNode tree = jsonParser.readValueAsTree();
            String oboId = java.util.Optional.ofNullable(tree.get("name"))
                    .map(tools.jackson.databind.JsonNode::asText)
                    .map(it -> it.replace("_", ":"))
                    .orElseThrow(() -> new tools.jackson.core.JsonParseException(jsonParser, "Could not parse instrument oboId."));
            String label = java.util.Optional.ofNullable(tree.get("label"))
                    .map(tools.jackson.databind.JsonNode::asText)
                    .orElseThrow(() -> new tools.jackson.core.JsonParseException(jsonParser, "Could not parse instrument label."));
            String iri = java.util.Optional.ofNullable(tree.get("classIri"))
                    .map(tools.jackson.databind.JsonNode::asText)
                    .orElseThrow(() -> new tools.jackson.core.JsonParseException(jsonParser, "Could not parse instrument iri."));
            return new life.qbic.projectmanagement.infrastructure.experiment.measurement.jpa.IpMeasurementJpaRepository.Instrument(label, oboId, iri);
        }
    }

    /**
     * JSON deserializer for {@code Instrument} record from NgsMeasurementJpaRepository.
     */
    private static class NgsInstrumentDeserializer extends tools.jackson.databind.JsonDeserializer<life.qbic.projectmanagement.infrastructure.experiment.measurement.jpa.NgsMeasurementJpaRepository.Instrument> {
        @Override
        public life.qbic.projectmanagement.infrastructure.experiment.measurement.jpa.NgsMeasurementJpaRepository.Instrument deserialize(
                tools.jackson.core.JsonParser jsonParser,
                tools.jackson.databind.DeserializationContext ctxt
        ) throws java.io.IOException, tools.jackson.core.JacksonException {
            tools.jackson.databind.JsonNode tree = jsonParser.readValueAsTree();
            String oboId = java.util.Optional.ofNullable(tree.get("name"))
                    .map(tools.jackson.databind.JsonNode::asText)
                    .map(it -> it.replace("_", ":"))
                    .orElseThrow(() -> new tools.jackson.core.JsonParseException(jsonParser, "Could not parse instrument oboId."));
            String label = java.util.Optional.ofNullable(tree.get("label"))
                    .map(tools.jackson.databind.JsonNode::asText)
                    .orElseThrow(() -> new tools.jackson.core.JsonParseException(jsonParser, "Could not parse instrument label."));
            String iri = java.util.Optional.ofNullable(tree.get("classIri"))
                    .map(tools.jackson.databind.JsonNode::asText)
                    .orElseThrow(() -> new tools.jackson.core.JsonParseException(jsonParser, "Could not parse instrument iri."));
            return new life.qbic.projectmanagement.infrastructure.experiment.measurement.jpa.NgsMeasurementJpaRepository.Instrument(label, oboId, iri);
        }
    }

    /**
     * JSON deserializer for {@code MsDevice} record from PxpMeasurementJpaRepository.
     */
    private static class MsDeviceDeserializer extends tools.jackson.databind.JsonDeserializer<life.qbic.projectmanagement.infrastructure.experiment.measurement.jpa.PxpMeasurementJpaRepository.MsDevice> {
        @Override
        public life.qbic.projectmanagement.infrastructure.experiment.measurement.jpa.PxpMeasurementJpaRepository.MsDevice deserialize(
                tools.jackson.core.JsonParser jsonParser,
                tools.jackson.databind.DeserializationContext ctxt
        ) throws java.io.IOException {
            tools.jackson.databind.JsonNode tree = jsonParser.readValueAsTree();
            String oboId = java.util.Optional.ofNullable(tree.get("name"))
                    .map(tools.jackson.databind.JsonNode::asText)
                    .map(it -> it.replace("_", ":"))
                    .orElseThrow(() -> new tools.jackson.core.JsonParseException(jsonParser, "Could not parse msDevice oboId."));
            String label = java.util.Optional.ofNullable(tree.get("label"))
                    .map(tools.jackson.databind.JsonNode::asText)
                    .orElseThrow(() -> new tools.jackson.core.JsonParseException(jsonParser, "Could not parse msDevice label."));
            String iri = java.util.Optional.ofNullable(tree.get("classIri"))
                    .map(tools.jackson.databind.JsonNode::asText)
                    .orElseThrow(() -> new tools.jackson.core.JsonParseException(jsonParser, "Could not parse msDevice iri."));
            return new life.qbic.projectmanagement.infrastructure.experiment.measurement.jpa.PxpMeasurementJpaRepository.MsDevice(label, oboId, iri);
        }
    }
}
