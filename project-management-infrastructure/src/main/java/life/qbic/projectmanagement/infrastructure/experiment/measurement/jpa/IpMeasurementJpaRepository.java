package life.qbic.projectmanagement.infrastructure.experiment.measurement.jpa;

import static life.qbic.projectmanagement.infrastructure.jpa.JpaSpecifications.contains;
import static life.qbic.projectmanagement.infrastructure.jpa.JpaSpecifications.distinct;
import static life.qbic.projectmanagement.infrastructure.jpa.JpaSpecifications.formattedClientTimeContains;
import static life.qbic.projectmanagement.infrastructure.jpa.JpaSpecifications.jsonContains;
import static life.qbic.projectmanagement.infrastructure.jpa.JpaSpecifications.propertyContains;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import java.io.IOException;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import life.qbic.application.commons.time.DateTimeFormat;
import life.qbic.projectmanagement.application.measurement.IpMeasurementLookup.IpSortKey;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementId;
import life.qbic.projectmanagement.infrastructure.PreventAnyUpdateEntityListener;
import life.qbic.projectmanagement.infrastructure.experiment.measurement.jpa.IpMeasurementJpaRepository.Instrument.InstrumentReadConverter;
import life.qbic.projectmanagement.infrastructure.experiment.measurement.jpa.IpMeasurementJpaRepository.IpMeasurementInformation;
import org.hibernate.collection.spi.PersistentBag;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

@Repository
public interface IpMeasurementJpaRepository extends
    PagingAndSortingRepository<IpMeasurementInformation, MeasurementId>,
    JpaSpecificationExecutor<IpMeasurementInformation> {

  class IpMeasurementFilter implements
      MeasurementFilter<IpMeasurementInformation, IpMeasurementFilter> {

    private record SampleFilter(boolean isInclusion, Set<String> sampleIds) {

      static SampleFilter including(Set<String> sampleIds) {
        return new SampleFilter(true, new HashSet<>(sampleIds));
      }

      static SampleFilter excluding(Set<String> sampleIds) {
        return new SampleFilter(false, new HashSet<>(sampleIds));
      }

      private SampleFilter {
        Objects.requireNonNull(sampleIds);
      }
    }

    private final String experimentId;
    private String searchTerm;
    private int timeZoneOffsetMillis;
    private DateTimeFormat dateTimeFormat;
    private final List<SampleFilter> sampleFilters = new ArrayList<>();

    private IpMeasurementFilter(String experimentId,
        @NonNull String searchTerm,
        int timeZoneOffsetMillis) {
      this.searchTerm = Objects.requireNonNull(searchTerm);
      this.experimentId = experimentId;
      this.timeZoneOffsetMillis = timeZoneOffsetMillis;
    }

    public static IpMeasurementFilter forExperiment(String experimentId) {
      return new IpMeasurementFilter(experimentId, "", 0);
    }

    public static IpMeasurementFilter withoutExperiment() {
      return new IpMeasurementFilter(null, "", 0);
    }

    @Override
    public Optional<String> getExperimentId() {
      return Optional.ofNullable(experimentId);
    }

    @Override
    public IpMeasurementFilter anyContaining(String searchTerm) {
      this.searchTerm = Optional.ofNullable(searchTerm).orElse("");
      return this;
    }

    @Override
    public IpMeasurementFilter atClientTimeOffset(int clientTimeZoneOffsetMillis,
        DateTimeFormat dateTimeFormat) {
      this.timeZoneOffsetMillis = clientTimeZoneOffsetMillis;
      this.dateTimeFormat = dateTimeFormat;
      return this;
    }

    @Override
    public IpMeasurementFilter includingSamples(Set<String> sampleIds) {
      sampleFilters.add(SampleFilter.including(sampleIds));
      return this;
    }

    @Override
    public IpMeasurementFilter excludingSamples(Set<String> sampleIds) {
      sampleFilters.add(SampleFilter.excluding(sampleIds));
      return this;
    }

    protected static Join<IpMeasurementInformation, IpSampleInfo> getSampleInfos(
        Root<IpMeasurementInformation> root) {
      return root.join("sampleInfos");
    }

    public Specification<IpMeasurementInformation> asSpecification() {
      return distinct(matchesExperiment()
          .and(measuresSamples())
          .and(containsSearchTerm()));
    }

    private @NonNull Specification<IpMeasurementInformation> measuresSamples() {
      if (sampleFilters.isEmpty()) {
        return Specification.unrestricted();
      }
      Set<String> includedSampleIds = sampleFilters.stream()
          .filter(SampleFilter::isInclusion)
          .flatMap(it -> it.sampleIds().stream())
          .collect(Collectors.toSet());
      Set<String> excludedSampleIds = sampleFilters.stream()
          .filter(Predicate.not(SampleFilter::isInclusion))
          .flatMap(it -> it.sampleIds().stream())
          .collect(Collectors.toSet());
      return (root, query, criteriaBuilder) -> criteriaBuilder.and(
          getSampleInfos(root).get("sample").get("sampleId").in(includedSampleIds),
          getSampleInfos(root).get("sample").get("sampleId").in(excludedSampleIds).not());
    }

    private @NonNull Specification<IpMeasurementInformation> containsSearchTerm() {
      if (searchTerm.isBlank()) {
        return Specification.unrestricted();
      }
      return Specification.anyOf(
          propertyContains("measurementCode", searchTerm),
          propertyContains("measurementName", searchTerm),
          propertyContains("samplePool", searchTerm),
          propertyContains("facility", searchTerm),
          propertyContains("mhcAntibody", searchTerm),
          propertyContains("mhcTypingMethod", searchTerm),
          propertyContains("enrichmentMethod", searchTerm),
          propertyContains("lcmsMethod", searchTerm),
          propertyContains("lcColumn", searchTerm),
          propertyContains("dataAcquisition", searchTerm),
          propertyContains("massRange", searchTerm),
          propertyContains("retentionTimeRange", searchTerm),
          propertyContains("chargeRange", searchTerm),
          propertyContains("ionMobilityRange", searchTerm),
          propertyContains("cycleFractionName", searchTerm),
          contains(root -> root.get("organisation").get("label").as(String.class), searchTerm),
          contains(root -> root.get("organisation").get("iri").as(String.class), searchTerm),
          jsonContains(root -> root.get("instrument"), "$.label", searchTerm),
          formattedClientTimeContains("registeredAt", searchTerm, timeZoneOffsetMillis,
              dateTimeFormat),
          contains(root -> getSampleInfos(root)
              .get("sample").get("sampleLabel").as(String.class), searchTerm));
    }

    private Specification<IpMeasurementInformation> matchesExperiment() {
      if (getExperimentId().isEmpty()) {
        return Specification.unrestricted();
      }
      return (root, query, criteriaBuilder) ->
          criteriaBuilder.equal(getSampleInfos(root).get("sample").get("experimentId"),
              experimentId);
    }

    @Override
    public String toString() {
      return new StringJoiner(", ", IpMeasurementFilter.class.getSimpleName() + "[", "]")
          .add("experimentId='" + experimentId + "'")
          .add("searchTerm='" + searchTerm + "'")
          .toString();
    }
  }

  record Instrument(String label, String oboId, String iri) implements Serializable {

    @JsonComponent
    static class InstrumentJsonDeserializer extends JsonDeserializer<Instrument> {

      @Override
      public Instrument deserialize(JsonParser jsonParser, DeserializationContext ctxt)
          throws IOException, JacksonException {
        JsonNode tree = jsonParser.readValueAsTree();
        String oboId = Optional.ofNullable(tree.get("name"))
            .map(JsonNode::asText)
            .map(it -> it.replace("_", ":"))
            .orElseThrow(() -> new JsonParseException("Could not parse instrument oboId."));
        String label = Optional.ofNullable(tree.get("label"))
            .map(JsonNode::asText)
            .orElseThrow(() -> new JsonParseException("Could not parse instrument label."));
        String iri = Optional.ofNullable(tree.get("classIri"))
            .map(JsonNode::asText)
            .orElseThrow(() -> new JsonParseException("Could not parse instrument iri."));
        return new Instrument(label, oboId, iri);
      }
    }

    @ReadingConverter
    static class InstrumentReadConverter implements AttributeConverter<Instrument, String> {

      private final ObjectMapper objectMapper;

      public InstrumentReadConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
      }

      @Override
      public String convertToDatabaseColumn(Instrument attribute) {
        return "";
      }

      @Override
      public Instrument convertToEntityAttribute(String dbData) {
        try {
          return objectMapper.readValue(dbData, Instrument.class);
        } catch (JsonProcessingException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

  record Organisation(String label, String iri) {

  }

  /**
   * Read-only JPA entity representing a sample's participation in an immunopeptidomics
   * measurement.
   *
   * <p>The same sample can appear in multiple measurements, so a {@link MeasuredSampleId
   * composite primary key} is used to allow Hibernate's first-level cache to correctly
   * distinguish instances.
   *
   * <p>Sample metadata is delegated to {@link SampleLookupEntity}.
   */
  @Entity
  @Table(name = "specific_measurement_metadata_ip")
  final class IpSampleInfo {

    @EmbeddedId
    private MeasuredSampleId id;

    @ManyToOne
    @MapsId("measurementId")
    @JoinColumn(name = "measurement_id")
    private IpMeasurementInformation measurement;

    @ManyToOne
    @MapsId("sampleId")
    @JoinColumn(name = "sample_id")
    private SampleLookupEntity sample;

    protected IpSampleInfo() {
    }

    public String sampleId() {
      return id.sampleId();
    }

    public String sampleCode() {
      return sample.sampleCode();
    }

    public String sampleLabel() {
      return sample.sampleLabel();
    }

    String experimentId() {
      return sample.experimentId();
    }

    @Override
    public String toString() {
      return new StringJoiner(", ", IpSampleInfo.class.getSimpleName() + "[", "]")
          .add("sampleId='" + sampleId() + "'")
          .add("measurement=" + measurement.measurementId)
          .add("sampleCode='" + sampleCode() + "'")
          .add("sampleLabel='" + sampleLabel() + "'")
          .toString();
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof IpSampleInfo that)) {
        return false;
      }
      return Objects.equals(sampleId(), that.sampleId()) && Objects.equals(
          measurement.measurementId,
          that.measurement.measurementId) && Objects.equals(sampleCode(), that.sampleCode())
          && Objects.equals(sampleLabel(), that.sampleLabel());
    }

    @Override
    public int hashCode() {
      int result = Objects.hashCode(sampleId());
      result = 31 * result + Objects.hashCode(measurement.measurementId);
      result = 31 * result + Objects.hashCode(sampleCode());
      result = 31 * result + Objects.hashCode(sampleLabel());
      return result;
    }
  }

  @Entity(name = "ip_measurement_metadata")
  @Table(name = "ip_measurements")
  @EntityListeners(PreventAnyUpdateEntityListener.class)
  final class IpMeasurementInformation {

    @Id
    @Column(name = "measurement_id")
    private String measurementId;

    @Column(name = "projectId", nullable = false)
    private String projectId;

    @Column(name = "measurementCode")
    private String measurementCode;

    @Column(name = "measurementName")
    private String measurementName;

    @Column(name = "facility")
    private String facility;

    @Embedded
    @AttributeOverride(name = "label", column = @Column(name = "label"))
    @AttributeOverride(name = "iri", column = @Column(name = "IRI"))
    private Organisation organisation;

    @Column(name = "instrument")
    @Convert(converter = InstrumentReadConverter.class)
    private Instrument instrument;

    @Column(name = "samplePool")
    private String samplePool;

    @Column(name = "registrationTime")
    private Instant registeredAt;

    @Column(name = "mhcAntibody")
    private String mhcAntibody;

    @Column(name = "mhcTypingMethod")
    private String mhcTypingMethod;

    @Column(name = "enrichmentMethod")
    private String enrichmentMethod;

    @Column(name = "lcmsMethod")
    private String lcmsMethod;

    @Column(name = "lcColumn")
    private String lcColumn;

    @Column(name = "dataAcquisition")
    private String dataAcquisition;

    @Column(name = "massRange")
    private String massRange;

    @Column(name = "retentionTimeRange")
    private Integer retentionTimeRange;

    @Column(name = "chargeRange")
    private String chargeRange;

    @Column(name = "ionMobilityRange")
    private String ionMobilityRange;

    @Column(name = "sampleMass")
    private Double sampleMass;

    @Column(name = "sampleVolume")
    private Double sampleVolume;

    @Column(name = "cycleFractionName")
    private String cycleFractionName;

    @Column(name = "prepDate")
    private java.time.LocalDate prepDate;

    @Column(name = "msRunDate")
    private java.time.LocalDate msRunDate;

    @Column(name = "comment")
    private String comment;

    @OneToMany(mappedBy = "measurement", fetch = FetchType.EAGER)
    private List<IpSampleInfo> sampleInfos;

    protected IpMeasurementInformation() {
    }

    public String getExperimentId() {
      return sampleInfos.getFirst().experimentId();
    }

    public String measurementId() {
      return measurementId;
    }

    public String projectId() {
      return projectId;
    }

    public String measurementCode() {
      return measurementCode;
    }

    public String measurementName() {
      return measurementName;
    }

    public String facility() {
      return facility;
    }

    public Organisation organisation() {
      return organisation;
    }

    public Instrument instrument() {
      return instrument;
    }

    public String samplePool() {
      return samplePool;
    }

    public Instant registeredAt() {
      return registeredAt;
    }

    public String mhcAntibody() {
      return mhcAntibody;
    }

    public String mhcTypingMethod() {
      return mhcTypingMethod;
    }

    public String enrichmentMethod() {
      return enrichmentMethod;
    }

    public String lcmsMethod() {
      return lcmsMethod;
    }

    public String lcColumn() {
      return lcColumn;
    }

    public String dataAcquisition() {
      return dataAcquisition;
    }

    public String massRange() {
      return massRange;
    }

    public Integer retentionTimeRange() {
      return retentionTimeRange;
    }

    public String chargeRange() {
      return chargeRange;
    }

    public String ionMobilityRange() {
      return ionMobilityRange;
    }

    public Double sampleMass() {
      return sampleMass;
    }

    public Double sampleVolume() {
      return sampleVolume;
    }

    public String cycleFractionName() {
      return cycleFractionName;
    }

    public java.time.LocalDate prepDate() {
      return prepDate;
    }

    public java.time.LocalDate msRunDate() {
      return msRunDate;
    }

    public String comment() {
      return comment;
    }

    public List<IpSampleInfo> sampleInfos() {
      return sampleInfos.stream().toList();
    }

    @Override
    public String toString() {
      return new StringJoiner(", ", IpMeasurementInformation.class.getSimpleName() + "[", "]")
          .add("experimentID=" + getExperimentId())
          .add("measurementId=" + measurementId)
          .add("projectId='" + projectId + "'")
          .add("measurementCode='" + measurementCode + "'")
          .add("measurementName='" + measurementName + "'")
          .add("facility='" + facility + "'")
          .add("organisation=" + organisation)
          .add("instrument=" + instrument)
          .add("samplePool='" + samplePool + "'")
          .add("registeredAt=" + registeredAt)
          .add("mhcAntibody='" + mhcAntibody + "'")
          .add("enrichmentMethod='" + enrichmentMethod + "'")
          .add("lcmsMethod='" + lcmsMethod + "'")
          .add("lcColumn='" + lcColumn + "'")
          .add("dataAcquisition='" + dataAcquisition + "'")
          .add("massRange='" + massRange + "'")
          .add("chargeRange='" + chargeRange + "'")
          .add("sampleMass=" + sampleMass)
          .add("sampleVolume=" + sampleVolume)
          .add("cycleFractionName='" + cycleFractionName + "'")
          .add("prepDate=" + prepDate)
          .add("msRunDate=" + msRunDate)
          .add("sampleInfos=" + sampleInfos)
          .toString();
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof IpMeasurementInformation that)) {
        return false;
      }
      return Objects.equals(measurementId, that.measurementId) && Objects.equals(
          projectId, that.projectId) && Objects.equals(measurementCode, that.measurementCode)
          && Objects.equals(measurementName, that.measurementName)
          && Objects.equals(facility, that.facility) && Objects.equals(organisation,
          that.organisation) && Objects.equals(instrument, that.instrument)
          && Objects.equals(samplePool, that.samplePool) && Objects.equals(
          registeredAt, that.registeredAt) && Objects.equals(mhcAntibody, that.mhcAntibody)
          && Objects.equals(enrichmentMethod, that.enrichmentMethod) && Objects.equals(
          lcmsMethod, that.lcmsMethod) && Objects.equals(lcColumn, that.lcColumn)
          && Objects.equals(dataAcquisition, that.dataAcquisition) && Objects.equals(
          massRange, that.massRange) && Objects.equals(chargeRange, that.chargeRange)
          && Objects.equals(sampleMass, that.sampleMass)
          && Objects.equals(sampleVolume, that.sampleVolume)
          && Objects.equals(cycleFractionName, that.cycleFractionName)
          && Objects.equals(prepDate, that.prepDate)
          && Objects.equals(msRunDate, that.msRunDate)
          && Objects.equals(sampleInfos.stream().toList(),
          that.sampleInfos.stream().toList());
    }

    @Override
    public int hashCode() {
      int result = Objects.hashCode(measurementId);
      result = 31 * result + Objects.hashCode(projectId);
      result = 31 * result + Objects.hashCode(measurementCode);
      result = 31 * result + Objects.hashCode(measurementName);
      result = 31 * result + Objects.hashCode(facility);
      result = 31 * result + Objects.hashCode(organisation);
      result = 31 * result + Objects.hashCode(instrument);
      result = 31 * result + Objects.hashCode(samplePool);
      result = 31 * result + Objects.hashCode(registeredAt);
      result = 31 * result + Objects.hashCode(mhcAntibody);
      result = 31 * result + Objects.hashCode(enrichmentMethod);
      result = 31 * result + Objects.hashCode(lcmsMethod);
      result = 31 * result + Objects.hashCode(lcColumn);
      result = 31 * result + Objects.hashCode(dataAcquisition);
      result = 31 * result + Objects.hashCode(massRange);
      result = 31 * result + Objects.hashCode(chargeRange);
      result = 31 * result + Objects.hashCode(sampleMass);
      result = 31 * result + Objects.hashCode(sampleVolume);
      result = 31 * result + Objects.hashCode(cycleFractionName);
      result = 31 * result + Objects.hashCode(prepDate);
      result = 31 * result + Objects.hashCode(msRunDate);
      result = 31 * result + Objects.hashCode(sampleInfos.stream().toList());
      return result;
    }
  }
}
