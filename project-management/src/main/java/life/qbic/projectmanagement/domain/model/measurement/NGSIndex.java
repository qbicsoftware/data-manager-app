package life.qbic.projectmanagement.domain.model.measurement;

/**
 * <b>NGS Index</b>
 *
 * <p>Dual Indices employed for pooled measurements during Multiplexing in the library prep
 * process.
 * Multiplexing allows large numbers of libraries to be pooled and sequenced simultaneously during a
 * single run on an instrument. In the NGS context a dual indices system is employed in pooled
 * samples consisting of the indices I5 and I7 in the following combinations: Double indexing i5 +
 * i7 Single indexing i7 A pooled measurement always must have at least one of the index
 * combinations, meaning a singular index of i5 is NOT possible and the index is irrelevant in a
 * non-pooled context. Additional information can be found at <a
 * href="https://www.illumina.com/techniques/sequencing/ngs-library-prep/multiplexing.html">Multiplexing</a>
 */
public record NGSIndex(String indexI5, String indexI7) {

  public static NGSIndex doubleIndexing(String indexI5, String indexI7) {
    return new NGSIndex(indexI5, indexI7);
  }

  public static NGSIndex singleIndexing(String indexI7) {
    return new NGSIndex("", indexI7);
  }
}
