package life.qbic.projectmanagement.domain.model.associated_dataset;

import java.util.Objects;

/**
 * InvenioRDM-specific access status of a record or its files.
 *
 * <p>InvenioRDM's access model distinguishes two independent dimensions:
 * <ul>
 *   <li>Record-level access — whether the metadata/record itself is visible</li>
 *   <li>File-level access — whether the actual files are downloadable</li>
 * </ul>
 * Each dimension independently carries one of: {@link #PUBLIC}, {@link #RESTRICTED},
 * or {@link #EMBARGOED} (time-locked until a specific date).</p>
 *
 * <p>This is an InvenioRDM-specific vocabulary. The source-agnostic aggregate
 * derives a coarse {@link AccessLevel} from it (see {@code AssociatedDataset.connect}).</p>
 *
 * @since 1.12.0
 */
public enum InvenioRdmAccessStatus {

  /**
   * Fully accessible without authentication.
   */
  PUBLIC,

  /**
   * Requires authentication (a valid Personal Access Token) to access.
   */
  RESTRICTED,

  /**
   * Time-locked — accessible only after a specific date.
   */
  EMBARGOED;

  public static InvenioRdmAccessStatus parse(String value) {
    Objects.requireNonNull(value, "value must not be null");
    try {
      return valueOf(value.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Unknown InvenioRDM access status: " + value, e);
    }
  }
}
