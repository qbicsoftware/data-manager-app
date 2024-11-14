package life.qbic.datamanager.views.general.section;

import com.vaadin.flow.component.html.Div;

/**
 * <b>Section</b> (div component)
 * <p>
 * A section is divided into two main parts:
 *
 * <ul>
 *   <li>{@link SectionHeader}</li>
 *   <li>{@link SectionContent}</li>
 * </ul>
 * <p>
 * The section header can be used to describe the section's content with a short title. The content
 * itself can be greatly customized, there is no restriction in what clients want to add there.
 * <p>
 * The section merely offers a structured and reusable component to layout header and content
 * in a user-friendly way, based on UI/UX requirements.
 *
 *
 *<p></p>
 * <b>Relevant CSS</b>
 * <p>
 * The relevant CSS classes for this component are:
 *
 * <ul>
 *   <li><code>section</code></li>
 * </ul>
 *
 * @since 1.6.0
 */
public class Section extends Div implements Controllable {

  private SectionContent content;

  private SectionHeader sectionHeader;


  private Section() {
    addClassName("section");
    addClassName("trailing-margin-large");
    sectionHeader = new SectionHeader();
    content = new SectionContent();
    rebuild();
  }

  public void setHeader(SectionHeader sectionHeader) {
    this.sectionHeader = sectionHeader;
    rebuild();
  }

  public void setContent(SectionContent content) {
    this.content = content;
    rebuild();
  }

  public SectionContent content() {
    return content;
  }

  private void rebuild() {
    removeAll();
    add(sectionHeader, content);
  }

  @Override
  public void enableControls() {
    sectionHeader.enableControls();
  }

  @Override
  public void disableControls() {
    sectionHeader.disableControls();
  }

  public static class SectionBuilder {

    private final Section section;

    public SectionBuilder() {
      section = new Section();
      setDefaults();
    }

    private void setDefaults() {
      section.disableControls();
    }

    public SectionBuilder withHeader(SectionHeader sectionHeader) {
      section.setHeader(sectionHeader);
      return this;
    }

    public SectionBuilder disableControls() {
      section.disableControls();
      return this;
    }

    public SectionBuilder enableControls() {
      section.enableControls();
      return this;
    }


    public Section build() {
      return section;
    }
  }


}
