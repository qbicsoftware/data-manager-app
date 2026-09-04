package life.qbic.projectmanagement.application.associated_dataset;

import java.util.Objects;
import life.qbic.projectmanagement.domain.model.associated_dataset.ResourceMetadata;

/**
 * Result of resolving the current/latest state of a record on a source
 * system (sync, DATSET-04/08).
 *
 * <p>Pairs the canonical {@link ResourceMetadata} snapshot with the
 * <em>effective</em> external handle. For versioned sources (InvenioRDM)
 * the effective handle may differ from the handle the caller resolved —
 * when a new version was published the latest record lives under a new
 * record id — and the connection must follow it (ADR-0005).</p>
 *
 * @param metadata            the latest metadata snapshot
 * @param externalHandleValue the record identifier the snapshot was
 *                            actually fetched from (the latest record)
 * @since 1.13.0
 */
public record ResolvedRecord(ResourceMetadata metadata, String externalHandleValue) {

  public ResolvedRecord {
    Objects.requireNonNull(metadata, "metadata must not be null");
    Objects.requireNonNull(externalHandleValue, "externalHandleValue must not be null");
  }
}