package life.qbic.datamanager.views.general.section;

import com.vaadin.flow.component.html.Div;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class SectionHeader extends Div {

  private SectionTitle sectionTitle;

  private ActionBar actionBar;

  private SectionNote sectionNote;

  private Div headerRow;

  public SectionHeader(SectionTitle sectionTitle, ActionBar actionBar, SectionNote sectionNote) {
    this(sectionTitle, actionBar);
    this.sectionNote = sectionNote;
    rebuild();
  }

  public SectionHeader(SectionTitle sectionTitle, ActionBar actionBar) {
    this();
    this.sectionTitle = sectionTitle;
    this.actionBar = actionBar;
    rebuild();
  }

  public SectionHeader(SectionTitle sectionTitle) {
    this();
    this.sectionTitle = sectionTitle;
    this.actionBar = new ActionBar();
    rebuild();
  }

  private void rebuild() {
    removeAll();
    this.headerRow.removeAll();
    headerRow.add(sectionTitle);
    headerRow.add(actionBar);
    this.add(headerRow);
    this.add(sectionNote);
  }

  public SectionHeader() {
    addClassName("section-header");
    addClassName("normal-trailing-margin");
    this.headerRow = new Div();
    headerRow.addClassName("section-header-row");
    this.sectionTitle = new SectionTitle();
    this.actionBar = new ActionBar();
    this.sectionNote = new SectionNote();
    rebuild();
  }

  public void setTitle(SectionTitle title) {
    this.sectionTitle = title;
    rebuild();
  }

  public void setActionBar(ActionBar actionBar) {
    this.actionBar = actionBar;
    rebuild();
  }

  public void setSectionNote(SectionNote sectionNote) {
    this.sectionNote = sectionNote;
    rebuild();
  }

  public void setSmallTrailingMargin() {
    removeClassName("small-trailing-margin");
    removeClassName("normal-trailing-margin");
    addClassName("small-trailing-margin");
  }

  public void setNormalTrailingMargin() {
    removeClassName("small-trailing-margin");
    removeClassName("normal-trailing-margin");
    addClassName("normal-trailing-margin");
  }

}
