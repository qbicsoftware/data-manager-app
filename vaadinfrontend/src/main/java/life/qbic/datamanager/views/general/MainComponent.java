package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.html.Div;

/**
 * <class short description - One Line!>
 * <p>
 * <More detailed description - When to use, what it solves, etc.>
 *
 * @since <version tag>
 */

public abstract class MainComponent extends Div {

  private Div content;
  private Div support;

  public MainComponent(Div content, Div support) {
    this.content = content;
    this.support = content;
    content.addClassName("content");
    support.addClassName("support");
    add(content);
    add(support);
  }

  public void setContent(Div content) {
    this.content = content;
  }

  public void setSupport(Div support) {
    this.support = support;
  }

  public Div getContent() {
    return content;
  }

  public Div getSupport() {
    return support;
  }
}
