package life.qbic.datamanager.views.general.section;

import com.vaadin.flow.component.html.Div;
import java.util.ArrayList;
import java.util.List;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class Header extends Div {

  private static final List<String> CLASS_NAMES_SIZES = new ArrayList<>();
  private static final String CLASS_NAME = "header";

  static {
    CLASS_NAMES_SIZES.add(Size.SMALL.value());
    CLASS_NAMES_SIZES.add(Size.MEDIUM.value());
    CLASS_NAMES_SIZES.add(Size.LARGE.value());
  }

  public Header() {
    addClassName(CLASS_NAME);
    setToMedium();
  }

  public Header(String text) {
    this();
    add(text);
  }

  public Header(String text, Size size) {
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
      case SMALL: setToSmall();
      case MEDIUM: setToMedium();
      case LARGE: setToLarge();
    }
  }

  public enum Size {
    SMALL("small-size"),
    MEDIUM("medium-size"),
    LARGE("large-size");

    private final String value;

    Size(String value) {
      this.value = value;
    }

    public String value() {
      return value;
    }
  }
}
