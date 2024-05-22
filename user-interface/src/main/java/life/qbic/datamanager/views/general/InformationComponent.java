package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import java.io.Serial;

/**
 * <class short description - One Line!>
 * <p>
 * <More detailed description - When to use, what it solves, etc.>
 *
 * @since <version tag>
 */
public class InformationComponent extends PageArea {

  @Serial
  private static final long serialVersionUID = 7161304802207319605L;
  private int sectionNumber = 0;
  private final Span title = new Span();

  public InformationComponent() {
    title.addClassName("title");
    addComponentAsFirst(title);
    addClassName("general-information-component");
  }

  public void setTitle(String title) {
    this.title.setText(title);
  }

  public void addSection(String title, String text) {
    Div section = new Div();
    section.addClassName("section");
    ++sectionNumber;
    Span titleWithNumber = new Span(String.format("%s. %s", sectionNumber, title));
    titleWithNumber.addClassName("section-title");
    section.addComponentAsFirst(titleWithNumber);
    Paragraph paragraph = new Paragraph(text);
    section.add(paragraph);
    add(section);
  }

}
