package life.qbic.datamanager.views.projects.project.info;

import com.vaadin.flow.component.html.Div;

/**
 * <b><Simple Paragraph</b>
 * <p>
 * Truly is what the name suggests: a simple paragraph, which is a div element with a annotated CSS
 * class "simple-paragraph".
 *
 * @since 1.6.0
 */
public class SimpleParagraph extends Div {

  public SimpleParagraph() {
    addClassName("simple-paragraph");
  }

  public SimpleParagraph(String text) {
    this();
    setText(text);
  }

}
