package life.qbic.projectmanagement.infrastructure.external.invenio

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Specification

/**
 * Unit tests for InvenioRDM v12 response JSON parsing.
 *
 * <p>Verifies that the DTOs in {@link InvenioRdmClient} correctly
 * deserialize responses produced by InvenioRDM v12 deployments when
 * {@code Accept: application/vnd.inveniordm.v1+json} is sent. Both
 * FDAT and modern Zenodo serve identical v12 JSON under that header,
 * so the fixtures below are representative of both.</p>
 *
 * @since 1.12.0
 */
class InvenioRdmClientParsingSpec extends Specification {

  private static final ObjectMapper MAPPER = new ObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

  // ── Record detail response parsing ──────────────────────────────────

  def "parses v12 record detail response with community"() {
    given: "a v12 record response (Zenodo/FDAT shape with v12 Accept header)"
    def json = '''
    {
      "id": "abc-123",
      "links": {
        "self_html": "https://fdat.example.org/records/abc-123"
      },
      "created": "2024-06-15T10:00:00.000000+00:00",
      "updated": "2024-07-01T12:00:00.000000+00:00",
      "is_published": true,
      "access": {
        "record": "public",
        "files": "public",
        "embargo": { "active": false, "until": null, "reason": null },
        "status": "open"
      },
      "pids": {
        "doi": {
          "identifier": "10.57754/FDAT.abc-123",
          "provider": "datacite",
          "client": "datacite"
        }
      },
      "versions": { "is_latest": true, "index": 3 },
      "parent": {
        "communities": {
          "default": "73eb987a-a71f-4ed7-950f-a4b68192d85d",
          "ids": ["73eb987a-a71f-4ed7-950f-a4b68192d85d"],
          "entries": [
            {
              "id": "73eb987a-a71f-4ed7-950f-a4b68192d85d",
              "slug": "humanities",
              "metadata": {
                "title": "Faculty of Humanities",
                "type": { "id": "organization" }
              },
              "access": { "visibility": "public" }
            }
          ]
        }
      },
      "metadata": {
        "title": "Test Record With Community",
        "publication_date": "2024-06-15",
        "description": "<p>A test record.</p>",
        "creators": [
          {
            "person_or_org": {
              "type": "personal",
              "name": "Smith, Alice",
              "given_name": "Alice",
              "family_name": "Smith"
            },
            "affiliations": [{ "id": "01xyz", "name": "University A" }]
          },
          {
            "person_or_org": {
              "type": "personal",
              "name": "Jones, Bob",
              "given_name": "Bob",
              "family_name": "Jones"
            },
            "affiliations": []
          }
        ],
        "resource_type": { "id": "dataset", "title": { "en": "Dataset" } }
      }
    }
    '''

    when:
    def rec = MAPPER.readValue(json, InvenioRdmClient.RecordResponse)

    then: "core fields are parsed"
    rec.id == "abc-123"
    rec.isPublished
    rec.links.selfHtml == "https://fdat.example.org/records/abc-123"
    rec.access.status == "open"
    rec.access.invenioRecord == "public"
    rec.access.files == "public"
    rec.pids.doi.identifier == "10.57754/FDAT.abc-123"
    rec.versions.index == 3
    rec.versions.isLatest

    and: "metadata is parsed"
    rec.metadata.title == "Test Record With Community"
    rec.metadata.publicationDate == "2024-06-15"
    rec.metadata.description == "<p>A test record.</p>"
    rec.metadata.creators.size() == 2
    rec.metadata.creators[0].resolvedName() == "Smith, Alice"
    rec.metadata.creators[1].resolvedName() == "Jones, Bob"
    rec.metadata.resourceType.resolvedTitle() == "Dataset"

    and: "community is extracted from parent.communities.entries"
    rec.parent != null
    rec.parent.communities != null
    rec.parent.communities.defaultCommunity == "73eb987a-a71f-4ed7-950f-a4b68192d85d"
    rec.parent.communities.entries.size() == 1
    rec.parent.communities.entries[0].displayLabel() == "Faculty of Humanities"
  }

  def "parses v12 record without community (null parent)"() {
    given: "a v12 record response with no parent block"
    def json = '''
    {
      "id": "xyz-456",
      "is_published": true,
      "access": { "record": "public", "files": "public", "status": "open" },
      "pids": { "doi": { "identifier": "10.57754/FDAT.xyz-456" } },
      "versions": { "is_latest": true, "index": 1 },
      "metadata": {
        "title": "Record Without Community",
        "publication_date": "2024-01-01",
        "creators": [],
        "resource_type": { "id": "dataset", "title": { "en": "Dataset" } }
      }
    }
    '''

    when:
    def rec = MAPPER.readValue(json, InvenioRdmClient.RecordResponse)

    then:
    rec.id == "xyz-456"
    rec.parent == null
    rec.metadata.title == "Record Without Community"
    InvenioRdmDatasetSource.community(rec.parent) == null
  }

  def "parses v12 record with empty communities entries"() {
    given:
    def json = '''
    {
      "id": "empty-001",
      "access": { "status": "open" },
      "parent": { "communities": { "entries": [] } },
      "metadata": { "title": "No Community Record", "publication_date": "2024-01-01" }
    }
    '''

    when:
    def rec = MAPPER.readValue(json, InvenioRdmClient.RecordResponse)

    then:
    rec.parent != null
    rec.parent.communities.entries.isEmpty()
    InvenioRdmDatasetSource.community(rec.parent) == null
  }

  // ── Search response parsing ─────────────────────────────────────────

  def "parses v12 search response with hits"() {
    given: "a v12 search response"
    def json = '''
    {
      "hits": {
        "total": 42,
        "hits": [
          {
            "id": "rec-001",
            "links": { "self_html": "https://fdat.example.org/records/rec-001" },
            "created": "2024-06-15T10:00:00.000000+00:00",
            "access": {
              "record": "public",
              "files": "public",
              "status": "open",
              "embargo": { "active": false, "until": null, "reason": null }
            },
            "metadata": {
              "title": "Test Dataset One",
              "publication_date": "2024-06-15",
              "description": "First test dataset",
              "creators": [
                {
                  "person_or_org": {
                    "type": "personal",
                    "name": "Doe, Jane",
                    "given_name": "Jane",
                    "family_name": "Doe"
                  },
                  "affiliations": [{ "id": "01abc", "name": "Test University" }]
                }
              ],
              "resource_type": { "id": "dataset", "title": { "en": "Dataset" } }
            },
            "pids": {
              "doi": { "identifier": "10.57754/FDAT.rec-001" }
            },
            "versions": { "is_latest": true, "index": 3 }
          },
          {
            "id": "rec-002",
            "links": { "self_html": "https://fdat.example.org/records/rec-002" },
            "access": {
              "record": "restricted",
              "files": "restricted",
              "status": "restricted",
              "embargo": { "active": false }
            },
            "metadata": {
              "title": "Restricted Record",
              "publication_date": "2024-07-01",
              "creators": [],
              "resource_type": { "id": "dataset" }
            },
            "pids": {
              "doi": { "identifier": "10.57754/FDAT.rec-002" }
            },
            "versions": { "is_latest": false, "index": 1 }
          }
        ]
      }
    }
    '''

    when: "parsed"
    def response = MAPPER.readValue(json, InvenioRdmClient.SearchResultResponse)

    then: "envelope is correct"
    response.hits.total == 42
    response.hits.hits.size() == 2

    and: "first hit is parsed"
    def h1 = response.hits.hits[0]
    h1.id == "rec-001"
    h1.links.selfHtml == "https://fdat.example.org/records/rec-001"
    h1.metadata.title == "Test Dataset One"
    h1.metadata.publicationDate == "2024-06-15"
    h1.metadata.creators.size() == 1
    h1.metadata.creators[0].resolvedName() == "Doe, Jane"
    h1.metadata.resourceType.resolvedTitle() == "Dataset"
    h1.access.status == "open"
    h1.pids.doi.identifier == "10.57754/FDAT.rec-001"
    h1.versions.index == 3
    h1.versions.isLatest

    and: "second hit is parsed"
    def h2 = response.hits.hits[1]
    h2.id == "rec-002"
    h2.access.status == "restricted"
    h2.metadata.creators.isEmpty()
    h2.metadata.resourceType.resolvedTitle() == null
    h2.versions.index == 1
    !h2.versions.isLatest
  }

  def "parses empty search response"() {
    given:
    def json = '''{"hits": {"total": 0, "hits": []}}'''

    when:
    def response = MAPPER.readValue(json, InvenioRdmClient.SearchResultResponse)

    then:
    response.hits.total == 0
    response.hits.hits.isEmpty()
  }

  // ── Community display label precedence ──────────────────────────────

  def "Community.displayLabel prefers title over slug over id"() {
    given:
    def c = new InvenioRdmClient.Community(id, slug, meta)

    expect:
    InvenioRdmDatasetSource.community(buildParent(c)) == expected

    where:
    id     | slug            | meta                                           | expected
    "abc"  | "biology"       | metaWithTitle("Department of Biology")         | "Department of Biology"
    "abc"  | "biology"       | null                                           | "biology"
    "abc"  | null            | null                                           | "abc"
    "abc"  | "biology"       | metaWithTitle("")                              | "biology"
    "abc"  | null            | metaWithTitle("A Group")                       | "A Group"
  }

  private InvenioRdmClient.Parent buildParent(InvenioRdmClient.Community c) {
    def pc = new InvenioRdmClient.ParentCommunities(null, null, [c])
    def p = new InvenioRdmClient.Parent(pc)
    return p
  }

  private InvenioRdmClient.CommunityMetadata metaWithTitle(String title) {
    new InvenioRdmClient.CommunityMetadata(title)
  }

  // ── Access mapping ──────────────────────────────────────────────────

  def "access.status 'open' maps to PUBLIC"() {
    given:
    def access = new InvenioRdmClient.RecordAccess("public", "public", "open")

    expect:
    InvenioRdmDatasetSource.accessLevel(access) == life.qbic.projectmanagement.domain.model.associated_dataset.AccessLevel.PUBLIC
    InvenioRdmDatasetSource.recordAccessStatus(access) == life.qbic.projectmanagement.domain.model.associated_dataset.InvenioRdmAccessStatus.PUBLIC
    InvenioRdmDatasetSource.fileAccessStatus(access) == life.qbic.projectmanagement.domain.model.associated_dataset.InvenioRdmAccessStatus.PUBLIC
  }

  def "access.status 'restricted' maps to RESTRICTED"() {
    given:
    def access = new InvenioRdmClient.RecordAccess("restricted", "restricted", "restricted")

    expect:
    InvenioRdmDatasetSource.accessLevel(access) == life.qbic.projectmanagement.domain.model.associated_dataset.AccessLevel.RESTRICTED
    InvenioRdmDatasetSource.recordAccessStatus(access) == life.qbic.projectmanagement.domain.model.associated_dataset.InvenioRdmAccessStatus.RESTRICTED
    InvenioRdmDatasetSource.fileAccessStatus(access) == life.qbic.projectmanagement.domain.model.associated_dataset.InvenioRdmAccessStatus.RESTRICTED
  }

  def "access with independent record/file dimensions"() {
    given: "record is public, files are restricted"
    def access = new InvenioRdmClient.RecordAccess("public", "restricted", "open")

    expect:
    InvenioRdmDatasetSource.recordAccessStatus(access) == life.qbic.projectmanagement.domain.model.associated_dataset.InvenioRdmAccessStatus.PUBLIC
    InvenioRdmDatasetSource.fileAccessStatus(access) == life.qbic.projectmanagement.domain.model.associated_dataset.InvenioRdmAccessStatus.RESTRICTED
    InvenioRdmDatasetSource.accessDetail(access) == "Record: public | Files: restricted"
  }

  def "null access maps to RESTRICTED for coarse level"() {
    expect:
    InvenioRdmDatasetSource.accessLevel(null) == life.qbic.projectmanagement.domain.model.associated_dataset.AccessLevel.RESTRICTED
  }

  // ── PID extraction ─────────────────────────────────────────────────

  def "DOI is extracted from pids.doi.identifier"() {
    given:
    def pids = new InvenioRdmClient.Pids(
        new InvenioRdmClient.PidEntry("10.57754/FDAT.abc-123"))

    expect:
    InvenioRdmDatasetSource.safePid(pids, "fallback-id") == "10.57754/FDAT.abc-123"
  }

  def "null pids falls back to record ID"() {
    expect:
    InvenioRdmDatasetSource.safePid(null, "rec-42") == "rec-42"
  }

  def "pids with no DOI falls back to record ID"() {
    given:
    def pids = new InvenioRdmClient.Pids(null)

    expect:
    InvenioRdmDatasetSource.safePid(pids, "rec-99") == "rec-99"
  }

  // ── Version extraction ──────────────────────────────────────────────

  def "version index is formatted as v-number"() {
    given:
    def versions = new InvenioRdmClient.RecordVersions(true, 5)

    expect:
    InvenioRdmDatasetSource.versionString(versions) == "v5"
  }

  def "null versions returns null"() {
    expect:
    InvenioRdmDatasetSource.versionString(null) == null
  }

  def "version index zero returns null"() {
    given:
    def versions = new InvenioRdmClient.RecordVersions(false, 0)

    expect:
    InvenioRdmDatasetSource.versionString(versions) == null
  }

  // ── Creator resolution ─────────────────────────────────────────────

  def "creators are resolved from person_or_org.name"() {
    given: "a v12 creator with nested person_or_org"
    def json = '''
    {
      "person_or_org": {
        "type": "personal",
        "name": "Meyer, Lina",
        "given_name": "Lina",
        "family_name": "Meyer"
      },
      "affiliations": [{ "id": "01abc", "name": "Uni Tübingen" }]
    }
    '''

    when:
    def creator = MAPPER.readValue(json, InvenioRdmClient.Creator)

    then:
    creator.resolvedName() == "Meyer, Lina"
    creator.resolvedAffiliation() == "Uni Tübingen"
  }

  def "creators without person_or_org resolve to null name"() {
    given: "a creator entry with no person_or_org (should not happen in v12, handled defensively)"
    def json = '''{ "affiliations": [] }'''

    when:
    def creator = MAPPER.readValue(json, InvenioRdmClient.Creator)

    then:
    creator.resolvedName() == null
    creator.resolvedAffiliation() == null
  }

  def "multiple creators are collected"() {
    given:
    def json = '''
    [
      { "person_or_org": { "name": "A, B" }, "affiliations": [] },
      { "person_or_org": { "name": "C, D" }, "affiliations": [] },
      { "affiliations": [] }
    ]
    '''

    when:
    def raw = MAPPER.readValue(json,
        MAPPER.typeFactory.constructCollectionType(List, InvenioRdmClient.Creator))

    then:
    def names = InvenioRdmDatasetSource.resolvedCreators(raw)
    names == ["A, B", "C, D"]
    names.size() == 2
  }

  def "null creators list returns empty"() {
    expect:
    InvenioRdmDatasetSource.resolvedCreators(null).isEmpty()
  }

  // ── Resource type title resolution ─────────────────────────────────

  def "resource type with localised title object"() {
    given:
    def json = '''{ "id": "dataset", "title": { "en": "Dataset", "de": "Datensatz" } }'''

    when:
    def rt = MAPPER.readValue(json, InvenioRdmClient.ResourceType)

    then:
    rt.id == "dataset"
    rt.resolvedTitle() == "Dataset"
  }

  def "resource type with only non-English locale"() {
    given:
    def json = '''{ "id": "dataset", "title": { "de": "Datensatz" } }'''

    when:
    def rt = MAPPER.readValue(json, InvenioRdmClient.ResourceType)

    then:
    rt.resolvedTitle() == "Datensatz"
  }

  def "resource type with missing title returns null"() {
    given:
    def json = '''{ "id": "dataset" }'''

    when:
    def rt = MAPPER.readValue(json, InvenioRdmClient.ResourceType)

    then:
    rt.resolvedTitle() == null
  }

  // ── Date parsing ────────────────────────────────────────────────────

  def "valid publication date is parsed"() {
    expect:
    InvenioRdmDatasetSource.parseDateOrToday("2024-07-14") ==
        java.time.LocalDate.of(2024, 7, 14)
  }

  def "null publication date falls back to today"() {
    expect:
    InvenioRdmDatasetSource.parseDateOrToday(null) == java.time.LocalDate.now()
  }

  def "blank publication date falls back to today"() {
    expect:
    InvenioRdmDatasetSource.parseDateOrToday("  ") == java.time.LocalDate.now()
  }

  def "malformed publication date falls back to today"() {
    expect:
    InvenioRdmDatasetSource.parseDateOrToday("not-a-date") ==
        java.time.LocalDate.now()
  }

  // ── Title extraction ────────────────────────────────────────────────

  def "missing hit title falls back to untitled placeholder"() {
    given:
    def h = new InvenioRdmClient.Hit(
        null, null, new InvenioRdmClient.HitMetadata(
            null, null, null, null, null),
        null, null, null, null, null)

    expect:
    InvenioRdmDatasetSource.safeHitTitle(h) == "(untitled record)"
  }

  def "blank record title falls back to untitled placeholder"() {
    given:
    def rec = new InvenioRdmClient.RecordResponse(
        null, null, new InvenioRdmClient.RecordMetadata(
            "   ", null, null, null, null),
        null, null, false, null, null, null, null)

    expect:
    InvenioRdmDatasetSource.safeRecordTitle(rec) == "(untitled record)"
  }

  // ── Zenodo record (with v12 header) ─────────────────────────────────

  def "parses Zenodo v12 record with biosyslit community"() {
    given: "a full Zenodo record as served with v12 Accept header"
    def json = '''
    {
      "id": "20654629",
      "links": {
        "self_html": "https://zenodo.org/records/20654629"
      },
      "is_published": true,
      "access": {
        "record": "public",
        "files": "public",
        "embargo": { "active": false, "reason": null },
        "status": "open"
      },
      "pids": {
        "doi": {
          "identifier": "10.5281/zenodo.20654629",
          "provider": "datacite",
          "client": "datacite"
        }
      },
      "versions": { "is_latest": true, "index": 3 },
      "parent": {
        "communities": {
          "default": "c529f97d-f8cb-4c13-a439-9e36891694c2",
          "ids": ["c529f97d-f8cb-4c13-a439-9e36891694c2"],
          "entries": [{
            "id": "c529f97d-f8cb-4c13-a439-9e36891694c2",
            "slug": "biosyslit",
            "metadata": { "title": "Biodiversity Literature Repository" }
          }]
        }
      },
      "metadata": {
        "title": "Fig. 2 in The structure of terrestrial mammal communities...",
        "publication_date": "2024-08-19",
        "creators": [
          {
            "person_or_org": {
              "type": "personal",
              "name": "McShea, William J",
              "given_name": "William J",
              "family_name": "McShea"
            }
          }
        ],
        "resource_type": { "id": "image-figure", "title": { "en": "Figure", "de": "Abbildung" } }
      }
    }
    '''

    when:
    def rec = MAPPER.readValue(json, InvenioRdmClient.RecordResponse)

    then: "all v12 fields are parsed correctly"
    rec.id == "20654629"
    rec.links.selfHtml == "https://zenodo.org/records/20654629"
    InvenioRdmDatasetSource.safePid(rec.pids, rec.id) == "10.5281/zenodo.20654629"
    InvenioRdmDatasetSource.versionString(rec.versions) == "v3"
    InvenioRdmDatasetSource.community(rec.parent) == "Biodiversity Literature Repository"
    InvenioRdmDatasetSource.safeRecordTitle(rec) == "Fig. 2 in The structure of terrestrial mammal communities..."
    InvenioRdmDatasetSource.resolvedResourceType(rec.metadata.resourceType) == "Figure"
    InvenioRdmDatasetSource.accessLevel(rec.access) == life.qbic.projectmanagement.domain.model.associated_dataset.AccessLevel.PUBLIC
  }
}
