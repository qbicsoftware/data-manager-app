package life.qbic.datamanager.views.general.pagination

import spock.lang.Specification

class PaginationBarSpec extends Specification {

    def "can be constructed and updated without a UI context"() {
        given:
        def bar = new PaginationBar(ListStateCodec.ALLOWED_PAGE_SIZES, ListStateCodec.DEFAULT_PAGE_SIZE,
                "projects")

        when: "the bar is given a three-digit total"
        bar.setListState(3, 1000L, 24)

        then:
        noExceptionThrown()
    }

    def "compact bar without a numbered window can be constructed and updated"() {
        given:
        def bar = new PaginationBar(ListStateCodec.ALLOWED_PAGE_SIZES, ListStateCodec.DEFAULT_PAGE_SIZE,
                "projects", false)

        when:
        bar.setListState(2, 500L, 24)

        then:
        noExceptionThrown()
    }

    def "accepts an out-of-range page and clamps it to the last valid page"() {
        given:
        def bar = new PaginationBar([24, 48], 24, "projects")

        when: "99 items at 48 per page leave 3 pages; page 99 is clamped"
        bar.setListState(99, 100L, 48)

        then:
        noExceptionThrown()
    }

    def "rejects a default page size that is not offered"() {
        when:
        new PaginationBar([24, 48], 12, "projects")
        then:
        thrown(IllegalArgumentException)
    }
}