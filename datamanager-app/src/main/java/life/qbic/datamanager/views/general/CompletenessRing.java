package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import java.io.Serial;

/**
 * A completeness ring: a donut showing the proportion of completed items with the percentage in the
 * centre.
 *
 * <p>The arc and the centre agree — both express <em>coverage</em> (completed / total). The ring never
 * carries the meaning alone: the caller pairs it with a sentence that states the outstanding gap, and
 * the ring exposes {@code role=progressbar} with an {@code aria-valuetext} so assistive technology
 * gets the same fact in words. State is also named by the caller's tag, so colour is reinforcement,
 * never the only signal.</p>
 *
 * @since 1.19.0
 */
public class CompletenessRing extends Div {

  @Serial
  private static final long serialVersionUID = 1L;

  private final Div dial = new Div();
  private final Span centre = new Span();

  /**
   * @param completed     the number of completed items
   * @param total         the total number of items; a value of {@code 0} or less renders the
   *                      "no data" state (a dashed track and an em dash)
   * @param ariaValueText the spoken form, e.g. "264 of 272 measurements still need primary data"
   */
  public CompletenessRing(int completed, int total, String ariaValueText) {
    addClassName("completeness-ring");
    getElement().setAttribute("role", "progressbar");
    getElement().setAttribute("aria-valuemin", "0");
    getElement().setAttribute("aria-valuemax", "100");
    dial.addClassName("completeness-ring__dial");
    centre.addClassName("completeness-ring__centre");
    add(dial, centre);
    apply(completed, total, ariaValueText);
  }

  private void apply(int completed, int total, String ariaValueText) {
    if (total <= 0) {
      addClassName("completeness-ring--none");
      centre.setText("—");
      dial.getElement().setProperty("innerHTML", svg(0, true));
      getElement().setAttribute("aria-valuenow", "0");
      getElement().setAttribute("aria-valuetext", ariaValueText);
      return;
    }
    double ratio = Math.max(0d, Math.min(1d, (double) completed / total));
    int percentage = (int) Math.round(ratio * 100);
    if (ratio >= 1d) {
      addClassName("completeness-ring--complete");
    } else if (ratio <= 0d) {
      addClassName("completeness-ring--empty");
    } else {
      addClassName("completeness-ring--partial");
    }
    centre.setText(percentage + "%");
    dial.getElement().setProperty("innerHTML", svg(percentage, false));
    getElement().setAttribute("aria-valuenow", Integer.toString(percentage));
    getElement().setAttribute("aria-valuetext", ariaValueText);
  }

  /**
   * A donut on a 36×36 viewBox whose circumference is 100 units, so the value can be written
   * directly as a percentage in {@code stroke-dasharray}.
   */
  private static String svg(int percentage, boolean noData) {
    String trackDash = noData ? " stroke-dasharray=\"1.5 3\"" : "";
    String value = noData ? "" : """
        <circle class="completeness-ring__value" cx="18" cy="18" r="15.9155"
                fill="none" stroke-width="3.5" stroke-linecap="round"
                stroke-dasharray="%d 100" transform="rotate(-90 18 18)"/>""".formatted(percentage);
    return """
        <svg class="completeness-ring__svg" viewBox="0 0 36 36" aria-hidden="true" focusable="false">
          <circle class="completeness-ring__track" cx="18" cy="18" r="15.9155"
                  fill="none" stroke-width="3.5"%s/>
          %s
        </svg>""".formatted(trackDash, value);
  }
}
