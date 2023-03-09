package life.qbic.datamanager.views.project.view.components;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.board.Board;
import com.vaadin.flow.component.board.Row;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.ChartType;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoIcon;
import com.vaadin.flow.theme.lumo.LumoUtility.IconSize;
import java.io.Serial;
import life.qbic.datamanager.views.general.ToggleDisplayEditComponent;
import life.qbic.datamanager.views.layouts.CardLayout;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;

/**
 * Project Details Component
 * <p>
 * Shows project details to the user.
 *
 * @since 1.0.0
 */
@SpringComponent
@UIScope
public class ExperimentDetailsComponent extends Composite<CardLayout> {

  @Serial
  private static final long serialVersionUID = -8992991642015281245L;
  private static final String TITLE = "Draft Experiment";
  private final transient Handler handler;
  private static final Logger logger = LoggerFactory.logger(ExperimentDetailsComponent.class);
  private ToggleDisplayEditComponent<Span, TextField, String> experimentNotes;
  private Chart registeredSamples;
  private HorizontalLayout topLayout;
  private HorizontalLayout tagLayout;
  private VerticalLayout detailLayout;
  private TabSheet experimentSheet;
  private Board summaryCardBoard;
  private Board sampleGroupsCardBoard;

  public ExperimentDetailsComponent() {
    this.handler = new Handler();
    getContent().addTitle(TITLE);
    initTopLayout();
    initTabSheet();
  }

  private void initTopLayout() {
    topLayout = new HorizontalLayout();
    registeredSamples = new Chart(ChartType.AREASPLINE);
    initDetailLayout();
    topLayout.add(detailLayout, registeredSamples);
    getContent().addFields(topLayout);
    topLayout.setWidthFull();
  }

  private void initDetailLayout() {
    detailLayout = new VerticalLayout();
    Span tagExample = new Span("CellLines");
    Icon plusIcon = LumoIcon.PLUS.create();
    plusIcon.addClassNames(IconSize.SMALL);
    tagLayout = new HorizontalLayout();
    tagLayout.add(tagExample, plusIcon);
    tagLayout.getChildren()
        .forEach(component -> component.getElement().getThemeList().add("badge"));
    Span noNotesDefined = new Span("Click to add Notes");
    experimentNotes = new ToggleDisplayEditComponent<>(Span::new, new TextField(), noNotesDefined);
    detailLayout.add(tagLayout, experimentNotes);
  }

  private void initTabSheet() {
    experimentSheet = new TabSheet();
    initSummaryCardBoard();
    initSampleGroupsCardBoard();
    experimentSheet.add("Summary", summaryCardBoard);
    experimentSheet.add("Sample Groups", sampleGroupsCardBoard);
    getContent().addFields(experimentSheet);
    experimentSheet.setSizeFull();
  }

  private void initSummaryCardBoard() {
    summaryCardBoard = new Board();
    Row topRow = new Row(new CardLayout(), new CardLayout());
    Row bottomRow = new Row(new CardLayout(), new CardLayout());
    summaryCardBoard.add(topRow, bottomRow);
    summaryCardBoard.setSizeFull();
  }

  private void initSampleGroupsCardBoard() {
    sampleGroupsCardBoard = new Board();
    sampleGroupsCardBoard.setWidthFull();
    //ToDo Fill with Content
  }


  public void setStyles(String... componentStyles) {
    getContent().addClassNames(componentStyles);
  }

  /**
   * Component logic for the {@link ExperimentDetailsComponent}
   */
  private final class Handler {

    public Handler() {

    }

  }
}
