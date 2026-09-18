package life.qbic.datamanager.views.projects.project.measurements

import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.data.provider.SortDirection
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory
import life.qbic.projectmanagement.application.measurement.IpMeasurementLookup
import life.qbic.projectmanagement.application.measurement.NgsMeasurementLookup
import life.qbic.projectmanagement.application.measurement.PxpMeasurementLookup
import org.springframework.context.support.StaticMessageSource
import spock.lang.Specification

import java.time.Instant

/**
 * Pins the sortable grid-column comparators of the measurement lists to the property shown in
 * their header. Regression guard: the "Measurement Name" columns previously sorted by the
 * measurement code (copy-paste), which produced a wrong order for header-driven sorts.
 */
class MeasurementDetailsGridColumnsSpec extends Specification {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z")

    private MeasurementDetailsComponent newComponent() {
        def messageSource = new StaticMessageSource()
        def messages = new MessageSourceNotificationFactory(messageSource)
        return new MeasurementDetailsComponent(messages,
                Stub(NgsMeasurementLookup),
                Stub(PxpMeasurementLookup),
                Stub(IpMeasurementLookup))
    }

    private static NgsMeasurementLookup.MeasurementInfo ngs(String id, String code, String name) {
        return new NgsMeasurementLookup.MeasurementInfo(id, "P1", "E1", code, name, "facility",
                new NgsMeasurementLookup.Organisation("Org", "http://org"),
                new NgsMeasurementLookup.Instrument("Inst", "obo:1", "http://inst"),
                "pool", NOW, "readType", "kit", "cell", "protocol", List.of())
    }

    private static PxpMeasurementLookup.MeasurementInfo pxp(String id, String code, String name) {
        return new PxpMeasurementLookup.MeasurementInfo(id, "P1", "E1", code, name, "facility",
                new PxpMeasurementLookup.Organisation("Org", "http://org"),
                new PxpMeasurementLookup.MsDevice("dev", "obo:1", "http://dev"),
                "pool", NOW,
                "enzyme", "method", "enrichment", 1.0, "labelType", "label", "replicate",
                "lcms", "column", List.of())
    }

    private static IpMeasurementLookup.MeasurementInfo ip(String id, String code, String name) {
        return new IpMeasurementLookup.MeasurementInfo(id, "P1", "E1", code, name, "facility",
                new IpMeasurementLookup.Organisation("Org", "http://org"),
                new IpMeasurementLookup.Instrument("Inst", "obo:1", "http://inst"),
                "pool", NOW, "antibody", "typing", "enrich", "lcms", "column", "acq",
                "range", null, "charge", null, 1.0, 1.0, "cycle", "prep", "run", "comment",
                List.of())
    }

    private static <T> Grid.Column<T> columnByHeader(Grid<T> grid, String header) {
        return grid.getColumns().find { it.getHeaderText() == header }
    }

    def "NGS Measurement Name column orders by measurement name, not code"() {
        given:
        def component = newComponent()
        def grid = component.ngsGrid()
        // name and code order opposite: sorting by code would put a first
        def byCodeFirst = ngs("1", "A", "Zebra")
        def byNameFirst = ngs("2", "Z", "Alpha")
        def column = columnByHeader(grid, "Measurement Name")
        def comparator = column.getComparator(SortDirection.ASCENDING)

        expect:
        comparator.compare(byCodeFirst, byNameFirst) > 0   // "Zebra" > "Alpha" by name
        comparator.compare(byNameFirst, byCodeFirst) < 0
    }

    def "PxP Measurement Name column orders by measurement name, not code"() {
        given:
        def component = newComponent()
        def grid = component.pxpGrid()
        def byCodeFirst = pxp("1", "A", "Zebra")
        def byNameFirst = pxp("2", "Z", "Alpha")
        def column = columnByHeader(grid, "Measurement Name")
        def comparator = column.getComparator(SortDirection.ASCENDING)

        expect:
        comparator.compare(byCodeFirst, byNameFirst) > 0
        comparator.compare(byNameFirst, byCodeFirst) < 0
    }

    def "IP Measurement Name column orders by measurement name"() {
        given:
        def component = newComponent()
        def grid = component.ipGrid()
        def byCodeFirst = ip("1", "A", "Zebra")
        def byNameFirst = ip("2", "Z", "Alpha")
        def column = columnByHeader(grid, "Measurement Name")
        def comparator = column.getComparator(SortDirection.ASCENDING)

        expect:
        comparator.compare(byCodeFirst, byNameFirst) > 0
        comparator.compare(byNameFirst, byCodeFirst) < 0
    }

    def "NGS QBiC Measurement ID column orders by code"() {
        given:
        def component = newComponent()
        def grid = component.ngsGrid()
        def a = ngs("1", "A", "A name")
        def b = ngs("2", "B", "B name")
        def column = columnByHeader(grid, "QBiC Measurement ID")
        def comparator = column.getComparator(SortDirection.ASCENDING)

        expect:
        comparator.compare(a, b) < 0
        comparator.compare(b, a) > 0
    }
}