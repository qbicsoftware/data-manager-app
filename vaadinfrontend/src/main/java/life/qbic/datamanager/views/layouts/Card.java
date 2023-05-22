package life.qbic.datamanager.views.layouts;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

/**
 * <b>Card, A {@link Composite} component containing a {@link Div} hosting the content to be shown
 * on a card within a {@link PageComponent} in the data-manager-app</b>
 */
public class Card extends Composite<Div> {

  private final Span title = new Span("");

  public Card() {
    add(title);
    setDefaultCardStyles();
  }

  public Card(String title, Component... content) {
    this.title.setText(title);
    getContent().add(content);
    add(this.title);
    setDefaultCardStyles();
  }

  public void setTitle(String title) {
    this.title.setText(title);
  }

  public void add(Component... components) {
    getContent().add(components);
  }

  public void remove(Component... components) {
    getContent().remove(components);
  }

  private void setDefaultCardStyles() {
    addClassNames("card");
    getContent().addClassName("content");
    title.addClassName("title");
  }
}
