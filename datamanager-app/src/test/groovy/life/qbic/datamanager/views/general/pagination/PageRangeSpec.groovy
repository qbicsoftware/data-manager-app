package life.qbic.datamanager.views.general.pagination

import life.qbic.datamanager.views.general.pagination.PageRange.Item
import spock.lang.Specification

class PageRangeSpec extends Specification {

    def "shows a single page for one-page pagination"() {
        expect:
        PageRange.items(1, 1) == [new Item(1, false)]
    }

    def "shows all pages without ellipsis for small totals"() {
        expect:
        PageRange.items(4, 7)*.pageNumber() == [1, 2, 3, 4, 5, 6, 7]
    }

    def "keeps first and last page visible, inserting ellipses for large totals"() {
        expect:
        // ellipsis markers carry pageNumber 0
        PageRange.items(5, 12)*.pageNumber() == [1, 0, 3, 4, 5, 6, 7, 0, 12]
    }

    def "shows the window from the start on the first page"() {
        expect:
        PageRange.items(1, 12)*.pageNumber() == [1, 2, 3, 0, 12]
    }

    def "shows the window towards the end on the last page"() {
        expect:
        PageRange.items(12, 12)*.pageNumber() == [1, 0, 10, 11, 12]
    }

    def "clamps an out-of-range current page into the valid range"() {
        expect:
        PageRange.items(0, 10)*.pageNumber() == [1, 2, 3, 0, 10]
        and:
        PageRange.items(99, 10)*.pageNumber() == [1, 0, 8, 9, 10]
    }

    def "rejects a total page count below one"() {
        when:
        PageRange.items(1, 0)
        then:
        thrown(IllegalArgumentException)
    }
}