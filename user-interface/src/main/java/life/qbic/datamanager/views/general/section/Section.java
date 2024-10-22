package life.qbic.datamanager.views.general.section;

import com.vaadin.flow.component.html.Div;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class Section extends Div implements ControlElements {

  private Header header;

  private SubHeader subHeader;

  private SectionContent content;

  private Section() {
    addClassName("section");
    header = new Header();
    content = new SectionContent();
    add(header);
    add(content);
  }

  public void setSubHeader(SubHeader subHeader) {
    remove(subHeader);
    this.subHeader = subHeader;
    add(subHeader);
  }

  public void setHeader(Header header) {
    remove(this.header);
    this.header = header;
    add(header);
  }

  public void setContent(SectionContent content) {
    remove(this.content);
    this.content = content;
    add(content);
  }

  @Override
  public void enableControls() {

  }

  @Override
  public void disableControls() {

  }

  @Override
  public boolean controlsEnabled() {
    return false;
  }

  @Override
  public boolean controlsDisabled() {
    return false;
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

    public SectionBuilder withSubHeader(SubHeader subHeader) {
      section.setSubHeader(subHeader);
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

    public SectionBuilder withHeader(Header header) {
      section.setHeader(header);
      return this;
    }


    public Section build() {
      return section;
    }
  }


}
