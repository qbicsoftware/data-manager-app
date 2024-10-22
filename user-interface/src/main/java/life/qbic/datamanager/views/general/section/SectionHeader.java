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
    this.add(sectionTitle);
    this.add(actionBar);
  }

  public SectionHeader() {
    addClassName("section-header");
  }

  public void setTitle(SectionTitle title) {
    this.sectionTitle = title;
    rebuild();
  }

  public void setActionBar(ActionBar actionBar) {
    this.actionBar = actionBar;
    rebuild();
  }

}
