package life.qbic.datamanager.views.general.section;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class SectionContent extends Div {

  public SectionContent() {
    addClassName("section-content");
  }

  public SectionContent(Component... components) {
    this();
    add(components);
  }

}
