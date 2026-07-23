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
 * InvenioRDM v12-based repositories.
 *
 * <p>With the {@code Accept: application/vnd.inveniordm.v1+json} request
 * header (set by {@link InvenioRdmClient}), both Zenodo and FDAT return
 * identical InvenioRDM v12 record shapes from {@code GET /api/records}
 * and {@code GET /api/records/{id}}. This adapter models that single
 * format; no version negotiation or response shimming is needed.</p>
 *
 * <p>Per ADR-0002 P2, this adapter is stateless — there is no session
 * with the external service.</p>
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
  public SearchResult search(SearchQuery query, InstanceConfig config,
      String actingUserId) {
    Objects.requireNonNull(query, "query must not be null");
    Objects.requireNonNull(config, "config must not be null");

    int invenioPage = query.page() + 1;
    var params = new InvenioRdmClient.SearchParams(
        query.effectiveQuery(), invenioPage, query.pageSize());

    try {
      var response = client.search(config.baseUrl(), params);
      List<SearchHit> hits = mapSearchHits(response, config.displayName());
      return new SearchResult(hits, response.hits.total, query.page(),
          query.pageSize());
    } catch (ApplicationException e) {
      log.error("Search failed on %s for query '%s'"
          .formatted(config.displayName(), query.effectiveQuery()));
      throw e;
    }
  }

  @Override
  public Optional<ResourceMetadata> resolveMetadata(
      String externalHandleValue, InstanceConfig config,
      String actingUserId) {
    Objects.requireNonNull(externalHandleValue,
        "externalHandleValue must not be null");
    Objects.requireNonNull(config, "config must not be null");

    try {
      var record = client.getRecord(config.baseUrl(), externalHandleValue);
      InvenioRdmResourceMetadata metadata = mapRecordToResourceMetadata(
          record, config.displayName());
      return Optional.of(metadata);
    } catch (ApplicationException e) {
      if (e.getMessage() != null && e.getMessage().contains("status 404")) {
        return Optional.empty();
      }
      log.error("Failed to resolve record %s on %s"
          .formatted(externalHandleValue, config.displayName()));
      throw e;
    }
  }

  // ── Mapping: v12 response → DTO ─────────────────────────────────────

  private List<SearchHit> mapSearchHits(
      InvenioRdmClient.SearchResultResponse response,
      String resourceProvider) {
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
    String title = safeHitTitle(h);
    String pid = safePid(h.pids, h.id);
    LocalDate publicationDate = parseDateOrToday(
        h.metadata != null ? h.metadata.publicationDate : null);
    String description = h.metadata != null ? h.metadata.description : null;
    String resourceType = resolvedResourceType(
        h.metadata != null ? h.metadata.resourceType : null);
    List<String> creators = resolvedCreators(
        h.metadata != null ? h.metadata.creators : null);
    String version = versionString(h.versions);

    // v12 access block lives at the response top level.
    AccessLevel accessLevel = accessLevel(h.access);
    String accessDetail = accessDetail(h.access);

    return new SearchHit(
        String.valueOf(h.id),
        title, pid, version, publicationDate, resourceProvider,
        description, accessLevel == AccessLevel.PUBLIC, accessDetail
    );
  }

  private InvenioRdmResourceMetadata mapRecordToResourceMetadata(
      InvenioRdmClient.RecordResponse rec, String resourceProvider) {
    String title = safeRecordTitle(rec);
    String pid = safePid(rec.pids, rec.id);
    LocalDate publicationDate = parseDateOrToday(
        rec.metadata != null ? rec.metadata.publicationDate : null);
    String description = rec.metadata != null
        ? rec.metadata.description : null;
    String resourceType = resolvedResourceType(
        rec.metadata != null ? rec.metadata.resourceType : null);
    List<String> creators = resolvedCreators(
        rec.metadata != null ? rec.metadata.creators : null);
    String version = versionString(rec.versions);
    String accessLink = selfHtmlLink(rec.links);
    String community = community(rec.parent);

    // v12 access block lives at the response top level.
    InvenioRdmAccessStatus recordAccess = recordAccessStatus(rec.access);
    InvenioRdmAccessStatus fileAccess = fileAccessStatus(rec.access);

    return new InvenioRdmResourceMetadata(
        title, pid, version, accessLink,
        resourceProvider, creators, resourceType,
        community,
        publicationDate, description,
        recordAccess, fileAccess
    );
  }

  // ── Field extractors (package-private for testing) ──────────────────

  /**
   * Extracts the primary community display label from a v12 record's
   * {@code parent.communities.entries} list. Returns the first entry's
   * human-readable label, or null when the record belongs to no
   * community.
   */
  static String community(InvenioRdmClient.Parent parent) {
    if (parent == null || parent.communities == null
        || parent.communities.entries == null
        || parent.communities.entries.isEmpty()) {
      return null;
    }
    InvenioRdmClient.Community first =
        parent.communities.entries.getFirst();
    return first != null ? first.displayLabel() : null;
  }

  /**
   * v12 access status: {@code access.status} of {@code "open"} is
   * PUBLIC, everything else is RESTRICTED.
   */
  static AccessLevel accessLevel(InvenioRdmClient.RecordAccess access) {
    if (access == null || access.status == null
        || access.status.isBlank()) {
      return AccessLevel.RESTRICTED;
    }
    return "open".equalsIgnoreCase(access.status)
        ? AccessLevel.PUBLIC : AccessLevel.RESTRICTED;
  }

  static String accessDetail(InvenioRdmClient.RecordAccess access) {
    String recordLabel = access != null && access.record != null
        ? access.record.toLowerCase() : "unknown";
    String filesLabel = access != null && access.files != null
        ? access.files.toLowerCase() : "unknown";
    return "Record: " + recordLabel + " | Files: " + filesLabel;
  }

  static InvenioRdmAccessStatus recordAccessStatus(
      InvenioRdmClient.RecordAccess access) {
    if (access == null) return InvenioRdmAccessStatus.PUBLIC;
    String rec = access.record != null ? access.record : access.status;
    return mapAccessStatus(rec);
  }

  static InvenioRdmAccessStatus fileAccessStatus(
      InvenioRdmClient.RecordAccess access) {
    if (access == null) return InvenioRdmAccessStatus.PUBLIC;
    String files = access.files != null ? access.files : access.status;
    return mapAccessStatus(files);
  }

  static InvenioRdmAccessStatus mapAccessStatus(String value) {
    if (value == null || value.isBlank()) {
      return InvenioRdmAccessStatus.PUBLIC;
    }
    return switch (value.toLowerCase()) {
      case "public", "open" -> InvenioRdmAccessStatus.PUBLIC;
      case "restricted" -> InvenioRdmAccessStatus.RESTRICTED;
      case "embargoed" -> InvenioRdmAccessStatus.EMBARGOED;
      default -> InvenioRdmAccessStatus.RESTRICTED;
    };
  }

  /**
   * v12 PID extraction: DOI lives at {@code pids.doi.identifier}.
   * Falls back to the record {@code id} when no DOI is present.
   */
  static String safePid(InvenioRdmClient.Pids pids, String recordId) {
    if (pids != null && pids.doi != null
        && pids.doi.identifier != null
        && !pids.doi.identifier.isBlank()) {
      return pids.doi.identifier;
    }
    return String.valueOf(recordId);
  }

  /**
   * v12 version string: {@code versions.index} is 1-based.
   * Returns "v{index}" when present and positive, null otherwise.
   */
  static String versionString(InvenioRdmClient.RecordVersions versions) {
    if (versions == null) return null;
    return versions.index <= 0 ? null : "v" + versions.index;
  }

  static List<String> resolvedCreators(
      List<InvenioRdmClient.Creator> raw) {
    if (raw == null) return List.of();
    return raw.stream()
        .filter(c -> c != null && c.resolvedName() != null)
        .map(InvenioRdmClient.Creator::resolvedName)
        .toList();
  }

  static String resolvedResourceType(InvenioRdmClient.ResourceType rt) {
    if (rt == null) return null;
    return rt.resolvedTitle();
  }

  static String selfHtmlLink(InvenioRdmClient.HitLinks links) {
    return links != null ? links.selfHtml : null;
  }

  static String safeHitTitle(InvenioRdmClient.Hit h) {
    if (h.metadata != null && h.metadata.title != null
        && !h.metadata.title.isBlank()) {
      return h.metadata.title;
    }
    return "(untitled record)";
  }

  static String safeRecordTitle(InvenioRdmClient.RecordResponse rec) {
    if (rec.metadata != null && rec.metadata.title != null
        && !rec.metadata.title.isBlank()) {
      return rec.metadata.title;
    }
    return "(untitled record)";
  }

  static LocalDate parseDateOrToday(String raw) {
    if (raw == null || raw.isBlank()) return LocalDate.now();
    try {
      return LocalDate.parse(raw);
    } catch (DateTimeParseException e) {
      return LocalDate.now();
    }
  }
}
