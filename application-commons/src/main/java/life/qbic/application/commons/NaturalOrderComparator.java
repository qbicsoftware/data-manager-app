package life.qbic.application.commons;


import java.math.BigInteger;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <class short description - One Line!>
 * <p>
 * <More detailed description - When to use, what it solves, etc.>
 *
 * @since <version tag>
 *
 */
public final class NaturalOrderComparator implements Comparator<String> {

  private static final Pattern CHUNK = Pattern.compile("(\\d+|\\D+)");

  public static final NaturalOrderComparator CASE_INSENSITIVE = new NaturalOrderComparator(true);
  public static final NaturalOrderComparator CASE_SENSITIVE = new NaturalOrderComparator(false);

  private final boolean caseInsensitive;

  private NaturalOrderComparator(boolean caseInsensitive) {
    this.caseInsensitive = caseInsensitive;
  }

  @Override
  public int compare(String a, String b) {
    if (a == null && b == null) return 0;
    if (a == null) return -1;
    if (b == null) return 1;

    Matcher ma = CHUNK.matcher(a);
    Matcher mb = CHUNK.matcher(b);

    while (ma.find() && mb.find()) {
      int cmp = compareChunk(ma.group(), mb.group());
      if (cmp != 0) return cmp;
    }
    return a.length() - b.length();
  }

  private int compareChunk(String chunkA, String chunkB) {
    boolean bothNumeric = Character.isDigit(chunkA.charAt(0)) && Character.isDigit(chunkB.charAt(0));
    if (bothNumeric) {
      return new BigInteger(chunkA).compareTo(new BigInteger(chunkB));
    }
    return caseInsensitive ? chunkA.compareToIgnoreCase(chunkB) : chunkA.compareTo(chunkB);
  }
}
