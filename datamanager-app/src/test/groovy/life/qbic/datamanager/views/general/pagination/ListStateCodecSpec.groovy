package life.qbic.datamanager.views.general.pagination

import com.vaadin.flow.router.QueryParameters
import life.qbic.application.commons.SortOrder
import spock.lang.Specification

class ListStateCodecSpec extends Specification {

    static final SortOrder DEFAULT_SORT = new SortOrder("lastModified", true)

    static final List<SortOrder> ALLOWED_SORTS = [
            new SortOrder("lastModified", true),
            new SortOrder("projectTitle", false),
            new SortOrder("projectCode", false),
    ]

    def "parses defaults when no parameters are present"() {
        given:
        def params = QueryParameters.empty()

        when:
        def state = ListStateCodec.parse(params, DEFAULT_SORT, ALLOWED_SORTS)

        then:
        state.page() == 1
        state.pageSize() == ListStateCodec.DEFAULT_PAGE_SIZE
        state.filter() == ""
        state.sort() == DEFAULT_SORT
    }

    def "parses all parameters from a query string"() {
        given:
        def params = QueryParameters.fromString("page=3&size=48&q=cancer&sort=projectTitle:asc")

        when:
        def state = ListStateCodec.parse(params, DEFAULT_SORT, ALLOWED_SORTS)

        then:
        state.page() == 3
        state.pageSize() == 48
        state.filter() == "cancer"
        state.sort() == new SortOrder("projectTitle", false)
    }

    def "falls back to defaults for invalid page, size and sort values"() {
        given:
        def params = QueryParameters.fromString("page=0&size=7&sort=bogusField:desc")

        when:
        def state = ListStateCodec.parse(params, DEFAULT_SORT, ALLOWED_SORTS)

        then:
        state.page() == 1
        state.pageSize() == ListStateCodec.DEFAULT_PAGE_SIZE
        state.filter() == ""
        state.sort() == DEFAULT_SORT
    }

    def "sort values not among the allowed sorts fall back to the default sort"() {
        given:
        // projectCode descending is allowed for the projects list, lastModified ascending is not
        def params = QueryParameters.fromString("sort=lastModified:asc")

        when:
        def state = ListStateCodec.parse(params, DEFAULT_SORT, ALLOWED_SORTS)

        then:
        state.sort() == DEFAULT_SORT
    }

    def "serialises a state to query parameters and parses back to the identical state"() {
        given:
        def state = new ListState(2, 24, "cancer", new SortOrder("lastModified", true))

        when:
        def params = ListStateCodec.toQueryParameters(state)

        then: "all parameters are present and URL-decoded"
        params.getSingleParameter("page").get() == "2"
        params.getSingleParameter("size").get() == "24"
        params.getSingleParameter("q").get() == "cancer"
        params.getSingleParameter("sort").get() == "lastModified:desc"
        and: "the serialised form parses back to the identical state"
        ListStateCodec.parse(params, DEFAULT_SORT, ALLOWED_SORTS) == state
    }

    def "omits a blank filter from the query parameters"() {
        given:
        def params = ListStateCodec.toQueryParameters(new ListState(1, 24, "", DEFAULT_SORT))

        expect:
        !params.getParameters().containsKey("q")
        and:
        params.getSingleParameter("page").get() == "1"
        and:
        params.getSingleParameter("sort").get() == "lastModified:desc"
    }

    def "rejects states with invalid page or page size"() {
        when:
        new ListState(0, 24, "", DEFAULT_SORT)
        then:
        thrown(IllegalArgumentException)

        when:
        new ListState(1, 0, "", DEFAULT_SORT)
        then:
        thrown(IllegalArgumentException)
    }
}