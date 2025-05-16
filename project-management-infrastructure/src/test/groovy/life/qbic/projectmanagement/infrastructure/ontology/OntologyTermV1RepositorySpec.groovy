package life.qbic.projectmanagement.infrastructure.ontology

import spock.lang.Specification

/**
 * Tests for the sample code service implementation
 *
 * @since 1.0.0
 */
class OntologyTermV1RepositorySpec extends Specification {

    def "Given a one-word search, the correct ontology searchterm is created"() {
        given:
        def searchWord = "Mus"
        def repo = new SpeciesTermRepository(Mock(OntologyTermRepositoryJpaInterface.class))

        when:
        def result = repo.buildSearchTerm(searchWord).replace("  "," ")

        then:
        result.equals('"Mus" < +Mus*')
    }

    def "Given a two-word search, the correct ontology searchterm is created"() {
        given:
        def searchWord = "Mus musc"
        def repo = new SpeciesTermRepository(Mock(OntologyTermRepositoryJpaInterface.class))

        when:
        def result = repo.buildSearchTerm(searchWord).replace("  "," ")

        then:
        result.equals('"Mus musc" < "Mus" < +Mus musc*')
    }

    def "Given a three-word search, the correct ontology searchterm is created"() {
        given:
        def searchWord = "Mus musculus domesticus"
        def repo = new SpeciesTermRepository(Mock(OntologyTermRepositoryJpaInterface.class))

        when:
        def result = repo.buildSearchTerm(searchWord).replace("  "," ")

        then:
        result.equals('"Mus musculus domesticus" < "Mus musculus" < +Mus musculus domesticus*')
    }

}
