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

  private SectionContent content;

  private SectionHeader sectionHeader;


  private Section() {
    addClassName("section");
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

  private void rebuild() {
    removeAll();
    add(sectionHeader, content);
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
