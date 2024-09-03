package life.qbic.projectmanagement.infrastructure

import spock.lang.Shared
import spock.lang.Specification

class CachedOrganisationRepositorySpec extends Specification {

    @Shared
    CachedOrganisationRepository cachedOrganisationRepository = new CachedOrganisationRepository();

    def setup() {
        cachedOrganisationRepository.resolve("https://ror.org/03a1kwz48")
        cachedOrganisationRepository.resolve("https://ror.org/00v34f693")
    }

    def "Given a ROR IRI with valid ROR id, resolve the correct organisation"() {
        given:
        def cachedRepoInstance = new CachedOrganisationRepository()

        when:
        def result = cachedRepoInstance.resolve(rorIri)

        then:
        result.isPresent()
        result.get().label().matches(organisationName)
        !cachedRepoInstance.cacheUsedForLastRequest()



        where:
        rorIri | organisationName
        "https://ror.org/03a1kwz48" | "University of Tübingen"
        "https://ror.org/00v34f693" | "Quantitative Biology Center"

    }

    def "Given subsequent queries of the sample ror id, use the cache"() {
        when:
        def result = cachedOrganisationRepository.resolve(rorIri)

        then:
        result.isPresent()
        result.get().label().matches(organisationName)
        cachedOrganisationRepository.cacheEntries() == 2
        cachedOrganisationRepository.cacheUsedForLastRequest()

        where:
        rorIri | organisationName
        "https://ror.org/03a1kwz48" | "University of Tübingen"
        "https://ror.org/03a1kwz48" | "University of Tübingen"
        "https://ror.org/03a1kwz48" | "University of Tübingen"
        "https://ror.org/03a1kwz48" | "University of Tübingen"
        "https://ror.org/00v34f693" | "Quantitative Biology Center"
        "https://ror.org/00v34f693" | "Quantitative Biology Center"
        "https://ror.org/00v34f693" | "Quantitative Biology Center"
        "https://ror.org/00v34f693" | "Quantitative Biology Center"
        "https://ror.org/00v34f693" | "Quantitative Biology Center"
    }

    def "Given an unknown ROR IRI, return an empty result"() {
        given:
        def cachedRepoInstance = new CachedOrganisationRepository()

        when:
        def result = cachedRepoInstance.resolve( "https://ror.org/00v3223")

        then:
        result.isEmpty()
    }

    def "Given a full cache, free a slot and write the new entry"() {
        given:
        def singularRepoInstance = new CachedOrganisationRepository(1)
        singularRepoInstance.resolve("https://ror.org/03a1kwz48")

        and: // we override the cache entry since the size is 1
        singularRepoInstance.resolve("https://ror.org/00v34f693")

        when: // we search the first ROR entry again
        singularRepoInstance.resolve("https://ror.org/03a1kwz48")

        then: // the search did not read from the cache
        !singularRepoInstance.cacheUsedForLastRequest()
    }
}
