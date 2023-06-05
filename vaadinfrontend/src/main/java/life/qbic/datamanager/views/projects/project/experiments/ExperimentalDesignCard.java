package life.qbic.datamanager.views.projects.project.experiments;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility.Background;
import com.vaadin.flow.theme.lumo.LumoUtility.Border;
import com.vaadin.flow.theme.lumo.LumoUtility.BorderColor;
import com.vaadin.flow.theme.lumo.LumoUtility.BorderRadius;
import com.vaadin.flow.theme.lumo.LumoUtility.BoxShadow;
import com.vaadin.flow.theme.lumo.LumoUtility.Display;
import com.vaadin.flow.theme.lumo.LumoUtility.FontSize;
import com.vaadin.flow.theme.lumo.LumoUtility.IconSize;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin.Right;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin.Top;
import com.vaadin.flow.theme.lumo.LumoUtility.Overflow;
import com.vaadin.flow.theme.lumo.LumoUtility.Padding;
import com.vaadin.flow.theme.lumo.LumoUtility.TextAlignment;
import com.vaadin.flow.theme.lumo.LumoUtility.TextColor;
import com.vaadin.flow.theme.lumo.LumoUtility.TextOverflow;
import com.vaadin.flow.theme.lumo.LumoUtility.Whitespace;
import java.util.List;
import life.qbic.datamanager.views.general.Tag;
import life.qbic.datamanager.views.projects.project.ProjectViewPage;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;

/**
 * <b>Experimental Design Card</b>
 *
 * <p>A PageComponent based Component showing the information stored in the
 * {@link life.qbic.projectmanagement.domain.project.experiment.ExperimentalDesign} associated with
 * a project in the {@link ExperimentListComponent} of the {@link ProjectViewPage}
 */
public class ExperimentalDesignCard extends Composite<HorizontalLayout> {

  private final HorizontalLayout contentLayout = getContent();
  private final VerticalLayout experimentDetailLayout = new VerticalLayout();
  private final Experiment experiment;
  private boolean isComplete = false;
  private final Span experimentStatus = new Span("Incomplete");
  private boolean isActive = false;
  private final Span activeTag = new Span("active");

  public ExperimentalDesignCard(Experiment experiment) {
    this.experiment = experiment;
    initExperimentStatusLayout();
    initExperimentDetailsLayout();
    setCardStyles();
  }

  private void initExperimentStatusLayout() {
    //Writing-mode sideway property is only supported in firefox browser, therefore manual rotation is necessary
    experimentStatus.getStyle().set("rotate", "180deg");
    experimentStatus.getStyle().set("writing-mode", "vertical-lr");
    experimentStatus.addClassName(TextAlignment.CENTER);
    //We want to keep the rounded corner of the card to also apply to only the leftmost corners of the span
    experimentStatus.getStyle()
        .set("border radius", "var(--lumo-border-radius-m) 0 0 var(--lumo-border-radius-m)");
    setComplete(false);
    contentLayout.add(experimentStatus);
  }

  private void initExperimentDetailsLayout() {
    experimentDetailLayout.setMargin(false);
    experimentDetailLayout.setPadding(false);
    initTopRow();
    initBottomRow();
    //Since the experimentStatusWidth is not taking into account we have to allow space for it otherwise the content overflows the container
    experimentDetailLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
    experimentDetailLayout.setWidth(100, Unit.PERCENTAGE);
    experimentDetailLayout.addClassName(Overflow.HIDDEN);
    contentLayout.add(experimentDetailLayout);
  }

  private void initTopRow() {
    HorizontalLayout topRow = new HorizontalLayout();
    Span experimentTitle = new Span(experiment.getName());
    experimentTitle.addClassNames("text-2xl", "font-bold");
    experimentTitle.addClassName(Whitespace.NOWRAP);
    experimentTitle.addClassName(TextOverflow.ELLIPSIS);
    experimentTitle.addClassName(Overflow.HIDDEN);
    experimentTitle.addClassName(Display.INLINE);
    experimentTitle.setTitle(experiment.getName());
    activeTag.getElement().getThemeList().add("badge success primary");
    activeTag.addClassName(BorderRadius.MEDIUM);
    topRow.setAlignItems(Alignment.CENTER);
    topRow.setJustifyContentMode(JustifyContentMode.BETWEEN);
    topRow.setWidthFull();
    topRow.add(experimentTitle, activeTag);
    experimentDetailLayout.add(topRow);
  }

  private void initBottomRow() {
    HorizontalLayout bottomRow = new HorizontalLayout();
    HorizontalLayout tagLayout = new HorizontalLayout();
    List<String> tags = List.of("Label 1", "Label 2");
    tags.forEach(tag -> tagLayout.add(new Tag(tag)));
    tagLayout.getElement().setAttribute("Title", String.join(" ", tags));
    tagLayout.addClassName("spacing-m");
    tagLayout.addClassName(Overflow.HIDDEN);
    tagLayout.addClassName(Whitespace.NOWRAP);
    tagLayout.addClassName(TextOverflow.ELLIPSIS);
    tagLayout.addClassName(Display.INLINE);
    Icon flaskIcon = VaadinIcon.FLASK.create();
    flaskIcon.addClassNames("mt-s", "mb-s");
    flaskIcon.setSize(IconSize.MEDIUM);
    //We need a span to wrap around the icon so the icon stays at the same size if the screen size changes
    Span iconSpan = new Span(flaskIcon);
    bottomRow.setWidthFull();
    bottomRow.setAlignItems(Alignment.CENTER);
    bottomRow.setJustifyContentMode(JustifyContentMode.BETWEEN);
    bottomRow.add(tagLayout, iconSpan);
    experimentDetailLayout.add(bottomRow);
  }

  private void setCardStyles() {
    contentLayout.setMaxHeight(100, Unit.PERCENTAGE);
    contentLayout.setMaxWidth(100, Unit.PERCENTAGE);
    contentLayout.setMargin(false);
    contentLayout.setPadding(false);
    this.getStyle().set("cursor", "pointer");
    this.addClassNames(Background.BASE, Border.ALL, BorderColor.CONTRAST_10, BorderRadius.MEDIUM,
        BoxShadow.SMALL, FontSize.SMALL);
    this.addClassName(Top.SMALL);
    this.addClassName(Right.SMALL);
    this.addClassName(Padding.Right.MEDIUM);
  }

  public void setActive(boolean isActive) {
    this.isActive = isActive;
    activeTag.setVisible(isActive);
  }

  public boolean isActive() {
    return isActive;
  }

  public void setComplete(boolean isComplete) {
    this.isComplete = isComplete;
    if (isComplete) {
      experimentStatus.setText("Complete");
      experimentStatus.addClassNames(Background.PRIMARY, TextColor.PRIMARY_CONTRAST);
    } else {
      experimentStatus.setText("Incomplete");
      experimentStatus.addClassName(Background.CONTRAST_30);
      experimentStatus.getStyle().set("color", "var(--lumo-contrast-100pct)");
    }
  }

  public boolean isComplete() {
    return isComplete;
  }
}
