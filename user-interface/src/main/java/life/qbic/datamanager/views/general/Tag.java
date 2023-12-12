package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.html.Span;

public class Tag extends Span {

  public Tag(String text) {
    super(text);
    addClassName("tag");
  }

  public Tag(String text, String toolTip) {
    this(text);
    setTitle(toolTip);
  }
}
