package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.html.Div;

/**
 * <b>The main component</b>
 *
 * <p>The main component function as an abstract class containing the content and support area for
 * individual pages in the data-manager-application</p>
 *
 * @since <1.0.0>
 */

public abstract class MainComponent extends Div {

  private Div content;
  private Div support;

  protected MainComponent(Div content, Div support) {
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
