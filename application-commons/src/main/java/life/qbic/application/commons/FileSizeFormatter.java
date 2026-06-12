package life.qbic.application.commons;

import java.util.Locale;

/**
 * Utility class for formatting file sizes in human-readable format.
 * <p>
 * Provides methods to convert byte values to human-readable strings
 * using decimal (SI) units where 1 KB = 1000 B, 1 MB = 1000 KB,
 * and 1 GB = 1000 MB.
 */
public class FileSizeFormatter {

  private static final long base = 1024;
  private static final long KILOBYTE = base;
  private static final long MEGABYTE = base * KILOBYTE;
  private static final long GIGABYTE = base * MEGABYTE;

  private FileSizeFormatter() {
    // Utility class - no instantiation
  }

  /**
   * Formats a byte value into a human-readable string.
   * <p>
   * The value is scaled to the most appropriate decimal unit (B, KB, MB, or GB)
   * and rounded to 2 decimal places.
   *
   * @param bytes the size in bytes
   * @return a formatted string with the appropriate unit (e.g., "3.00 GB", "512.75 MB")
   */
  public static String formatBytes(long bytes) {
    if (bytes < 0) {
      return "0 B";
    }
    if (bytes < KILOBYTE) {
      return bytes + " B";
    }
    if (bytes < MEGABYTE) {
      return String.format(Locale.US, "%.2f KB", bytes / (double) KILOBYTE);
    }
    if (bytes < GIGABYTE) {
      return String.format(Locale.US, "%.2f MB", bytes / (double) MEGABYTE);
    }
    return String.format(Locale.US, "%.2f GB", bytes / (double) GIGABYTE);
  }
}
