package life.qbic.datamanager.views.general.footer;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import java.io.Serial;
import life.qbic.datamanager.views.general.PageArea;

/**
 * Information Component
 * <p>
 * Basic {@link PageArea} which provides the possibility to add Sections with text to be used in
 * Components with a lot of text without interactivity the {@link Impressum}
 */
public class InformationComponent extends PageArea {

  @Serial
  private static final long serialVersionUID = 7161304802207319605L;
  private final Span title = new Span();
  private int sectionNumber = 0;

  public InformationComponent() {
    title.addClassName("title");
    addComponentAsFirst(title);
    addClassName("information-component");
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
