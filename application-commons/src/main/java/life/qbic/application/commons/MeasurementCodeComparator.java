package life.qbic.application.commons;

import java.util.Comparator;

/**
 * Compares measurement codes of the form {@code <Code>-<Timestamp>} (e.g.
 * {@code NGSQ2RGU6004A0-627616355705691}): the opaque code portion is compared first,
 * and the numeric timestamp is used as a tie-breaker for re-measurements of the same
 * sample
 *
 */
public final class MeasurementCodeComparator implements Comparator<String> {

  public static final MeasurementCodeComparator INSTANCE = new MeasurementCodeComparator();

  private MeasurementCodeComparator() {}

  @Override
  public int compare(String a, String b) {
    if (a == null && b == null) return 0;
    if (a == null) return -1;
    if (b == null) return 1;

    int hyphenA = a.lastIndexOf('-');
    int hyphenB = b.lastIndexOf('-');

    String codeA = hyphenA >= 0 ? a.substring(0, hyphenA) : a;
    String codeB = hyphenB >= 0 ? b.substring(0, hyphenB) : b;

    int codeCmp = codeA.compareTo(codeB);
    if (codeCmp != 0) {
      return codeCmp;
    }

    String timestampA = hyphenA >= 0 ? a.substring(hyphenA + 1) : "";
    String timestampB = hyphenB >= 0 ? b.substring(hyphenB + 1) : "";
    if (timestampA.isEmpty() || timestampB.isEmpty()) {
      return timestampA.length() - timestampB.length();
    }
    return Long.compare(Long.parseLong(timestampA), Long.parseLong(timestampB));
  }
}
