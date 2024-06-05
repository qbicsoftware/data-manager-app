package life.qbic.datamanager.views.general.footer;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import java.io.Serial;

/**
 * Information Card Component
 * <p>
 * Basic card styled {@link Div} which provides the possibility to add Sections with text to be used
 * in Components with a lot of text without interactivity the {@link LegalNotice}
 */

public class InformationCardComponent extends Div {

  @Serial
  private static final long serialVersionUID = 3458218586395387273L;
  private final Span cardTitle = new Span();

  public InformationCardComponent() {
    cardTitle.addClassName("card-title");
    addClassName("information-card-component");
    add(cardTitle);
  }

  public void setTitle(String title) {
    cardTitle.setText(title);
  }

  public void addSection(String title, Component... components) {
    Div section = new Div();
    section.addClassName("section");
    Span sectionTitle = new Span(title);
    sectionTitle.addClassName("section-title");
    section.addComponentAsFirst(sectionTitle);
    section.add(components);
    add(section);
  }

}
