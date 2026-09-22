package life.qbic.datamanager.views.projects.project.measurements.pagination

import life.qbic.application.commons.SortOrder
import org.springframework.data.domain.Sort
import spock.lang.Specification

class MeasurementSortSpec extends Specification {

    def "default sort is registeredAt descending"() {
        expect:
        MeasurementSort.DEFAULT == new SortOrder("registeredAt", true)
    }

    def "translates a SortOrder into a Spring Data Sort"() {
        expect:
        MeasurementSort.toSpringDataSort(new SortOrder("facility", false), MeasurementDomain.NGS)
            == Sort.by(Sort.Direction.ASC, "facility")
    }

    def "rejects a sort property that is not a valid sort key for the domain"() {
        when:
        MeasurementSort.toSpringDataSort(new SortOrder("bogusField", true), MeasurementDomain.NGS)

        then:
        thrown(IllegalArgumentException)

        when: "an IP-only sort key is rejected for the NGS domain"
        MeasurementSort.toSpringDataSort(new SortOrder("mhcAntibody", true), MeasurementDomain.NGS)

        then:
        thrown(IllegalArgumentException)
    }

    def "allowed sort orders contain the domain sort keys"() {
        expect:
        MeasurementSort.allowedSortOrders(MeasurementDomain.NGS).any { it.propertyName() == "measurementCode" }
        MeasurementSort.allowedSortOrders(MeasurementDomain.IP).any { it.propertyName() == "mhcAntibody" }
        MeasurementSort.allowedSortOrders(MeasurementDomain.PXP).any { it.propertyName() == "technicalReplicateName" }
        MeasurementSort.allowedSortOrders(MeasurementDomain.PXP).any { it.propertyName() == "mhcTypingMethod" } == false
    }

    def "validates IP sort keys for the IP domain"() {
        expect:
        MeasurementSort.toSpringDataSort(new SortOrder("mhcAntibody", true), MeasurementDomain.IP)
            == Sort.by(Sort.Direction.DESC, "mhcAntibody")
    }
}