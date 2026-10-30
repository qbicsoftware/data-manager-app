package life.qbic.datamanager.views.projects.project.dataset;

import java.util.List;
import life.qbic.projectmanagement.application.query.MeasurementDataRow;

/**
 * What the actions of the Dataset page need to know about the current selection.
 *
 * <p>The page groups its measurements by measurement method, and every write action
 * (export, edit, delete) is per-method at the service boundary: a genomics measurement is deleted
 * through {@code deleteNgsMeasurements}, a proteomics one through {@code deletePxpMeasurements}.
 * Rather than scatter that dispatch across the view, the selection is reduced once into ids per
 * method, here, and the action code reads it.</p>
 *
 * <p>{@code genomics}, {@code proteomics} and {@code immunopeptidomics} hold the selected
 * measurement ids per method. {@code withData} counts how many of the selected measurements already
 * have data available, and {@code total} is the size of the whole selection.</p>
 * @since 1.21.0
 */
public record MeasurementSelection(List<String> genomics, List<String> proteomics,
                                   List<String> immunopeptidomics, int withData, int total) {

  public MeasurementSelection {
    genomics = genomics == null ? List.of() : List.copyOf(genomics);
    proteomics = proteomics == null ? List.of() : List.copyOf(proteomics);
    immunopeptidomics = immunopeptidomics == null ? List.of() : List.copyOf(immunopeptidomics);
  }

  /** A selection with nothing in it. */
  public static MeasurementSelection empty() {
    return new MeasurementSelection(List.of(), List.of(), List.of(), 0, 0);
  }

  /**
   * Reduces the selected measurement rows into ids per measurement method.
   *
   * @param rows the selected rows
   */
  public static MeasurementSelection of(List<MeasurementDataRow> rows) {
    if (rows == null || rows.isEmpty()) {
      return empty();
    }
    java.util.List<String> genomics = new java.util.ArrayList<>();
    java.util.List<String> proteomics = new java.util.ArrayList<>();
    java.util.List<String> immunopeptidomics = new java.util.ArrayList<>();
    int withData = 0;
    for (MeasurementDataRow row : rows) {
      if (row.hasData()) {
        withData++;
      }
      switch (row.measurementType()) {
        case "GENOMICS" -> genomics.add(row.measurementId());
        case "PROTEOMICS" -> proteomics.add(row.measurementId());
        case "IMMUNOPEPTIDOMICS" -> immunopeptidomics.add(row.measurementId());
        default -> throw new IllegalArgumentException(
            "Unknown measurement type: " + row.measurementType());
      }
    }
    return new MeasurementSelection(genomics, proteomics, immunopeptidomics, withData,
        rows.size());
  }

  /** Whether nothing is selected. */
  public boolean isEmpty() {
    return total == 0;
  }

  /**
   * Whether every selected measurement is free of data and may therefore be deleted.
   *
   * <p>Deletion is blocked by the service for any measurement with attached data, so the page
   * refuses the whole action rather than letting a partial deletion happen and reporting a
   * confusing half-success.</p>
   */
  public boolean isDeletable() {
    return !isEmpty() && withData == 0;
  }
}
