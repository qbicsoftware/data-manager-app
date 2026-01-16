package life.qbic.datamanager.views.projects.project.measurements;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.SortDirection;
import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.OffsetBasedRequest;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.general.grid.component.FilterGrid;
import life.qbic.datamanager.views.general.grid.component.FilterGridTab;
import life.qbic.datamanager.views.general.grid.component.FilterGridTabSheet;
import life.qbic.projectmanagement.infrastructure.experiment.measurement.foobar.NgsMeasurementJpaRepository;
import life.qbic.projectmanagement.infrastructure.experiment.measurement.foobar.NgsMeasurementJpaRepository.NgsMeasurementFilter;
import life.qbic.projectmanagement.infrastructure.experiment.measurement.foobar.NgsMeasurementJpaRepository.NgsMeasurementInformation;
import life.qbic.projectmanagement.infrastructure.experiment.measurement.foobar.PxpMeasurementJpaRepository;
import life.qbic.projectmanagement.infrastructure.experiment.measurement.foobar.PxpMeasurementJpaRepository.PxpMeasurementFilter;
import life.qbic.projectmanagement.infrastructure.experiment.measurement.foobar.PxpMeasurementJpaRepository.PxpMeasurementInformation;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * A component to show detailed information about existing measurements within an experiment.
 */
public class MeasurementDetailsComponentV2 extends PageArea implements Serializable {

  private final FilterGridTabSheet tabSheet;
  private SearchTermFilter ngsSearchTermFilter = SearchTermFilter.empty();
  private SearchTermFilter pxpSearchTermFilter = SearchTermFilter.empty();

  private final NgsMeasurementJpaRepository jpaRepositoryNgs; /*TODO add service in the middle*/
  private final PxpMeasurementJpaRepository jpaRepositoryPxp; /*TODO add service in the middle*/

  /**
   * A filter containing a search term
   *
   * @param searchTerm
   */
  record SearchTermFilter(String searchTerm) {

    static SearchTermFilter empty() {
      return new SearchTermFilter("");
    }

    public SearchTermFilter replaceWith(String searchTerm) {
      return new SearchTermFilter(searchTerm);
    }
  }

  public MeasurementDetailsComponentV2(
      NgsMeasurementJpaRepository jpaRepositoryNgs,
      PxpMeasurementJpaRepository jpaRepositoryPxp) {
    this.jpaRepositoryNgs = requireNonNull(jpaRepositoryNgs);
    this.jpaRepositoryPxp = requireNonNull(jpaRepositoryPxp);
    addClassNames("measurement-details-component");

    //setup tab sheet
    tabSheet = new FilterGridTabSheet();
    tabSheet.showPrimaryFeatureButton();
    tabSheet.setCaptionPrimaryAction("Register Measurements");
    tabSheet.showPrimaryFeatureButton();
    tabSheet.setCaptionFeatureAction("Export");
    add(tabSheet);
  }

  public void setContext(Context context) {
    validateContext(context);
    String projectId = context.projectId().orElseThrow().value(); /*TODO use for service calls*/
    String experimentId = context.experimentId().orElseThrow().value();

    // for each domain, create a grid
    var ngsGrid = createNgsGrid();
    var pxpGrid = createPxpGrid();

    // for each domain, configure a filter grid
    FilterGrid<NgsMeasurementInformation, SearchTermFilter> filterGridNgs = FilterGrid.lazy(
        NgsMeasurementInformation.class,
        SearchTermFilter.class,
        ngsGrid,
        this::getNgsSearchTermFilter,
        query ->
        {
          Optional<String> searchTerm = query.getFilter().map(SearchTermFilter::searchTerm);
          NgsMeasurementFilter ngsMeasurementFilter = configureDatabaseFilterNgs(experimentId,
              searchTerm.orElse(null));
          var pageable = new OffsetBasedRequest(query.getOffset(), query.getLimit(),
              Sort.by(query.getSortOrders().stream().map(
                      s -> s.getDirection() == SortDirection.ASCENDING ? Order.asc(s.getSorted())
                          : Order.desc(s.getSorted()))
                  .toList()));
          List<NgsMeasurementInformation> list = jpaRepositoryNgs.findAll(
              ngsMeasurementFilter.asSpecification(), pageable).getContent();
          return list.stream();
        },
        query ->
        {
          Optional<String> searchTerm = query.getFilter().map(SearchTermFilter::searchTerm);
          NgsMeasurementFilter ngsMeasurementFilter = configureDatabaseFilterNgs(experimentId,
              searchTerm.orElse(null));
          return (int) jpaRepositoryNgs.count(ngsMeasurementFilter.asSpecification());
        },
        (searchTerm, filter) -> filter.replaceWith(searchTerm));

    FilterGrid<PxpMeasurementInformation, SearchTermFilter> filterGridPxp = FilterGrid.lazy(
        PxpMeasurementInformation.class,
        SearchTermFilter.class,
        pxpGrid,
        this::getPxpSearchTermFilter,
        query ->
        {
          Optional<String> searchTerm = query.getFilter().map(SearchTermFilter::searchTerm);
          PxpMeasurementFilter ngsMeasurementFilter = configureDatabaseFilterPxp(experimentId,
              searchTerm.orElse(null));
          var pageable = new OffsetBasedRequest(query.getOffset(), query.getLimit(),
              Sort.by(query.getSortOrders().stream().map(
                      s -> s.getDirection() == SortDirection.ASCENDING ? Order.asc(s.getSorted())
                          : Order.desc(s.getSorted()))
                  .toList()));
          List<PxpMeasurementInformation> list = jpaRepositoryPxp.findAll(
              ngsMeasurementFilter.asSpecification(), pageable).getContent();
          return list.stream();
        },
        query ->
        {
          Optional<String> searchTerm = query.getFilter().map(SearchTermFilter::searchTerm);
          PxpMeasurementFilter ngsMeasurementFilter = configureDatabaseFilterPxp(experimentId,
              searchTerm.orElse(null));
          return (int) jpaRepositoryPxp.count(ngsMeasurementFilter.asSpecification());
        },
        (searchTerm, filter) -> filter.replaceWith(searchTerm));

    //add corresponding tabs in a defined order
    tabSheet.removeAllTabs();

    if (jpaRepositoryNgs.count(new NgsMeasurementFilter(experimentId, "").asSpecification()) > 0) {
      var ngsTab = new FilterGridTab<>("Genomics", filterGridNgs);
      tabSheet.addTab(0, ngsTab);
      tabSheet.addPrimaryAction(ngsTab,
          tab -> System.out.println("registering ngs")); //TODO trigger edit
      tabSheet.addFeatureAction(ngsTab,
          tab -> System.out.println("exporting ngs")); //TODO trigger export
    }

    if (jpaRepositoryPxp.count(new PxpMeasurementFilter(experimentId, "").asSpecification()) > 0) {
      var pxpTab = new FilterGridTab<>("Proteomics", filterGridPxp);
      tabSheet.addTab(1, pxpTab);
      tabSheet.addPrimaryAction(pxpTab,
          tab -> System.out.println("registering pxp")); //TODO trigger edit
      tabSheet.addFeatureAction(pxpTab,
          tab -> System.out.println("exporting pxp")); //TODO trigger export
    }
  }

  private static @NonNull NgsMeasurementFilter configureDatabaseFilterNgs(
      @NonNull String experimentId,
      @Nullable String searchTerm) {
    return new NgsMeasurementFilter(experimentId,
        Optional.ofNullable(searchTerm).orElse(""));
  }

  private static @NonNull PxpMeasurementFilter configureDatabaseFilterPxp(
      @NonNull String experimentId,
      @Nullable String searchTerm) {
    return new PxpMeasurementFilter(experimentId,
        Optional.ofNullable(searchTerm).orElse(""));
  }


  SearchTermFilter getNgsSearchTermFilter() {
    return this.ngsSearchTermFilter;
  }

  SearchTermFilter getPxpSearchTermFilter() {
    return pxpSearchTermFilter;
  }

  private static Grid<NgsMeasurementInformation> createNgsGrid() {
    var ngsGrid = new Grid<NgsMeasurementInformation>();
    ngsGrid.addColumn(NgsMeasurementInformation::measurementCode)
        .setHeader("QBiC Measurement ID")
        .setKey("measurementCode")
        .setComparator(Comparator.comparing(NgsMeasurementInformation::measurementCode))
        .setAutoWidth(true)
        .setResizable(true)
        .setFrozen(true);
    ngsGrid.addColumn(NgsMeasurementInformation::measurementName)
        .setKey("measurementName")
        .setHeader("Measurement Name")
        .setComparator(Comparator.comparing(NgsMeasurementInformation::measurementCode))
        .setAutoWidth(true)
        .setResizable(true);
    //TODO add component column for samples
    ngsGrid.addColumn(info -> info
            .sampleInfos().stream()
            .map(sampleInfo -> "%s (%s)".formatted(sampleInfo.sampleLabel(), sampleInfo.sampleCode()))
            .collect(Collectors.joining(", ")))
        .setHeader("Samples")
        .setSortable(false)
        .setAutoWidth(true)
        .setResizable(true);
    ngsGrid.addColumn(NgsMeasurementInformation::facility)
        .setKey("facility")
        .setHeader("Facility")
        .setComparator(Comparator.comparing(NgsMeasurementInformation::facility))
        .setAutoWidth(true)
        .setResizable(true);
    ngsGrid.addColumn(info -> info.instrument().label() + "(" + info.instrument().oboId() + ")")
        .setKey("instrumentLabel")
        .setHeader("Instrument")
        .setComparator(Comparator.comparing(info -> info.instrument().label()))
        .setAutoWidth(true)
        .setResizable(true);
    ngsGrid.addColumn(info -> info.organisation().label() + "(" + info.organisation().iri() + ")")
        .setKey("organisation")
        .setHeader("Organisation")
        .setComparator(Comparator.comparing(info -> info.organisation().label()))
        .setAutoWidth(true)
        .setResizable(true);
    ngsGrid.addColumn(NgsMeasurementInformation::sequencingReadType)
        .setKey("sequencingReadType")
        .setHeader("Read type")
        .setComparator(Comparator.comparing(NgsMeasurementInformation::sequencingReadType))
        .setAutoWidth(true)
        .setResizable(true);
    ngsGrid.addColumn(NgsMeasurementInformation::libraryKit)
        .setKey("libraryKit")
        .setHeader("Library kit")
        .setComparator(Comparator.comparing(NgsMeasurementInformation::libraryKit))
        .setAutoWidth(true)
        .setResizable(true);
    ngsGrid.addColumn(NgsMeasurementInformation::flowCell)
        .setKey("flowCell")
        .setHeader("Flow cell")
        .setComparator(Comparator.comparing(NgsMeasurementInformation::flowCell))
        .setAutoWidth(true)
        .setResizable(true);
    ngsGrid.addColumn(NgsMeasurementInformation::sequencingRunProtocol)
        .setKey("sequencingRunProtocol")
        .setHeader("Run protocol")
        .setComparator(Comparator.comparing(NgsMeasurementInformation::sequencingRunProtocol))
        .setAutoWidth(true)
        .setResizable(true);
    ngsGrid.addColumn(NgsMeasurementInformation::registeredAt)
        .setKey("registeredAt")
        .setHeader("Registration Date")
        .setComparator(Comparator.comparing(NgsMeasurementInformation::registeredAt))
        .setAutoWidth(true)
        .setResizable(true);
    //TODO add component column for comments
    ngsGrid.addColumn(info -> info
            .sampleInfos().stream()
            .map(sampleInfo -> "%s (%s)".formatted(sampleInfo.sampleLabel(), sampleInfo.comment()))
            .collect(Collectors.joining(", ")))
        .setKey("comment")
        .setHeader("Comment")
        .setSortable(false)
        .setAutoWidth(true)
        .setResizable(true);
    return ngsGrid;
  }

  private static Grid<PxpMeasurementInformation> createPxpGrid() {
    var pxpGrid = new Grid<PxpMeasurementInformation>();
    pxpGrid.addColumn(PxpMeasurementInformation::measurementCode)
        .setHeader("QBiC Measurement ID")
        .setKey("measurementCode")
        .setComparator(Comparator.comparing(PxpMeasurementInformation::measurementCode))
        .setAutoWidth(true)
        .setResizable(true)
        .setFrozen(true);
    //TODO replace with component column
    pxpGrid.addColumn(info -> info
            .sampleInfos().stream()
            .map(sampleInfo -> "%s (%s)".formatted(sampleInfo.sampleLabel(), sampleInfo.sampleCode()))
            .collect(Collectors.joining(", ")))
        .setHeader("Samples")
        .setSortable(false)
        .setAutoWidth(true)
        .setResizable(true);
    pxpGrid.addColumn(PxpMeasurementInformation::measurementName)
        .setKey("measurementName")
        .setHeader("Measurement Name")
        .setComparator(Comparator.comparing(PxpMeasurementInformation::measurementCode))
        .setAutoWidth(true)
        .setResizable(true);
    pxpGrid.addColumn(info -> info.organisation().label() + "(" + info.organisation().iri() + ")")
        .setKey("organisation")
        .setHeader("Organisation")
        .setComparator(Comparator.comparing(info -> info.organisation().label()))
        .setAutoWidth(true)
        .setResizable(true);
    pxpGrid.addColumn(PxpMeasurementInformation::facility)
        .setKey("facility")
        .setHeader("Facility")
        .setComparator(Comparator.comparing(PxpMeasurementInformation::facility))
        .setAutoWidth(true)
        .setResizable(true);
    pxpGrid.addColumn(info -> info.msDevice().label() + "(" + info.msDevice().oboId() + ")")
        .setKey("msDeviceLabel")
        .setHeader("MsDevice")
        .setComparator(Comparator.comparing(info -> info.msDevice().label()))
        .setAutoWidth(true)
        .setResizable(true);
    pxpGrid.addColumn(PxpMeasurementInformation::registeredAt)
        .setKey("registeredAt")
        .setHeader("Registration Date")
        .setComparator(Comparator.comparing(PxpMeasurementInformation::registeredAt))
        .setAutoWidth(true)
        .setResizable(true);
    //TODO replace with component column
    pxpGrid.addColumn(info -> info
            .sampleInfos().stream()
            .map(sampleInfo -> "%s (%s)".formatted(sampleInfo.sampleLabel(), sampleInfo.comment()))
            .collect(Collectors.joining(", ")))
        .setKey("comment")
        .setHeader("Comment")
        .setSortable(false)
        .setAutoWidth(true)
        .setResizable(true);
    return pxpGrid;
  }

  private void validateContext(Context context) throws ContextValidationException {
    if (isNull(context)) {
      throw new ContextValidationException("Context cannot be null");
    }
    context.projectId().orElseThrow(
        () -> new ContextValidationException("Context must contain the project id"));
    context.experimentId().orElseThrow(
        () -> new ContextValidationException("Context must contain the experiment id"));
  }

  private static final class ContextValidationException extends ApplicationException {

    public ContextValidationException(String message) {
      super(message);
    }
  }
}
