package life.qbic.datamanager.views.general.section;

import com.vaadin.flow.component.html.Div;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class SectionNote extends Div {

  private String content;

  public SectionNote() {
    addClassName("sub-header");
  }

  public SectionNote(String text) {
    this();
    this.content = text;
    add(content);
  }

}
