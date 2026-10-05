package life.qbic.datamanager.views.projects.project.access

import com.vaadin.flow.router.QueryParameters
import life.qbic.datamanager.views.projects.project.access.ProjectAccessComponent.AccessFilter
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.ProjectRole
import spock.lang.Specification

class AccessRosterStateCodecSpec extends Specification {

    def "parses defaults when no parameters are present"() {
        given:
        def params = QueryParameters.empty()

        when:
        def state = AccessRosterStateCodec.parse(params)

        then:
        state.search() == ""
        state.filter() == AccessFilter.ALL
        state.role().isEmpty()
        state == AccessRosterState.defaults()
    }

    def "parses all parameters from a query string"() {
        given:
        def params = QueryParameters.fromString("q=cancer&filter=groups&role=admin")

        when:
        def state = AccessRosterStateCodec.parse(params)

        then:
        state.search() == "cancer"
        state.filter() == AccessFilter.GROUPS
        state.role().get() == ProjectRole.ADMIN
    }

    def "parses search terms with URL-encoded special characters"() {
        given:
        def params = QueryParameters.fromString("q=John%20Doe%20%26%20Co")

        when:
        def state = AccessRosterStateCodec.parse(params)

        then:
        state.search() == "John Doe & Co"
    }

    def "falls back to defaults for invalid filter and role values"() {
        given:
        def params = QueryParameters.fromString("filter=bogus&role=unknown")

        when:
        def state = AccessRosterStateCodec.parse(params)

        then:
        state.filter() == AccessFilter.ALL
        state.role().isEmpty()
    }

    def "role and filter values are case-insensitive"() {
        given:
        def params = QueryParameters.fromString("filter=PEOPLE&role=WRITE")

        when:
        def state = AccessRosterStateCodec.parse(params)

        then:
        state.filter() == AccessFilter.PEOPLE
        state.role().get() == ProjectRole.WRITE
    }

    def "serialises a state to query parameters and parses back to the identical state"() {
        given:
        def state = new AccessRosterState("cancer research",
            AccessFilter.GROUPS, Optional.of(ProjectRole.ADMIN))

        when:
        def params = AccessRosterStateCodec.toQueryParameters(state)

        then: "all parameters are present and URL-encoded"
        params.getSingleParameter("q").get() == "cancer research"
        params.getSingleParameter("filter").get() == "groups"
        params.getSingleParameter("role").get() == "admin"

        and: "the serialised form parses back to the identical state"
        AccessRosterStateCodec.parse(params) == state
    }

    def "omits default values from the query parameters so the URL stays clean"() {
        given:
        def state = AccessRosterState.defaults()

        when:
        def params = AccessRosterStateCodec.toQueryParameters(state)

        then:
        params.getParameters().isEmpty()

        and: "a state with only a search yields only the q parameter"
        def searchOnly = AccessRosterStateCodec.toQueryParameters(
            new AccessRosterState("cancer", AccessFilter.ALL, Optional.empty()))
        searchOnly.getParameters().keySet() == ["q"] as Set
    }
}