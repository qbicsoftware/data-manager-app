package life.qbic.datamanager.views.general.pagination

import spock.lang.Specification

class SelectionSpec extends Specification {

    def "empty selection has no ids and count zero"() {
        given:
        def selection = new Selection(null as Runnable)

        expect:
        selection.count() == 0
        selection.selectedIds().isEmpty()
        !selection.contains("ID-1")
    }

    def "select adds ids and notifies the change listener"() {
        given:
        def notified = 0
        def selection = new Selection(() -> notified++)

        when:
        selection.select("ID-1")
        selection.select(Set.of("ID-2", "ID-3"))

        then:
        selection.count() == 3
        selection.contains("ID-2")
        notified == 2
    }

    def "selecting an already selected id does not notify again"() {
        given:
        def notified = 0
        def selection = new Selection(() -> notified++)

        when:
        selection.select("ID-1")
        selection.select("ID-1")

        then:
        selection.count() == 1
        notified == 1
    }

    def "deselect removes ids and notifies"() {
        given:
        def notified = 0
        def selection = new Selection(() -> notified++)
        selection.select(Set.of("A", "B", "C"))

        when:
        selection.deselect("A")
        selection.deselect(Set.of("B", "C"))

        then:
        selection.count() == 0
        !selection.contains("A")
        notified == 3 // select(1) + deselect(2)
    }

    def "clear empties the selection and notifies once"() {
        given:
        def notified = 0
        def selection = new Selection(() -> notified++)
        selection.select(Set.of("A", "B"))

        when:
        selection.clear()

        then:
        selection.count() == 0
        notified == 2

        when: "clearing an empty selection does not notify"
        selection.clear()

        then:
        notified == 2
    }

    def "selectedIds returns an unmodifiable snapshot"() {
        given:
        def selection = new Selection(null as Runnable)
        selection.select("A")

        when:
        def snapshot = selection.selectedIds()
        snapshot.add("B")

        then:
        thrown(UnsupportedOperationException)
        selection.count() == 1
    }
}