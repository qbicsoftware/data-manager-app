package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.html.Span;

public class Tag extends Span {

  public Tag(String text) {
    super(text);
    setClassName("tag");
    addClassName(TagColor.PRIMARY.getColor());
  }

  public Tag(String text, String toolTip) {
    this(text);
    setTitle(toolTip);
  }

  public void setTagColor(TagColor tagColor) {
    setClassName("tag");
    addClassName(tagColor.getColor());
  }

  /**
   * Tag color enum is used to set the tag color to one of the predefined values to allow different
   * coloration of tags as necessary
   */
  public enum TagColor {
    ERROR("error"),
    PRIMARY("primary"),
    SUCCESS("success"),
    WARNING("warning"),
    VIOLET("violet"),
    PINK("pink"),
    CONTRAST("contrast");

    private final String color;

    TagColor(String color) {
      this.color = color;
    }

    public String getColor() {
      return color;
    }
  }
}
