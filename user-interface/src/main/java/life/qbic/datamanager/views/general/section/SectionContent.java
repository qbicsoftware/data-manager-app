package life.qbic.datamanager.views.general.section;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;

/**
 * <b>Section Content</b>
 * <p>
 * The content part of a {@link Section}.
 *
 * @since 1.6.0
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
