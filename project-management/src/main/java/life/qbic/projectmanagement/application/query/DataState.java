package life.qbic.projectmanagement.application.query;

/**
 * <b>The one state vocabulary for "how far along is this data".</b>
 *
 * <p>The project page used to describe one condition in five dialects: an amber "Waiting for data",
 * a lavender "Not complete yet", a plain "Not attached", a "25 %" ring and a "Registered" tag. Each
 * was individually defensible and collectively unreadable, because nothing on the screen said they
 * were the same axis at different grains.</p>
 *
 * <p>This enum is that axis. Every surface that reports data completeness derives its state from
 * here, so the word a reader learns in one place holds in the next. The grains stay different --
 * a project, an experiment, an analysis method and a single measurement all report against it --
 * but the vocabulary does not.</p>
 *
 * <p><b>Why {@link #PARTIAL} exists.</b> A two-value model (done / not done) cannot describe a
 * dataset holding 5 of 17 measurements' data. The rail used to mark such a project's stages done
 * because the stage had been reached, which is not the same as finished -- and a green check beside
 * a headline reading "4 of 9" reads as a contradiction. Partial states the middle without colouring
 * it as success or failure.</p>
 *
 * <p><b>Why waiting is not amber.</b> Amber conventionally means warning or at-risk. Missing primary
 * data is the normal, time-dependent state of a measurement that has been registered and not yet
 * uploaded by the facility, so colouring it as a warning spent the strongest colour on the least
 * alarming fact. Amber is reserved for {@link #AT_RISK}, where something genuinely needs a
 * steward's attention.</p>
 *
 * @see AssayStanding
 * @since 1.22.0
 */
public enum DataState {

  /** Every measurement carries primary data. */
  COMPLETE,

  /** Some measurements carry primary data and some do not. */
  PARTIAL,

  /** Measurements exist but none of them carries primary data yet. */
  WAITING,

  /**
   * Something needs a steward's attention -- data that has waited implausibly long, a blocked
   * upload, a measurement whose data cannot be attributed.
   *
   * <p>Reserved. Nothing computes it yet; it exists so that amber has a meaning before one of these
   * cases is modelled, rather than being re-used for "not arrived yet" in the meantime.</p>
   */
  AT_RISK;

  /**
   * The state of {@code withData} out of {@code total} measurements.
   *
   * <p>A total of zero is {@link #WAITING}, not {@link #COMPLETE}: there is nothing to have arrived,
   * and calling it complete would be the vacuous truth that makes an empty project look finished.</p>
   */
  public static DataState of(int withData, int total) {
    if (total <= 0 || withData <= 0) {
      return WAITING;
    }
    return withData >= total ? COMPLETE : PARTIAL;
  }

  /** Whether this state means the data has fully arrived. */
  public boolean isComplete() {
    return this == COMPLETE;
  }

  /**
   * Whether this state means nothing has arrived yet, so the reader is waiting on someone else.
   *
   * <p>Neither complete nor waiting is a fault, which is why neither is coloured as one.</p>
   */
  public boolean isWaiting() {
    return this == WAITING;
  }
}
