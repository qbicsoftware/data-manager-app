package life.qbic.datamanager.views.projects.project.dataset

import life.qbic.projectmanagement.application.query.MeasurementDataRow
import life.qbic.projectmanagement.application.query.MeasurementDataRow.DataStatus
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Instant

/**
 * Pins the selection reduction and the deletion guard.
 *
 * <p>These are the rules that decide whether a user may destroy measurement metadata, and both can
 * be wrong without failing loudly. The guard is deliberately strict: the service refuses to delete
 * a measurement whose data is attached, and removal of that data is owned by the data management
 * team. A page that let a mixed selection through would delete part of it and report a
 * half-success, which is the worst outcome for a bulk action — the user believes the sheet was
 * applied.</p>
 */
class MeasurementSelectionSpec extends Specification {

    private static MeasurementDataRow row(String id, String type, boolean withData,
            String poolLabel = null, int sampleCount = 1) {
        return new MeasurementDataRow(id, "exp-1", type, "CODE-" + id, "name", "QBiC", null,
                poolLabel, sampleCount, Instant.EPOCH,
                withData ? DataStatus.AVAILABLE : DataStatus.WAITING,
                withData ? 3L : null, withData ? 4096L : null, Set.of(),
                withData ? Instant.EPOCH : null)
    }

    def "an empty selection is empty"() {
        expect:
        MeasurementSelection.empty().isEmpty()
        MeasurementSelection.of([]).isEmpty()
        MeasurementSelection.of(null).isEmpty()
    }

    def "the selection is reduced to ids per measurement method"() {
        when:
        def selection = MeasurementSelection.of([
                row("a", "GENOMICS", false),
                row("b", "GENOMICS", false),
                row("c", "PROTEOMICS", false),
                row("d", "IMMUNOPEPTIDOMICS", false)])

        then: "each write call needs its own method's ids"
        selection.genomics() == ["a", "b"]
        selection.proteomics() == ["c"]
        selection.immunopeptidomics() == ["d"]
        selection.total() == 4
    }

    def "a selection with no attached data is deletable"() {
        when:
        def selection = MeasurementSelection.of([
                row("a", "GENOMICS", false),
                row("b", "PROTEOMICS", false)])

        then:
        selection.isDeletable()
        selection.withData() == 0
    }

    @Unroll
    def "a selection is not deletable when #withData of #total selected measurements have data"() {
        given: "a mixed selection — the case a partial deletion would silently half-apply"
        def rows = (1..total).collect { index ->
            row("id-${index}", "GENOMICS", index <= withData)
        }

        when:
        def selection = MeasurementSelection.of(rows)

        then: "the whole action is refused rather than partly performed"
        !selection.isDeletable()

        where:
        total | withData
        2     | 1
        3     | 3
        1     | 1
    }

    def "an empty selection is never deletable"() {
        expect:
        !MeasurementSelection.empty().isDeletable()
    }

    def "an unknown measurement type fails loudly instead of being dropped"() {
        when: "a row whose type no deletion call can handle"
        MeasurementSelection.of([row("a", "METABOLOMICS", false)])

        then: "silently ignoring it would leave a selected measurement untouched after a delete"
        thrown(IllegalArgumentException)
    }

    def "deletion is confirmed through the shared danger alert"() {
        given: "the source of the actions that perform deletion"
        // Resolved like the other source specs: this module's tests run from the module directory.
        def candidates = [
                new File("datamanager-app/src/main/java/life/qbic/datamanager/views/projects/project/dataset/MeasurementDatasetActions.java"),
                new File("src/main/java/life/qbic/datamanager/views/projects/project/dataset/MeasurementDatasetActions.java"),
                new File("../datamanager-app/src/main/java/life/qbic/datamanager/views/projects/project/dataset/MeasurementDatasetActions.java")
        ]
        def source = candidates.find { it.exists() }
        assert source != null: "MeasurementDatasetActions.java not found from ${new File('.').absolutePath}"
        def code = source.text
                .replaceAll('(?s)/\\*.*?\\*/', '')
                .replaceAll('(?m)^\\s*//.*$', '')
        def methodStart = code.indexOf("deleteDialog(")
        def method = code.substring(methodStart)

        expect: "a destructive, irreversible action must not look like an ordinary confirmation"
        // A hand-assembled AppDialog gave it a primary-coloured Delete button and no warning icon.
        method.contains("AlertDialog.danger(")

        and: "it must not assemble its own footer, which is what produced the ordinary styling"
        !method.contains("DialogFooter.with(dialog,")

        and: "the message states that the action cannot be undone"
        method.contains("cannot be undone")
    }

    def "the reduced selection is immutable"() {
        given:
        def mutable = new ArrayList<String>(["a"])
        def selection = new MeasurementSelection(mutable, List.of(), List.of(), 0, 1)

        when:
        mutable.add("b")

        then:
        selection.genomics() == ["a"]
    }
}
