package life.qbic.datamanager.views.projects.project.experiments

import com.vaadin.flow.data.provider.Query
import life.qbic.projectmanagement.application.ontology.SpeciesLookupInterface
import life.qbic.projectmanagement.application.ontology.SpeciesLookupService
import life.qbic.projectmanagement.application.ontology.TerminologySelect
import life.qbic.projectmanagement.application.ontology.TerminologyService
import life.qbic.projectmanagement.domain.model.OntologyTerm
import spock.lang.Specification

/**
 * The guard that stops a blank or one-letter filter reaching the ontology services.
 *
 * <p>TerminologyService is cross-ontology, so the query term is the only scoping there is: a blank
 * query is not "everything", it is an unbounded scan whose first page means nothing. These tests
 * pin the minimum so the "Type at least two letters to search" copy and the behaviour cannot drift
 * apart again.
 */
class OntologyFilterConnectorSpec extends Specification {

    // Both services validate their collaborator in the constructor, so they are built around a
    // mock of the collaborator rather than mocked themselves.
    def terminologySelect = Mock(TerminologySelect)
    def speciesLookupInterface = Mock(SpeciesLookupInterface)
    def terminologyService = new TerminologyService(terminologySelect)
    def speciesLookupService = new SpeciesLookupService(speciesLookupInterface)

    private static Query<OntologyTerm, String> query(String filter) {
        return new ContractTrackingQuery(filter)
    }

    /**
     * Records whether the connector read the query's bounds. Vaadin's DataCommunicator verifies this
     * per fetch and throws when a callback returns without calling getLimit()/getOffset() -- which
     * is what an early "too short to search" return did. The check is on the calls, not the result,
     * so it must hold even when the connector deliberately yields nothing.
     */
    private static class ContractTrackingQuery extends Query<OntologyTerm, String> {
        final Set<String> calls = new LinkedHashSet<>()

        ContractTrackingQuery(String filter) {
            super(0, 20, List.of(), null, filter)
        }

        @Override
        int getOffset() {
            calls.add("getOffset")
            return super.getOffset()
        }

        @Override
        int getLimit() {
            calls.add("getLimit")
            return super.getLimit()
        }

        boolean boundsWereRead() {
            return calls.contains("getOffset") && calls.contains("getLimit")
        }
    }

    def "a blank filter is not sent to the cross-ontology service"() {
        when:
        def result = OntologyFilterConnector.loadOntologyTerms(query(filter), terminologyService)

        then:
        result.toList().isEmpty()
        0 * terminologySelect.query(*_)

        where:
        filter << [null, "", "   "]
    }

    def "a single letter is not sent to the cross-ontology service"() {
        when:
        def result = OntologyFilterConnector.loadOntologyTerms(query("a"), terminologyService)

        then:
        result.toList().isEmpty()
        0 * terminologySelect.query(*_)
    }

    def "two letters reach the cross-ontology service with the filter intact"() {
        when:
        def result = OntologyFilterConnector.loadOntologyTerms(query("ab"), terminologyService)

        then:
        1 * terminologySelect.query("ab", 0, 20) >> List.of()
        result.toList().isEmpty()
    }

    def "surrounding whitespace does not count towards the minimum"() {
        when:
        OntologyFilterConnector.loadOntologyTerms(query(" a "), terminologyService)

        then:
        0 * terminologySelect.query(*_)
    }

    def "the scoped species lookup is guarded the same way"() {
        when:
        def result = OntologyFilterConnector.loadOntologyTerms(query(""), speciesLookupService)

        then:
        result.toList().isEmpty()
        0 * speciesLookupInterface.query(*_)
    }

    def "the minimum matches the length the form copy promises"() {
        expect:
        OntologyFilterConnector.MINIMUM_QUERY_LENGTH == 2
    }

    def "a too-short filter still reads the query bounds, so the data provider contract holds"() {
        given:
        def shortQuery = new ContractTrackingQuery("a")

        when:
        def result = OntologyFilterConnector.loadOntologyTerms(shortQuery, terminologyService)

        then: "nothing is fetched..."
        result.toList().isEmpty()
        0 * terminologySelect.query(*_)

        and: "...but getLimit() and getOffset() were still called"
        shortQuery.boundsWereRead()
    }

    def "a blank filter also still reads the query bounds"() {
        given:
        def blankQuery = new ContractTrackingQuery("")

        when:
        OntologyFilterConnector.loadOntologyTerms(blankQuery, speciesLookupService)

        then:
        0 * speciesLookupInterface.query(*_)
        blankQuery.boundsWereRead()
    }
}
