package life.qbic.projectmanagement.infrastructure.experiment.measurement.jpa;

import static life.qbic.projectmanagement.infrastructure.jpa.SpecificationFactory.CUSTOM_DATE_TIME_PATTERN;
import static life.qbic.projectmanagement.infrastructure.jpa.SpecificationFactory.contains;
import static life.qbic.projectmanagement.infrastructure.jpa.SpecificationFactory.distinct;
import static life.qbic.projectmanagement.infrastructure.jpa.SpecificationFactory.formattedClientTimeContains;
import static life.qbic.projectmanagement.infrastructure.jpa.SpecificationFactory.jsonContains;
import static life.qbic.projectmanagement.infrastructure.jpa.SpecificationFactory.propertyContains;

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
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;
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
import life.qbic.projectmanagement.application.measurement.NgsMeasurementLookup.NgsSortKey;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementId;
import life.qbic.projectmanagement.infrastructure.PreventAnyUpdateEntityListener;
import life.qbic.projectmanagement.infrastructure.experiment.measurement.jpa.NgsMeasurementJpaRepository.Instrument.InstrumentReadConverter;
import life.qbic.projectmanagement.infrastructure.experiment.measurement.jpa.NgsMeasurementJpaRepository.NgsMeasurementInformation;
import org.hibernate.collection.spi.PersistentBag;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

@Repository
public interface NgsMeasurementJpaRepository extends
    PagingAndSortingRepository<NgsMeasurementInformation, MeasurementId>,
    JpaSpecificationExecutor<NgsMeasurementInformation> {

  class NgsMeasurementFilter implements
      MeasurementFilter<NgsMeasurementInformation, NgsMeasurementFilter> {


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
    private final List<SampleFilter> sampleFilters = new ArrayList<>();

    private NgsMeasurementFilter(String experimentId,
        @NonNull String searchTerm,
        int timeZoneOffsetMillis) {
      this.searchTerm = Objects.requireNonNull(searchTerm);
      this.experimentId = experimentId;
      this.timeZoneOffsetMillis = timeZoneOffsetMillis;
    }

    public static NgsMeasurementFilter forExperiment(String experimentId) {
      return new NgsMeasurementFilter(experimentId, "", 0);
    }

    public static NgsMeasurementFilter withoutExperiment() {
      return new NgsMeasurementFilter(null, "", 0);
    }

    @Override
    public Optional<String> getExperimentId() {
      return Optional.ofNullable(experimentId);
    }

    @Override
    public NgsMeasurementFilter anyContaining(String searchTerm) {
      this.searchTerm = Optional.ofNullable(searchTerm).orElse("");
      return this;
    }

    @Override
    public NgsMeasurementFilter atClientTimeOffset(int clientTimeZoneOffsetMillis) {
      this.timeZoneOffsetMillis = clientTimeZoneOffsetMillis;
      return this;
    }


    @Override
    public NgsMeasurementFilter includingSamples(Set<String> sampleIds) {
      sampleFilters.add(SampleFilter.including(sampleIds));
      return this;
    }

    @Override
    public NgsMeasurementFilter excludingSamples(Set<String> sampleIds) {
      sampleFilters.add(SampleFilter.excluding(sampleIds));
      return this;
    }

    protected static Join<NgsMeasurementInformation, NgsSampleInfo> getSampleInfos(
        Root<NgsMeasurementInformation> root) {
      return root.join("sampleInfos");
    }

    public Specification<NgsMeasurementInformation> asSpecification() {
      return distinct(matchesExperiment()
          .and(measuresSamples())
          .and(containsSearchTerm()));
    }

    private @NonNull Specification<NgsMeasurementInformation> measuresSamples() {
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
          getSampleInfos(root).get("sampleId").in(includedSampleIds),
          getSampleInfos(root).get("sampleId").in(excludedSampleIds).not());
    }

    private @NonNull Specification<NgsMeasurementInformation> containsSearchTerm() {
      return Specification.anyOf(
          propertyContains("measurementCode", searchTerm),
          propertyContains("measurementName", searchTerm),
          propertyContains("samplePool", searchTerm),
          propertyContains("facility", searchTerm),
          propertyContains("measurementCode", searchTerm),
          propertyContains("sequencingRunProtocol", searchTerm),
          propertyContains("sequencingReadType", searchTerm),
          propertyContains("libraryKit", searchTerm),
          propertyContains("flowCell", searchTerm),
          contains(root -> root.get("organisation").get("label").as(String.class), searchTerm),
          contains(root -> root.get("organisation").get("iri").as(String.class), searchTerm),
          jsonContains(root -> root.get("instrument"), "$.label", searchTerm),
          formattedClientTimeContains("registeredAt", searchTerm, timeZoneOffsetMillis,
              CUSTOM_DATE_TIME_PATTERN),
          contains(root -> getSampleInfos(root)
              .get("sampleLabel").as(String.class), searchTerm),
          contains(root -> getSampleInfos(root)
              .get("comment").as(String.class), searchTerm));
    }


    private Specification<NgsMeasurementInformation> matchesExperiment() {
      if (getExperimentId().isEmpty()) {
        return Specification.unrestricted();
      }
      return (root, query, criteriaBuilder) ->
          criteriaBuilder.equal(getSampleInfos(root).get("experimentId"), experimentId);
    }

    @Override
    public String toString() {
      return new StringJoiner(", ", NgsMeasurementFilter.class.getSimpleName() + "[", "]")
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
            .map(JsonNode::asText) //e.g. EFO_0008633
            .map(it -> it.replace("_", ":")) //e.g. EFO:0008633
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

  @Entity
  @Table(name = "specific_measurement_metadata_ngs")
  @SecondaryTable(name = "sample", pkJoinColumns = @PrimaryKeyJoinColumn(
      name = "sample_id", referencedColumnName = "sample_id"
  ))
  final class NgsSampleInfo {

    @Id
    @Column(name = "sample_id")
    private String sampleId;
    @ManyToOne
    @JoinColumn(name = "measurement_id")
    private NgsMeasurementInformation measurement;

    @Column(table = "sample", name = "experiment_id")
    private String experimentId;
    @Column(table = "sample", name = "code")
    private String sampleCode;
    @Column(table = "sample", name = "label")
    private String sampleLabel;
    @Column(name = "indexI5")
    private String indexI5;
    @Column(name = "indexI7")
    private String indexI7;
    @Column(name = "comment")
    private String comment;

    protected NgsSampleInfo() {

    }

    public String sampleId() {
      return sampleId;
    }

    public String sampleCode() {
      return sampleCode;
    }

    public String sampleLabel() {
      return sampleLabel;
    }

    public String comment() {
      return comment;
    }

    public String indexI5() {
      return indexI5;
    }

    public String indexI7() {
      return indexI7;
    }

    String experimentId() {
      return experimentId;
    }

    @Override
    public String toString() {
      return new StringJoiner(", ", NgsSampleInfo.class.getSimpleName() + "[", "]")
          .add("sampleId='" + sampleId + "'")
          .add("measurement=" + measurement.measurementId)
          .add("sampleCode='" + sampleCode + "'")
          .add("sampleLabel='" + sampleLabel + "'")
          .add("comment='" + comment + "'")
          .toString();
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof NgsSampleInfo that)) {
        return false;
      }

      return Objects.equals(sampleId, that.sampleId) && Objects.equals(measurement.measurementId,
          that.measurement.measurementId) && Objects.equals(experimentId, that.experimentId)
          && Objects.equals(sampleCode, that.sampleCode) && Objects.equals(
          sampleLabel, that.sampleLabel) && Objects.equals(comment, that.comment);
    }

    @Override
    public int hashCode() {
      int result = Objects.hashCode(sampleId);
      result = 31 * result + Objects.hashCode(measurement.measurementId);
      result = 31 * result + Objects.hashCode(experimentId);
      result = 31 * result + Objects.hashCode(sampleCode);
      result = 31 * result + Objects.hashCode(sampleLabel);
      result = 31 * result + Objects.hashCode(comment);
      return result;
    }
  }


  @Entity(name = "ngs_measurement_metadata")
  @Table(name = "ngs_measurements")
  @EntityListeners(PreventAnyUpdateEntityListener.class)
  final class NgsMeasurementInformation {


    @Id
    @Column(name = "measurement_id")
    private String measurementId;

    @Column(name = "projectId", nullable = false)
    private String projectId;

    /**
     * Sortable by using {@link NgsSortKey#MEASUREMENT_ID}
     */
    @Column(name = "measurementCode")
    private String measurementCode;

    /**
     * Sortable by using {@link NgsSortKey#MEASUREMENT_NAME}
     */
    @Column(name = "measurementName")
    private String measurementName;

    /**
     * Sortable by using {@link NgsSortKey#FACILITY}
     */
    @Column(name = "facility")
    private String facility;

    @Embedded
    @AttributeOverride(name = "label", column = @Column(name = "label"))
    @AttributeOverride(name = "iri", column = @Column(name = "IRI"))
    private Organisation organisation;

    @Column(name = "instrument")
    @Convert(converter = InstrumentReadConverter.class)
    private Instrument instrument;

    /**
     * Sortable by using {@link NgsSortKey#SAMPLE_POOL}
     */
    @Column(name = "samplePool")
    private String samplePool;

    /**
     * Sortable by using {@link NgsSortKey#REGISTRATION_DATE}
     */
    @Column(name = "registrationTime")
    private Instant registeredAt;

    /**
     * Sortable by using {@link NgsSortKey#READ_TYPE}
     */
    @Column(name = "readType")
    private String sequencingReadType;

    /**
     * Sortable by using {@link NgsSortKey#LIBRARY_KIT}
     */
    @Column(name = "libraryKit")
    private String libraryKit;

    /**
     * Sortable by using {@link NgsSortKey#FLOW_CELL}
     */
    @Column(name = "flowcell")
    private String flowCell;

    /**
     * Sortable by using {@link NgsSortKey#RUN_PROTOCOL}
     */
    @Column(name = "runProtocol")
    private String sequencingRunProtocol;

    /**
     * Attention: During Hibernate session, this is a {@link PersistentBag}. The equals method of
     * {@link PersistentBag#equals(Object)} does not respect the {@link List#equals(Object)}
     * contract.
     */
    @OneToMany(mappedBy = "measurement", fetch = FetchType.EAGER)
    private List<NgsSampleInfo> sampleInfos;

    protected NgsMeasurementInformation() {

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

    public String sequencingReadType() {
      return sequencingReadType;
    }

    public String libraryKit() {
      return libraryKit;
    }

    public String flowCell() {
      return flowCell;
    }

    public String sequencingRunProtocol() {
      return sequencingRunProtocol;
    }

    public List<NgsSampleInfo> sampleInfos() {
      return sampleInfos.stream().toList();
    }

    @Override
    public String toString() {
      return new StringJoiner(", ", NgsMeasurementInformation.class.getSimpleName() + "[",
          "]")
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
          .add("sequencingReadType='" + sequencingReadType + "'")
          .add("libraryKit='" + libraryKit + "'")
          .add("flowCell='" + flowCell + "'")
          .add("sequencingRunProtocol='" + sequencingRunProtocol + "'")
          .add("sampleInfos=" + sampleInfos)
          .toString();
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof NgsMeasurementInformation that)) {
        return false;
      }

      return Objects.equals(measurementId, that.measurementId) && Objects.equals(
          projectId, that.projectId) && Objects.equals(measurementCode, that.measurementCode)
          && Objects.equals(measurementName, that.measurementName)
          && Objects.equals(facility, that.facility) && Objects.equals(organisation,
          that.organisation) && Objects.equals(instrument, that.instrument)
          && Objects.equals(samplePool, that.samplePool) && Objects.equals(
          registeredAt, that.registeredAt) && Objects.equals(sequencingReadType,
          that.sequencingReadType) && Objects.equals(libraryKit, that.libraryKit)
          && Objects.equals(flowCell, that.flowCell) && Objects.equals(
          sequencingRunProtocol, that.sequencingRunProtocol)
          //take care of breaking interface method in persistance bag
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
      result = 31 * result + Objects.hashCode(sequencingReadType);
      result = 31 * result + Objects.hashCode(libraryKit);
      result = 31 * result + Objects.hashCode(flowCell);
      result = 31 * result + Objects.hashCode(sequencingRunProtocol);
      //take care of breaking interface method in persistance bag
      result = 31 * result + Objects.hashCode(sampleInfos.stream().toList());
      return result;
    }
  }


}
