package life.qbic.projectmanagement.infrastructure.experiment.measurement.jpa;

import static life.qbic.projectmanagement.infrastructure.experiment.measurement.jpa.SpecificationFunctions.containsString;
import static life.qbic.projectmanagement.infrastructure.experiment.measurement.jpa.SpecificationFunctions.extractFormattedLocalDate;

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
import java.io.IOException;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
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

  final class NgsMeasurementFilter {

    private final String experimentId;
    private final String searchTerm;
    private final int timeZoneOffsetMillis;

    public NgsMeasurementFilter(@NonNull String experimentId,
        @NonNull String searchTerm,
        int timeZoneOffsetMillis) {
      this.searchTerm = Objects.requireNonNull(searchTerm);
      this.experimentId = Objects.requireNonNull(experimentId);
      this.timeZoneOffsetMillis = timeZoneOffsetMillis;
    }

    public Specification<NgsMeasurementInformation> asSpecification() {
      return matchesExperiment(experimentId)
          .and(containsSearchTerm(searchTerm, timeZoneOffsetMillis));
    }

    private static Specification<NgsMeasurementInformation> matchesExperiment(String experimentId) {
      return (root, query, criteriaBuilder) ->
      {
        if (Objects.isNull(query)) {
          return criteriaBuilder.disjunction();
        }
        query.distinct(true);
        return criteriaBuilder.equal(root.join("sampleInfos").get("experimentId"), experimentId);
      };
    }


    private static Specification<NgsMeasurementInformation> containsSearchTerm(String searchTerm,
        int clientOffsetMillis) {
      if (Objects.isNull(searchTerm) || searchTerm.isEmpty()) {
        return Specification.unrestricted();
      }
      return (root, query, criteriaBuilder) -> {
        if (Objects.isNull(query)) {
          return criteriaBuilder.disjunction();
        }
        query.distinct(true);

        //join for sample related matching
        Join<Object, String> sampleInfos = root.joinList("sampleInfos");
        return
            criteriaBuilder.or(
                containsString(criteriaBuilder, root.get("measurementCode"), searchTerm),
                containsString(criteriaBuilder, root.get("measurementName"), searchTerm),
                containsString(criteriaBuilder, root.get("samplePool"), searchTerm),
                containsString(criteriaBuilder, root.get("facility"), searchTerm),
                containsString(criteriaBuilder, root.get("sequencingRunProtocol"), searchTerm),
                containsString(criteriaBuilder, root.get("sequencingReadType"), searchTerm),
                containsString(criteriaBuilder, root.get("libraryKit"), searchTerm),
                containsString(criteriaBuilder, root.get("flowCell"), searchTerm),
                containsString(criteriaBuilder, root.get("organisation").get("label"), searchTerm),
                containsString(criteriaBuilder, root.get("organisation").get("iri"), searchTerm),
                SpecificationFunctions.containsStringInJson(criteriaBuilder, root.get("instrument"),
                    "$.label", searchTerm),
                containsString(criteriaBuilder,
                    extractFormattedLocalDate(criteriaBuilder, root.get("registeredAt"),
                        clientOffsetMillis, SpecificationFunctions.CUSTOM_DATE_TIME_PATTERN),
                    searchTerm),
                containsString(criteriaBuilder, sampleInfos.get("sampleCode"), searchTerm),
                containsString(criteriaBuilder, sampleInfos.get("sampleLabel"), searchTerm),
                containsString(criteriaBuilder, sampleInfos.get("comment"), searchTerm));
      };
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
