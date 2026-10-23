package life.qbic.datamanager.views.projects.create

import life.qbic.datamanager.views.projects.project.experiments.OntologyFilterConnector
import spock.lang.Specification
import spock.lang.Unroll

/**
 * The live hint on the ontology origin fields.
 *
 * <p>The connector deliberately returns nothing below the minimum term length, so the dropdown never
 * opens for a short term. That is correct behaviour but indistinguishable from a broken field unless
 * the field says why — which is what these cover.
 */
class OntologyComboboxFactorySpec extends Specification {

    @Unroll
    def "a term shorter than the minimum gets the typing prompt: '#term'"() {
        given:
        def minimum = OntologyFilterConnector.MINIMUM_QUERY_LENGTH

        expect:
        OntologyComboboxFactory.hintFor(term) == "Type at least ${minimum} letters to search."

        where:
        term << [null, "", "   ", "a", " a "]
    }

    @Unroll
    def "a searchable term gets the neutral hint: '#term'"() {
        expect:
        OntologyComboboxFactory.hintFor(term) == "Start typing — public ontology terms will be suggested."

        where:
        term << ["ab", "DNA", " a b ", "homo sapiens"]
    }

    def "the boundary is exactly the connector's minimum, so the hint cannot contradict the search"() {
        given:
        def minimum = OntologyFilterConnector.MINIMUM_QUERY_LENGTH
        def shortTerm = "x" * (minimum - 1)
        def justLongEnough = "x" * minimum

        expect: "a term one character short is refused and says so"
        OntologyComboboxFactory.hintFor(shortTerm).contains("Type at least")

        and: "the term at exactly the minimum is accepted without complaint"
        !OntologyComboboxFactory.hintFor(justLongEnough).contains("Type at least")
    }
}
