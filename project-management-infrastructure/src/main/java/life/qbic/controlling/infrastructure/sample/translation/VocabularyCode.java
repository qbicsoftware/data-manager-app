package life.qbic.controlling.infrastructure.sample.translation;

public enum VocabularyCode {
  SPECIES("Q_NCBI_TAXONOMY"),
  SPECIMEN("Q_PRIMARY_TISSUES"),
  ANALYTE("Q_SAMPLE_TYPES");

  private final String openbisCode;

  VocabularyCode(String openbisCode) {
    this.openbisCode = openbisCode;
  }

  public String openbisCode() {
    return openbisCode;
  }
}
