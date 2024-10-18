package life.qbic.datamanager.views.projects.project.samples;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.SortOrder;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.general.Tag;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.BatchRegistrationDialog;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.application.sample.SamplePreview;
import life.qbic.projectmanagement.domain.model.batch.Batch;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Sample Details Component
 * <p>
 * Component embedded within the {@link SampleInformationMain}. It allows the user to see the
 * information associated for all {@link Batch} and {@link Sample} of each
 * {@link Experiment within a {@link Project} Additionally it enables the user to register new
 * {@link Batch} and {@link Sample} via the contained {@link BatchRegistrationDialog}.
 */

@SpringComponent
@UIScope
public class SampleDetailsComponent extends PageArea implements Serializable {

  @Serial
  private static final long serialVersionUID = 2893730975944372088L;
  private final Span countSpan;
  private final Grid<SamplePreview> sampleGrid;
  private final transient SampleInformationService sampleInformationService;
  private Context context;

  public SampleDetailsComponent(@Autowired SampleInformationService sampleInformationService) {
    this.sampleInformationService = Objects.requireNonNull(sampleInformationService,
        "SampleInformationService cannot be null");
    addClassName("sample-details-component");
    sampleGrid = createSampleGrid();
    Div content = new Div();
    content.addClassName("sample-details-content");
    countSpan = new Span();
    countSpan.addClassName("sample-count");
    setSampleCount(0);
    content.add(countSpan, sampleGrid);
    add(content);
  }

  private static ComponentRenderer<Div, SamplePreview> createConditionRenderer() {
    return new ComponentRenderer<>(SampleDetailsComponent::createTagCollection,
        SampleDetailsComponent::fillTagCollection);
  }

  private static void fillTagCollection(Div div, SamplePreview samplePreview) {
    samplePreview
        .experimentalGroup()
        .condition()
        .getVariableLevels().stream()
        .map(variableLevel -> "%s: %s %s".formatted(variableLevel.variableName().value(),
                variableLevel.experimentalValue().value(),
                variableLevel.experimentalValue().unit().orElse(""))
            .trim())
        .map(s -> {
          Tag tag = new Tag(s);
          tag.setTitle(s);
          return tag;
        })
        .forEach(div::add);
  }

  private static Div createTagCollection() {
    Div tagCollection = new Div();
    tagCollection.addClassName("tag-collection");
    return tagCollection;
  }

  private static Grid<SamplePreview> createSampleGrid() {
    Grid<SamplePreview> sampleGrid = new Grid<>(SamplePreview.class);
    sampleGrid.addColumn(SamplePreview::sampleCode)
        .setHeader("Sample ID")
        .setSortProperty("sampleCode")
        .setAutoWidth(true)
        .setFlexGrow(0)
        .setTooltipGenerator(SamplePreview::sampleCode)
        .setFrozen(true);
    sampleGrid.addColumn(SamplePreview::sampleName)
        .setHeader("Sample Name")
        .setSortProperty("sampleName")
        .setTooltipGenerator(SamplePreview::sampleName)
        .setAutoWidth(true)
        .setResizable(true);
    sampleGrid.addColumn(SamplePreview::biologicalReplicate)
        .setHeader("Biological Replicate")
        .setSortProperty("biologicalReplicate")
        .setTooltipGenerator(SamplePreview::biologicalReplicate)
        .setAutoWidth(true)
        .setResizable(true);
    sampleGrid.addColumn(SamplePreview::batchLabel)
        .setHeader("Batch")
        .setSortProperty("batchLabel")
        .setTooltipGenerator(SamplePreview::batchLabel)
        .setAutoWidth(true)
        .setResizable(true);
    sampleGrid.addColumn(createConditionRenderer())
        .setHeader("Condition")
        .setSortProperty("experimentalGroup")
        .setAutoWidth(true)
        .setResizable(true);
    sampleGrid.addColumn(preview -> preview.species().getLabel())
        .setHeader("Species")
        .setSortProperty("species")
        .setTooltipGenerator(preview -> preview.species().formatted())
        .setAutoWidth(true)
        .setResizable(true);
    sampleGrid.addColumn(preview -> preview.specimen().getLabel())
        .setHeader("Specimen")
        .setSortProperty("specimen")
        .setTooltipGenerator(preview -> preview.specimen().formatted())
        .setAutoWidth(true);
    sampleGrid.addColumn(preview -> preview.analyte().getLabel())
        .setHeader("Analyte")
        .setSortProperty("analyte")
        .setTooltipGenerator(preview -> preview.analyte().formatted())
        .setAutoWidth(true)
        .setResizable(true);
    sampleGrid.addColumn(preview -> preview.analysisMethod().label())
        .setHeader("Analysis to Perform")
        .setSortProperty("analysisMethod")
        .setTooltipGenerator(samplePreview -> samplePreview.analysisMethod().label())
        .setAutoWidth(true)
        .setResizable(true);
    sampleGrid.addColumn(SamplePreview::comment)
        .setHeader("Comment")
        .setSortProperty("comment")
        .setTooltipGenerator(SamplePreview::comment)
        .setAutoWidth(true)
        .setResizable(true);
    sampleGrid.addClassName("sample-grid");
    sampleGrid.setColumnReorderingAllowed(true);
    return sampleGrid;
  }

  public void onSearchFieldValueChanged(String searchValue) {
    updateSampleGridDataProvider(context.experimentId().orElseThrow(), searchValue);
  }

  private void updateSampleGridDataProvider(ExperimentId experimentId, String filter) {
    sampleGrid.setItems(query -> {
      List<SortOrder> sortOrders = query.getSortOrders().stream().map(
              it -> new SortOrder(it.getSorted(), it.getDirection().equals(SortDirection.ASCENDING)))
          .collect(Collectors.toList());
      // if no order is provided by the grid order by last modified (least priority)
      sortOrders.add(SortOrder.of("sampleCode").ascending());
      return sampleInformationService.queryPreview(experimentId, query.getOffset(),
          query.getLimit(), List.copyOf(sortOrders), filter).stream();
    });
    sampleGrid.getLazyDataView().addItemCountChangeListener(
        countChangeEvent -> setSampleCount((int) sampleGrid.getLazyDataView().getItems().count()));
    sampleGrid.recalculateColumnWidths();
  }

  /**
   * Propagates the context to internal components.
   *
   * @param context the context in which the user is.
   */
  public void setContext(Context context) {
    if (context.experimentId().isEmpty()) {
      throw new ApplicationException("no experiment id in context " + context);
    }
    if (context.projectId().isEmpty()) {
      throw new ApplicationException("no project id in context " + context);
    }
    this.context = context;
    updateSampleGridDataProvider(this.context.experimentId().orElseThrow(), "");
  }

  private void setSampleCount(int i) {
    countSpan.setText(i + " samples");
  }

}
