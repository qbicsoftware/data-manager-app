package life.qbic.datamanager.views.general.section;

import com.vaadin.flow.component.html.Div;

/**
 * <b>Section Header</b> (div component)
 * <p>
 * A section header is usually part of a {@link Section} and contains of the components:
 *
 * <ul>
 *   <li>{@link SectionTitle}</li>
 *   <li>{@link ActionBar}</li>
 *   <li>{@link SectionNote}</li>
 * </ul>
 * <p>
 * This combination offers a usable header for a section with a concise and pre-formatted
 * title, and action bar with interactive elements that are related to possible interaction
 * of the user related to the content referenced in the section.
 * <p>
 * Since the action bar might need to be enabled or disabled by the client, the section header
 * implements the {@link Controllable} interface to show this behaviour.
 *
 * <p></p>
 * <b>Relevant CSS</b>
 * <p>
 * The relevant CSS classes for this component are:
 *  <ul>
 *  <li><code>section-header</code></li>
 *  <li><code>trailing-margin-normal</code></li>
 *  <li><code>section-header-row</code></li>
 * </ul>
 *
 * @since 1.6.0
 */
public class SectionHeader extends Div implements Controllable {

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

  public SectionHeader() {
    addClassName("section-header");
    addClassName("trailing-margin-normal");
    this.headerRow = new Div();
    headerRow.addClassName("section-header-row");
    this.sectionTitle = new SectionTitle();
    this.actionBar = new ActionBar();
    this.sectionNote = new SectionNote();
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
    removeClassName("trailing-margin-normal");
    removeClassName("trailing-margin-normal");
    addClassName("trailing-margin-small");
  }

  public void setNormalTrailingMargin() {
    removeClassName("trailing-margin-small");
    removeClassName("trailing-margin-normal");
    addClassName("trailing-margin-normal");
  }

  @Override
  public void enableControls() {
    actionBar.activateAllControls();
  }

  @Override
  public void disableControls() {
    actionBar.deactivateAllControls();
  }
}
