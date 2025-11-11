package life.qbic.datamanager.views.general.section;

import com.vaadin.flow.component.html.Div;

/**
 * <b>Section Note</b>
 * <p>
 * Section note as part of a subheader in the context of a {@link Section}.
 *
 * @since 1.6.0
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
