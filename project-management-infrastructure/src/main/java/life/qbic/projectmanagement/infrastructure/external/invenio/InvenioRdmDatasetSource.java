package life.qbic.projectmanagement.infrastructure.external.invenio;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import life.qbic.application.commons.ApplicationException;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.associated_dataset.DatasetSource;
import life.qbic.projectmanagement.application.associated_dataset.InstanceConfig;
import life.qbic.projectmanagement.application.associated_dataset.SearchHit;
import life.qbic.projectmanagement.application.associated_dataset.SearchQuery;
import life.qbic.projectmanagement.application.associated_dataset.SearchResult;
import life.qbic.projectmanagement.domain.model.associated_dataset.AccessLevel;
import life.qbic.projectmanagement.domain.model.associated_dataset.InvenioRdmAccessStatus;
import life.qbic.projectmanagement.domain.model.associated_dataset.InvenioRdmResourceMetadata;
import life.qbic.projectmanagement.domain.model.associated_dataset.ResourceMetadata;

/**
 * Infrastructure adapter implementing the {@link DatasetSource} port for
 * InvenioRDM-based repositories (e.g. Zenodo, FDAT).
 *
 * <p>Maps request/response structures between the Invenio REST API
 * (see {@link InvenioRdmClient} for the reference to the OpenAPI
 * specification) and the application-layer DTOs / domain types.</p>
 *
 * <p>Per ADR-0002 P2, this adapter is stateless — there is no session with
 * the external service. Each call carries everything it needs: the
 * {@link InstanceConfig} identifies the target instance, and the
 * {@code actingUserId} identifies the user invoking the call.</p>
 *
 * <p>For this iteration (FEAT-DATSET-01, public datasets only), the
 * {@code actingUserId} parameter is accepted but not used — searches
 * and resolves happen anonymously. The per-user token resolution
 * required for restricted datasets will be implemented in a follow-up
 * story (FEAT-DATSET-14). The parameter is kept now so the port
 * contract is stable across iterations.</p>
 *
 * @since 1.12.0
 */
public class InvenioRdmDatasetSource implements DatasetSource {

  private static final Logger log = logger(InvenioRdmDatasetSource.class);

  private final InvenioRdmClient client;

  public InvenioRdmDatasetSource(InvenioRdmClient client) {
    this.client = Objects.requireNonNull(client, "client must not be null");
  }

  // ── Port implementation ─────────────────────────────────────────────

  @Override
  public SearchResult search(SearchQuery query, InstanceConfig config, String actingUserId) {
    Objects.requireNonNull(query, "query must not be null");
    Objects.requireNonNull(config, "config must not be null");

    // InvenioRDM pages are 1-indexed
    int invenioPage = query.page() + 1;
    var params = new InvenioRdmClient.SearchParams(
        query.effectiveQuery(), invenioPage, query.pageSize());

    try {
      var response = client.search(config.baseUrl(), params);
      List<SearchHit> hits = mapSearchHits(response, config.displayName());
      return new SearchResult(hits, response.hits.total, query.page(), query.pageSize());
    } catch (ApplicationException e) {
      log.error("Search failed on %s for query '%s'"
          .formatted(config.displayName(), query.effectiveQuery()));
      throw e;
    }
  }

  @Override
  public Optional<ResourceMetadata> resolveMetadata(
      String externalHandleValue, InstanceConfig config, String actingUserId) {
    Objects.requireNonNull(externalHandleValue, "externalHandleValue must not be null");
    Objects.requireNonNull(config, "config must not be null");

    try {
      var record = client.getRecord(config.baseUrl(), externalHandleValue);
      InvenioRdmResourceMetadata metadata = mapRecordToResourceMetadata(record, config.displayName());
      return Optional.of(metadata);
    } catch (ApplicationException e) {
      // A "not found" surfaces as a non-transient 4xx — the application
      // service treats this as Optional.empty() downstream
      if (e.getMessage() != null && e.getMessage().contains("status 404")) {
        return Optional.empty();
      }
      log.error("Failed to resolve record %s on %s"
          .formatted(externalHandleValue, config.displayName()));
      throw e;
    }
  }

  // ── Mapping: InvenioRDM JSON → domain / DTO ─────────────────────────

  private List<SearchHit> mapSearchHits(
      InvenioRdmClient.SearchResultResponse response, String resourceProvider) {
    List<SearchHit> hits = new ArrayList<>();
    if (response.hits == null || response.hits.hits == null) {
      return hits;
    }
    for (InvenioRdmClient.Hit h : response.hits.hits) {
      try {
        hits.add(mapHit(h, resourceProvider));
      } catch (Exception e) {
        log.warn("Skipping malformed search hit (id=%s): %s"
            .formatted(h.id, e.getMessage()));
      }
    }
    return hits;
  }

  private SearchHit mapHit(InvenioRdmClient.Hit h, String resourceProvider) {
    String title = safeTitle(h);
    String pid = safePid(h);
    LocalDate publicationDate = safePublicationDate(h);
    String description = description(h);
    String resourceType = resourceType(h);
    List<String> creators = creators(h);
    String version = version(h);
    String accessLink = selfHtmlLink(h);

    // Zenodo uses access_right ("open"/"restricted"/"embargoed") in legacy API
    AccessLevel accessLevel = mapAccessRightToLevel(accessRight(h));
    String accessDetail = "Record: " + mapAccessRightLabel(accessRight(h))
        + " | Files: " + filesAccessLabel(accessRight(h));

    return new SearchHit(
        String.valueOf(h.id),     // external handle: Zenodo record ID
        title,
        pid,
        version,
        publicationDate,
        resourceProvider,
        description,
        accessLevel,
        accessDetail
    );
  }

  private InvenioRdmResourceMetadata mapRecordToResourceMetadata(
      InvenioRdmClient.RecordResponse rec, String resourceProvider) {
    String title = safeRecordTitle(rec);
    String pid = safeRecordPid(rec);
    LocalDate publicationDate = safeRecordPublicationDate(rec);
    String description = recordDescription(rec);
    String resourceType = recordResourceType(rec);
    List<String> creators = recordCreators(rec);
    String version = recordVersion(rec);
    String accessLink = selfHtmlLink(rec);

    // Detailed access mapping from record access block. The current
    // InvenioRDM spec (12.0.0) exposes `access.record`, `access.files`,
    // and `access.status`. Some older / Zenodo instances may only
    // surface the legacy `metadata.access_right` field. When the newer
    // form is missing we fall back and log a warn so the drift is
    // visible in server logs — a signal that this instance may need
    // its own adapter at some point.
    var access = rec.metadata != null ? rec.metadata.access : null;
    InvenioRdmAccessStatus recordAccess;
    InvenioRdmAccessStatus fileAccess;
    if (access != null && (access.status != null || access.record != null)) {
      recordAccess = recordAccessStatus(access);
      fileAccess = fileAccessStatus(access);
    } else {
      recordAccess = mapLegacyAccessRight(rec.metadata != null ? rec.metadata.accessRight : null);
      fileAccess = recordAccess; // legacy field collapses record + files
      log.warn("Record %s on %s uses legacy 'metadata.access_right' field; "
          + "'access.status/record/files' not present. API drift likely.".formatted(pid, resourceProvider));
    }
    LocalDate embargoUntil = embargoUntil(access);

    return new InvenioRdmResourceMetadata(
        title, pid, version, accessLink,
        resourceProvider, creators, resourceType,
        /* community */ null,   // Not exposed by Zenodo legacy search/record API
        publicationDate, description,
        recordAccess, fileAccess, embargoUntil
    );
  }

  /**
   * Maps the Zenodo-legacy {@code metadata.access_right} field
   * (one of: {@code open}, {@code restricted}, {@code embargoed}) to
   * the three-state {@link InvenioRdmAccessStatus}. Falls back to
   * {@link InvenioRdmAccessStatus#PUBLIC} if the field is missing or
   * unrecognised.
   */
  private static InvenioRdmAccessStatus mapLegacyAccessRight(String accessRight) {
    if (accessRight == null) return InvenioRdmAccessStatus.PUBLIC;
    return switch (accessRight.toLowerCase()) {
      case "open" -> InvenioRdmAccessStatus.PUBLIC;
      case "restricted" -> InvenioRdmAccessStatus.RESTRICTED;
      case "embargoed" -> InvenioRdmAccessStatus.EMBARGOED;
      default -> InvenioRdmAccessStatus.RESTRICTED;
    };
  }

  // ── Access mapping (InvenioRDM / Zenodo) ────────────────────────────

  private static String accessRight(InvenioRdmClient.Hit h) {
    return h.metadata != null && h.metadata.accessRight != null
        ? h.metadata.accessRight : "open";
  }

  /**
   * Map a Zenodo access_right string to coarse {@link AccessLevel}.
   * Per the InvenioRDM two-dimension model, this treats record + file
   * access as coupled — Zenodo's legacy API does not separate them.
   */
  private static AccessLevel mapAccessRightToLevel(String accessRight) {
    return "open".equalsIgnoreCase(accessRight)
        ? AccessLevel.PUBLIC : AccessLevel.RESTRICTED;
  }

  private static String mapAccessRightLabel(String accessRight) {
    if (accessRight == null) return "unknown";
    return switch (accessRight.toLowerCase()) {
      case "open" -> "public";
      case "restricted" -> "restricted";
      case "embargoed" -> "embargoed";
      default -> accessRight;
    };
  }

  private static String filesAccessLabel(String accessRight) {
    // Zenodo legacy API: files follow record access_rights
    return mapAccessRightLabel(accessRight);
  }

  /**
   * Map explicit record-access status. The newer InvenioRDM detail
   * response carries {@code access.record}, {@code access.files}, and
   * {@code access.status} (which is one of public/restricted/embargoed).
   */
  /**
   * Maps {@code access.record} (or {@code access.status} if unset).
   * Caller ensures {@code access != null} before invoking.
   */
  private InvenioRdmAccessStatus recordAccessStatus(InvenioRdmClient.RecordAccess access) {
    String rec = access.record;
    if (rec == null) rec = access.status;
    return mapAccessStatus(rec);
  }

  /**
   * Maps {@code access.files} (or {@code access.status} if unset).
   * Caller ensures {@code access != null} before invoking.
   */
  private InvenioRdmAccessStatus fileAccessStatus(InvenioRdmClient.RecordAccess access) {
    String files = access.files;
    if (files == null) files = access.status;
    return mapAccessStatus(files);
  }

  private InvenioRdmAccessStatus mapAccessStatus(String value) {
    if (value == null || value.isBlank()) return InvenioRdmAccessStatus.PUBLIC;
    return switch (value.toLowerCase()) {
      case "public" -> InvenioRdmAccessStatus.PUBLIC;
      case "restricted" -> InvenioRdmAccessStatus.RESTRICTED;
      case "embargoed" -> InvenioRdmAccessStatus.EMBARGOED;
      default -> InvenioRdmAccessStatus.RESTRICTED;
    };
  }

  private LocalDate embargoUntil(InvenioRdmClient.RecordAccess access) {
    if (access == null || access.embargo == null) return null;
    if (!access.embargo.active) return null;
    if (access.embargo.until == null) return null;
    try {
      return LocalDate.parse(access.embargo.until);
    } catch (DateTimeParseException e) {
      log.warn("Unparseable embargo 'until' date: " + access.embargo.until);
      return null;
    }
  }

  // ── Safe accessors (null-tolerant) ───────────────────────────────────

  private String safeTitle(InvenioRdmClient.Hit h) {
    if (h.title != null && !h.title.isBlank()) return h.title;
    if (h.metadata != null && h.metadata.title != null && !h.metadata.title.isBlank()) {
      return h.metadata.title;
    }
    return "(untitled record)";
  }

  private String safeRecordTitle(InvenioRdmClient.RecordResponse rec) {
    if (rec.metadata != null && rec.metadata.title != null && !rec.metadata.title.isBlank()) {
      return rec.metadata.title;
    }
    return "(untitled record)";
  }

  private String safePid(InvenioRdmClient.Hit h) {
    if (h.doi != null && !h.doi.isBlank()) return h.doi;
    if (h.metadata != null && h.metadata.doi != null && !h.metadata.doi.isBlank()) {
      return h.metadata.doi;
    }
    return String.valueOf(h.id);
  }

  private String safeRecordPid(InvenioRdmClient.RecordResponse rec) {
    if (rec.doi != null && !rec.doi.isBlank()) return rec.doi;
    if (rec.metadata != null && rec.metadata.doi != null && !rec.metadata.doi.isBlank()) {
      return rec.metadata.doi;
    }
    return String.valueOf(rec.id);
  }

  private LocalDate safePublicationDate(InvenioRdmClient.Hit h) {
    String raw = h.metadata != null ? h.metadata.publicationDate : null;
    return parseDateOrToday(raw);
  }

  private LocalDate safeRecordPublicationDate(InvenioRdmClient.RecordResponse rec) {
    String raw = rec.metadata != null ? rec.metadata.publicationDate : null;
    return parseDateOrToday(raw);
  }

  private LocalDate parseDateOrToday(String raw) {
    if (raw == null || raw.isBlank()) return LocalDate.now();
    try {
      return LocalDate.parse(raw);
    } catch (DateTimeParseException e) {
      return LocalDate.now();
    }
  }

  private String description(InvenioRdmClient.Hit h) {
    return h.metadata != null ? h.metadata.description : null;
  }

  private String recordDescription(InvenioRdmClient.RecordResponse rec) {
    return rec.metadata != null ? rec.metadata.description : null;
  }

  private String resourceType(InvenioRdmClient.Hit h) {
    if (h.metadata != null && h.metadata.resourceType != null
        && h.metadata.resourceType.title != null) {
      return h.metadata.resourceType.title;
    }
    return null;
  }

  private String recordResourceType(InvenioRdmClient.RecordResponse rec) {
    if (rec.metadata != null && rec.metadata.resourceType != null
        && rec.metadata.resourceType.title != null) {
      return rec.metadata.resourceType.title;
    }
    return null;
  }

  private List<String> creators(InvenioRdmClient.Hit h) {
    List<InvenioRdmClient.Creator> raw = h.metadata != null && h.metadata.creators != null
        ? h.metadata.creators : List.of();
    return raw.stream()
        .filter(c -> c != null && c.name != null && !c.name.isBlank())
        .map(c -> c.name)
        .toList();
  }

  private List<String> recordCreators(InvenioRdmClient.RecordResponse rec) {
    List<InvenioRdmClient.Creator> raw = rec.metadata != null && rec.metadata.creators != null
        ? rec.metadata.creators : List.of();
    return raw.stream()
        .filter(c -> c != null && c.name != null && !c.name.isBlank())
        .map(c -> c.name)
        .toList();
  }

  private String version(InvenioRdmClient.Hit h) {
    // Zenodo: version is often in metadata.version on newer instances,
    // or derived from relations.version for versioned records.
    if (h.metadata != null && h.metadata.version != null && !h.metadata.version.isBlank()) {
      return h.metadata.version;
    }
    if (h.metadata != null && h.metadata.relations != null
        && h.metadata.relations.version != null) {
      for (var relation : h.metadata.relations.version) {
        if (relation != null && relation.parent != null && relation.parent.pidValue != null) {
          // Treat the parent relation's pid_value as the version handle
          // (concept DOI / recid). We expose it as "v" + index if available.
          return "v" + relation.index;
        }
      }
    }
    return null;
  }

  private String recordVersion(InvenioRdmClient.RecordResponse rec) {
    if (rec.metadata != null && rec.metadata.version != null && !rec.metadata.version.isBlank()) {
      return rec.metadata.version;
    }
    return null;
  }

  private String selfHtmlLink(InvenioRdmClient.Hit h) {
    return h.links != null ? h.links.selfHtml : null;
  }

  private String selfHtmlLink(InvenioRdmClient.RecordResponse rec) {
    return rec.links != null ? rec.links.selfHtml : null;
  }

}
