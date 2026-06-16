# Implementation Plan — Story #1432: View and Access Immunopeptidomics Raw Datasets

**Story:** [View and Access Immunopeptidomics Raw Datasets](https://github.com/qbicsoftware/data-manager-app/issues/1432)
**Parent Feature:** [FEAT-IMMUNOPEPTIDOMICS-MEASUREMENT (#1412)](https://github.com/qbicsoftware/data-manager-app/issues/1412)
**Parent Task:** [Show Immunopeptidomic datasets within the raw data view (#1416)](https://github.com/qbicsoftware/data-manager-app/issues/1416)
**Requirement IDs:** `DATA-R-01`, `DATA-R-02`, `DATA-R-03`

---

## 1. User Story Recap

> As a user with project access, I want to see available immunopeptidomics raw datasets for measured samples and instructions how to access them for detailed investigation and processing.

**Acceptance Criteria:**
| # | Criteria |
|---|----------|
| AC1 | User can see contextual metadata and properties about each raw dataset |
| AC2 | User can filter datasets with generic text input |
| AC3 | User can see the total number of available raw datasets |
| AC4 | When no datasets exist, user is informed about how to register and that none exist yet |

---

## 2. Architecture & Design

### 2.1 The Pattern

The raw data view already follows a **three-tab, three-domain** pattern:

| Domain | Tab Name | Entity (Infrastructure) | View (DB) | Service Methods |
|--------|----------|------------------------|-----------|-----------------|
| NGS (Genomics) | "Genomics" | `LocalRawDatasetNgsEntry` | `v_ngs_measurement_sample_json` | `getRawDatasetInformationNgs()`, `countRawDataNgs()` |
| PxP (Proteomics) | "Proteomics" | `LocalRawDatasetPxpEntry` | `v_pxp_measurement_sample_json` | `getRawDatasetInformationPxP()`, `countRawDataPxp()` |
| **IP (Immunopeptidomics)** | **"Immunopeptidomics"** | `LocalRawDatasetIpEntry` (new) | `v_ip_measurement_sample_json` (new) | `getRawDatasetInformationIp()`, `countRawDataIp()` (new) |

Each domain has:
1. **Domain-specific JPA entity** mapped to a SQL view (read-only, populated by the data-scanner)
2. **Repository interface** extending `PagingAndSortingRepository` + `JpaSpecificationExecutor`
3. **Repository impl methods** in `LocalRawDatasetRepositoryImpl` that apply specs + pagination
4. **Service methods** in `AsyncProjectService` / `AsyncProjectServiceImpl` that expose `Flux<RawDatasetInformationXxx>` and `Mono<Integer> countRawDataXxx()`
5. **UI tab** in `RawDataDetailsComponent` with `FilterGrid`, search, sorting, and selection
6. **`hasRawData()` awareness** in `RemoteRawDataService` so the tab even shows up

The IP domain will replicate this pattern.

### 2.2 Measurement Code Prefix

Immunopeptidomics measurements use the `IP-` prefix (as documented in Feature #1412). The SQL view will filter `RemoteRawDatasetEntry` records where the measurement code starts with `IP-`. The data-scanner backend (separate repo) already associates uploaded files with `IP-` prefixed measurements.

### 2.3 Database View

The view `v_ip_measurement_sample_json` must mirror the shape of `v_ngs_measurement_sample_json` and `v_pxp_measurement_sample_json`:

| Column | Type | Source |
|--------|------|--------|
| `measurement_id` | String | From measurement record |
| `measurementCode` | String | `IP-XXXXX` |
| `measurementName` | String | From measurement metadata |
| `registration_at` | Timestamp | File upload timestamp |
| `file_count` | Int | Number of files |
| `file_types` | JSON | File extensions (array → JSON) |
| `total_filesize_bytes` | Long | Sum of file sizes |
| `experiment_id` | String | Parent experiment |
| `samples_json` | JSON | Array of `{sampleId, sampleName, label}` |

The view joins `remote_measurement_data` (base cache) against the IP measurement code set. A future unified MS read model can replace this with a single view, but per Feature #1412, the parallel-class pattern is the current path.

---

## 3. Task Breakdown

### Task 1: Database — Create `v_ip_measurement_sample_json` view

**Module:** SQL scripts / migration
**File:** `sql/complete-schema.sql` (or a new `sql/ip-view.sql`)

Create a SQL view that mirrors `v_ngs_measurement_sample_json` but filters for `IP-` prefixed measurement codes:

```sql
CREATE OR REPLACE VIEW v_ip_measurement_sample_json AS
SELECT
  rmd.measurement_id,
  m.measurement_code AS measurementCode,
  m.name AS measurementName,
  rmd.registration_at,
  rmd.file_count,
  rmd.file_types,
  rmd.total_filesize_bytes,
  m.experiment_id,
  (SELECT JSON_ARRAYAGG(
    JSON_OBJECT(
      'sampleId', s.sample_code,
      'sampleName', s.name,
      'label', s.code
    )
  )
   FROM sample s
   JOIN sample_measurement sm ON s.sample_id = sm.sample_id
   WHERE sm.measurement_id = rmd.measurement_id
  ) AS samples_json
FROM remote_measurement_data rmd
JOIN measurement m ON rmd.measurement_id = m.measurement_id
WHERE m.measurement_code LIKE 'IP-%'
  AND rmd.deleted = false;
```

> **Note:** The exact JOIN logic should match the patterns used in `v_ngs_measurement_sample_json` and `v_pxp_measurement_sample_json` — verify against the existing views in `sql/complete-schema.sql`.

---

### Task 2: Infrastructure — `LocalRawDatasetIpEntry` entity

**Module:** `project-management-infrastructure`
**File:** `src/main/java/life/qbic/projectmanagement/infrastructure/dataset/LocalRawDatasetIpEntry.java`

Create a JPA entity mapped to the `v_ip_measurement_sample_json` view, structurally identical to `LocalRawDatasetNgsEntry` and `LocalRawDatasetPxpEntry`:

- `@Entity`, `@Table(name = "v_ip_measurement_sample_json")`
- `@Id` on `measurement_id`
- Fields: `id`, `totalFileSizeBytes`, `measurementCode`, `measurementName`, `registrationDate`, `numberOfFiles`, `fileTypes` (converted via `FileTypesConverter`), `experimentId`, `measuredSamples` (converted via `MeasuredSamplesConverter`)
- Getters for all fields
- `equals()` / `hashCode()` following the NGS/PxP pattern

---

### Task 3: Infrastructure — `LocalRawDatasetIpJpaRepository`

**Module:** `project-management-infrastructure`
**File:** `src/main/java/life/qbic/projectmanagement/infrastructure/dataset/LocalRawDatasetIpJpaRepository.java`

Mirror `LocalRawDatasetInformationNgsJpaRepository`:

```java
public interface LocalRawDatasetIpJpaRepository
    extends PagingAndSortingRepository<LocalRawDatasetIpEntry, String>,
            JpaSpecificationExecutor<LocalRawDatasetIpEntry> {

  Page<LocalRawDatasetIpEntry> findAllByExperimentId(String experimentId, Pageable pageable);
}
```

---

### Task 4: Application — `LocalRawDatasetRepository` extension

**Module:** `project-management` (application layer)
**File:** `src/main/java/life/qbic/projectmanagement/application/dataset/LocalRawDatasetRepository.java`

Add three methods to the interface:

```java
List<RawDatasetInformationIp> findAllIp(String experimentId, int offset, int limit,
                                        RawDatasetFilter filter) throws LookupException;

Integer countIp(String experimentId, RawDatasetFilter filter) throws LookupException;
```

> **Note:** The interface already has `findAllPxP` and `findAllNgs` variants. Add the `Ip` variants following the same signature pattern.

---

### Task 5: Infrastructure — `LocalRawDatasetRepositoryImpl` methods

**Module:** `project-management-infrastructure`
**File:** `src/main/java/life/qbic/projectmanagement/infrastructure/dataset/LocalRawDatasetRepositoryImpl.java`

Add implementations for `findAllIp()` and `countIp()`. The `createFullSpecificationIp()` method should mirror `createFullSpecificationNgs()` but target the IP repository:

```java
private static Specification<LocalRawDatasetIpEntry> createFullSpecificationIp(RawDatasetFilter filter, String experimentId) {
  return distinct(
      allOf(
          exactMatches(root -> root.get("experimentId"), experimentId),
          anyOf(
              jsonContains(root -> root.get("measuredSamples"), "$[*].label", filter.filterTerm()),
              propertyContains("measurementCode", filter.filterTerm())
          )
      ));
}
```

Inject `LocalRawDatasetIpJpaRepository` as a constructor parameter (alongside the existing NGS/PxP repos).

In `LocalRawDatasetLookupService`, add `findAllIp()` and `countIp()` delegating to the repository with the existing `@PreAuthorize` annotation.

---

### Task 6: API — `AsyncProjectService` — `RawDatasetInformationIp` record + methods

**Module:** `project-management` (application API)
**File:** `src/main/java/life/qbic/projectmanagement/application/api/AsyncProjectService.java`

Add the following:

1. **`RawDatasetInformationIp` record** — identical structure to `RawDatasetInformationNgs` and `RawDatasetInformationPxP`:

```java
record RawDatasetInformationIp(RawDataset dataset,
                               List<BasicSampleInformation> linkedSampleInformation,
                               String measurementName) {
  public RawDatasetInformationIp {
    requireNonNull(dataset);
    requireNonNull(linkedSampleInformation);
    requireNonNull(measurementName);
  }
}
```

2. **Service methods** (mirroring NGS/PxP signatures):

```java
Flux<RawDatasetInformationIp> getRawDatasetInformationIp(String projectId, String experimentId,
    int offset, int limit, RawDatasetFilter filter);

Mono<Integer> countRawDataIp(String projectId, String experimentId, RawDatasetFilter rawDataFilter);
```

3. **`RawDataSortingKey` enum** — ensure existing keys (`SAMPLE_NAME`, `MEASUREMENT_ID`, `UPLOAD_DATE`) work for IP too (they should, since the entity shape is identical).

---

### Task 7: API — `AsyncProjectServiceImpl` — IP implementations

**Module:** `project-management`
**File:** `src/main/java/life/qbic/projectmanagement/application/api/AsyncProjectServiceImpl.java`

Implement the two new methods, mirroring the PxP/Ngs patterns:

```java
@Override
public Flux<RawDatasetInformationIp> getRawDatasetInformationIp(String projectId,
    String experimentId, int offset, int limit, RawDatasetFilter filter) {
  SecurityContext securityContext = SecurityContextHolder.getContext();
  return Flux.fromIterable(
          rawDatasetLookupService.findAllIp(projectId, experimentId, offset, limit, filter))
      .subscribeOn(scheduler)
      .contextWrite(reactiveSecurity(securityContext))
      .doOnError(error -> log.error(
          "Error for requesting raw datasets in project in the immunopeptidomics domain " + projectId,
          error))
      .onErrorMap(e -> mapToAPIException(e, "Raw dataset request failed"))
      .retryWhen(defaultRetryStrategy());
}

@Override
public Mono<Integer> countRawDataIp(String projectId, String experimentId,
    RawDatasetFilter rawDataFilter) {
  SecurityContext securityContext = SecurityContextHolder.getContext();
  return applySecurityContext(Mono.fromCallable(() ->
      rawDatasetLookupService.countIp(projectId, experimentId, rawDataFilter)))
      .subscribeOn(scheduler)
      .contextWrite(reactiveSecurity(securityContext))
      .doOnError(error -> log.error(
          "Error counting immunopeptidomics measurements for project " + projectId, error))
      .onErrorMap(e -> mapToAPIException(e, "Error counting immunopeptidomics measurements"))
      .retryWhen(defaultRetryStrategy());
}
```

---

### Task 8: UI — `RawDataDetailsComponent` — IP Tab

**Module:** `datamanager-app`
**File:** `src/main/java/life/qbic/datamanager/views/projects/project/rawdata/RawDataDetailsComponent.java`

Add an IP tab to the `FilterGridTabSheet` alongside NGS and PxP tabs. The pattern is already established:

1. **Check for existence:** `asyncProjectService.countRawDataIp(...).block() > 0`
2. **Create grid:** `createIpRawDataGrid()` with columns: Measurement Id, Measurement Name, Sample Name, Upload Date
3. **Create filter grid:** `createIpFilterGrid(grid, projectId, experimentId)` with search callback
4. **Add tab:** `addIpTab(filterTabSheet, 2, "Immunopeptidomics", filterGrid)`
5. **Primary action:** Export dataset URLs (like NGS/PxP tabs) — export URLs of selected IP datasets
6. **Item details renderer:** Show Sample Name(s), Number of Files, File Size, File Suffixes (identical to NGS/PxP detail rows)

The grid columns should reuse the existing `UiSortKey` enum values and `SORT_KEY_MAP` since they already cover `MEASUREMENT_ID`, `SAMPLE_NAME`, and `UPLOAD_DATE`.

**Export action file naming:** Use `"immunopeptidomics_measurement_dataset_locations.txt"` for the export filename (consistent with NGS/PxP naming).

---

### Task 9: UI — `RawDataMain` — `hasRawData()` IP awareness

**Module:** `project-management`
**File:** `src/main/java/life/qbic/projectmanagement/application/dataset/RemoteRawDataService.java`

Update `hasRawData()` to also check for IP measurements:

```java
// Add after existing NGS + PxP checks
private interface ImmunopeptidomicsMeasurementLookup {
  long countIpMeasurements(String projectId, MeasurementFilter filter);
  Stream<String> lookupIpMeasurements(String projectId, int offset, long count,
                                       Sort sort, MeasurementFilter filter);
  
  interface MeasurementFilter {
    static MeasurementFilter forExperiment(String experimentId) {
      // returns a filter that matches IP- prefixed codes for the experiment
    }
  }
}
```

Add IP measurement lookup similar to how NGS/PxP are looked up, then add IP measurement codes to the `allCodes` set before calling `remoteRawDataLookupService.countRawDataByMeasurementCodes()`.

> **Dependency note:** This requires the IP measurement lookup service (created in Story #1431) to be available. Ensure `RemoteRawDataService` has access to it via constructor injection.

---

### Task 10: Tests

#### Unit Tests
- **`LocalRawDatasetIpEntryTest`** — entity mapping, equals/hashCode
- **`LocalRawDatasetIpJpaRepositoryTest`** — repository queries against test DB
- **`LocalRawDatasetRepositoryImplIpTest`** — specification building, pagination, filtering
- **`AsyncProjectServiceIpTest`** — service layer IP methods (mock lookup service)

#### Integration Tests
- **Story #1441** (parent Task #1432's IT task): `RawDataDetailsComponentIpIntegrationTest` — verify the IP tab renders, search filters, and selection works end-to-end

---

## 4. Dependency Graph

```
Task 1 (DB view) ──┐
                   ├──→ Task 2 (Entity) ──→ Task 3 (Repo) ──→ Task 4 (API iface)
                   │                                  └──→ Task 5 (Repo impl) ──→ Task 6 (Service API)
Task 11 (IP Measurement Lookup, #1417) ────────────────────────────────────────────────→ Task 8 (UI)
                                                                                          ↑
Task 9 (hasRawData) ────────────────────────────────────────────────────────────────────┘
                                                                                          ↓
                                                                                Task 10 (Tests)
```

**Critical path:** Task 1 → Task 2 → Task 3 → Task 4 → Task 5 → Task 6 → Task 7 → Task 9 → Task 8 → Task 10

---

## 5. Files to Create / Modify

| # | Action | Module | File |
|---|--------|--------|------|
| 1 | Create | SQL | `sql/complete-schema.sql` (append `v_ip_measurement_sample_json`) |
| 2 | Create | infra | `LocalRawDatasetIpEntry.java` |
| 3 | Create | infra | `LocalRawDatasetIpJpaRepository.java` |
| 4 | Modify | application | `LocalRawDatasetRepository.java` (add `findAllIp`, `countIp`) |
| 5 | Modify | infra | `LocalRawDatasetRepositoryImpl.java` (add IP impl + repo injection) |
| 6 | Modify | application | `LocalRawDatasetLookupService.java` (add IP delegation) |
| 7 | Modify | application | `AsyncProjectService.java` (add `RawDatasetInformationIp`, `getRawDatasetInformationIp`, `countRawDataIp`) |
| 8 | Modify | application | `AsyncProjectServiceImpl.java` (add IP method implementations) |
| 9 | Modify | UI | `RawDataDetailsComponent.java` (add IP tab) |
| 10 | Modify | application | `RemoteRawDataService.java` (add IP to `hasRawData`) |
| 11 | Create | test | Various `*Test.java` and `*Spec.groovy` files |

---

## 6. Risks & Considerations

| Risk | Mitigation |
|------|-----------|
| **DB view DDL changes** — modifying `complete-schema.sql` affects all environments | Follow the AGENTS.md rule: require human approval for schema changes; document the DDL diff in a PR |
| **Data-scanner sync timing** — raw data won't appear until the scanner has processed IP measurements | The `LocalRawDatasetEntry` (base cache table) already exists and supports `IP-` prefixed measurement IDs; only the SQL view needs to be created |
| **Measurement lookup service** — `RemoteRawDataService.hasRawData()` needs access to IP measurement codes | Depends on the IP measurement lookup service from Story #1431 (Task #1417). If that task isn't complete, `hasRawData()` won't show the tab. Coordinate with Story #1431's implementation |
| **Export action file naming** — NGS uses `proteomics_...` and PxP uses `ngs_...` in filenames (swapped?) | Verify the existing NGS/PxP export filenames in `RawDataDetailsComponent.java` lines ~138-146. The IP export should use `"immunopeptidomics_measurement_dataset_locations.txt"` |
| **Sorting consistency** — the existing `SORT_KEY_MAP` already supports all three keys needed for IP | No changes needed to `UiSortKey` or `SORT_KEY_MAP` — the IP entity structure is identical to NGS/PxP |

---

## 7. Acceptance Verification

| Criterion | Verification Command |
|-----------|---------------------|
| AC1 — Contextual metadata visible | Run in dev mode (`./mvnw spring-boot:run -pl datamanager-app -Pdevelopment`), navigate to a project with IP measurements, verify tab shows all 6 columns |
| AC2 — Filter works | Type in the search field; verify grid rows update; check log for `RawDatasetFilter` containing the search term |
| AC3 — Dataset count shown | Verify the `FilterGrid` shows total count (e.g., "X datasets"); verify `countRawDataIp()` returns correct value |
| AC4 — Empty state messaging | When no IP datasets exist, `hasRawData()` returns false → `RawDataMain` shows `noRawDataRegisteredDisclaimer` |
| Build passes | `./mvnw clean verify -pl datamanager-app,project-management,project-management-infrastructure` |

---

## 8. Story-Level Acceptance

All 4 acceptance criteria are addressed. No new requirements are introduced — this story implements the **view** capability already covered by `DATA-R-01`, `DATA-R-02`, `DATA-R-03` in the Feature #1412 scope. The implementation is a horizontal extension of the existing NGS/PxP pattern, not a new capability.
