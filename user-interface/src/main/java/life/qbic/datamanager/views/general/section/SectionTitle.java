package life.qbic.datamanager.views.general.section;

import com.vaadin.flow.component.html.Div;
import java.util.ArrayList;
import java.util.List;

/**
 * <b>Section Title</b>
 * <p>
 * Is e.g. used in {@link SectionHeader} and formats a section title.
 * <p>
 *
 * <b>Relevant CSS</b>
 * <p>
 * The relevant CSS classes for this component are:
 * <ul>
 * <li><code>section-title</code></li>
 * </ul>
 *
 * @since 1.6.0
 */
public class SectionTitle extends Div {

  private static final List<String> CLASS_NAMES_SIZES = new ArrayList<>();
  private static final String CLASS_NAME = "section-title";

  static {
    CLASS_NAMES_SIZES.add(Size.SMALL.value());
    CLASS_NAMES_SIZES.add(Size.MEDIUM.value());
    CLASS_NAMES_SIZES.add(Size.LARGE.value());
  }

  public SectionTitle() {
    addClassName(CLASS_NAME);
    setToMedium();
  }

  public SectionTitle(String text) {
    this();
    add(text);
  }

  public SectionTitle(String text, Size size) {
    this(text);
    setSize(size);
  }

  private void setToMedium() {
    removeClassNames(CLASS_NAMES_SIZES.toArray(new String[0]));
    addClassName(Size.MEDIUM.value());
  }

  private void setToSmall() {
    removeClassNames(CLASS_NAMES_SIZES.toArray(new String[0]));
    addClassName(Size.SMALL.value());
  }

  private void setToLarge() {
    removeClassNames(CLASS_NAMES_SIZES.toArray(new String[0]));
    addClassName(Size.LARGE.value());
  }

  public void setSize(Size size) {
    switch (size) {
      case SMALL:
        setToSmall();
        break;
      case MEDIUM:
        setToMedium();
        break;
      case LARGE:
        setToLarge();
        break;
    }
  }

  public enum Size {
    SMALL("font-size-small"),
    MEDIUM("font-size-medium"),
    LARGE("font-size-large");

    private final String value;

    Size(String value) {
      this.value = value;
    }

    public String value() {
      return value;
    }
  }
}
