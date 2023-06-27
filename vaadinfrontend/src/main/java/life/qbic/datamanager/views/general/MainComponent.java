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

  private final Div content;
  private final Div support;

  protected MainComponent(Div content, Div support) {
    this.content = content;
    this.support = content;
    this.addClassName("main");
    content.addClassName("content");
    support.addClassName("support");
    add(content);
    add(support);
  }

  /**
   * <b>Getter for the {@link com.vaadin.flow.component.Component} functioning as Content component
   * within this abstract component.</b>
   *
   * <p>The content {@link com.vaadin.flow.component.Component} is intended as the component
   * showing the main information within the {@link MainComponent} </p>
   */
  public Div getContent() {
    return content;
  }

  /**
   * <b>Getter for the {@link com.vaadin.flow.component.Component} stored functioning as support
   * component within this abstract component.</b>
   *
   * <p>The content {@link com.vaadin.flow.component.Component} is intended as the component
   * showing additional supporting information(e.g. as a sidebar) within the {@link MainComponent}
   * </p>
   */
  public Div getSupport() {
    return support;
  }
}
