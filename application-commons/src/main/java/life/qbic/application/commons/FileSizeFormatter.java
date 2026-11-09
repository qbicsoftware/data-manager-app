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
   * Formats a byte value into a compact, human-readable string for a list cell or a summary line.
   *
   * <p><b>Whole units with an approximation mark, not decimals.</b> A data volume in a table is read
   * to answer "roughly how big is this" -- 300 MB or 4 GB -- and two decimal places imply a precision
   * the sync does not have (it records what the file system reported) while costing the width a
   * column needs for something that actually distinguishes rows. {@code ~300 MB} says the same thing
   * as {@code 300.00 MB} in half the characters and does not overstate accuracy.</p>
   *
   * <p>The approximation mark appears only when the value was actually rounded. An exact value --
   * a whole number of the unit -- is stated without it, so {@code ~1 KB} never claims uncertainty
   * about a figure that is exact. Below one kilobyte the byte count is exact by definition.</p>
   *
   * <p>Precision is kept to three significant digits within the unit: 9.4 MB and 941 MB both read
   * usefully, where rounding to whole MB would turn 0.4 MB into "0 MB".</p>
   *
   * @param bytes the size in bytes
   * @return a compact string such as {@code "300 MB"}, {@code "~9.4 MB"}, {@code "512 B"}
   */
  public static String formatBytesCompact(long bytes) {
    if (bytes < 0) {
      return "0 B";
    }
    if (bytes < KILOBYTE) {
      return bytes + " B";
    }
    long unit;
    String suffix;
    if (bytes < MEGABYTE) {
      unit = KILOBYTE;
      suffix = "KB";
    } else if (bytes < GIGABYTE) {
      unit = MEGABYTE;
      suffix = "MB";
    } else {
      unit = GIGABYTE;
      suffix = "GB";
    }
    double scaled = bytes / (double) unit;
    // One decimal below 100, none at or above it: "9.4 MB" and "941 MB" are both three significant
    // digits, which is as much as a reader uses. Rounding to whole MB throughout would turn a 0.4 MB
    // dataset into "0 MB".
    //
    // Whether the figure was rounded is decided by comparing the stated value against the real one,
    // NOT by inspecting the rendered string. 9 400 000 bytes is 8.96 MB, which renders as "9.0": the
    // string ends in ".0" but the value was rounded, and an exactness test on the text called it
    // exact -- contradicting the mark's whole purpose.
    int decimals = scaled >= 100 ? 0 : 1;
    String number = String.format(Locale.US, "%." + decimals + "f", scaled);
    double stated = Double.parseDouble(number);
    // A trailing ".0" is noise on whole values, so it is dropped either way; the mark then reports
    // whether dropping it hid a rounding.
    boolean whole = number.endsWith(".0");
    if (whole) {
      number = number.substring(0, number.length() - 2);
    }
    boolean rounded = Math.abs(stated * unit - bytes) > 0d;
    return (rounded ? "~" : "") + number + " " + suffix;
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
