package life.qbic.datamanager.views.projects.project.dataset

import spock.lang.Specification
import spock.lang.Unroll

/**
 * Guards the Dataset page against touching the UI from a reactive callback.
 *
 * <p>Reactor delivers a result on the thread that produced it, which for this application is the
 * shared elastic scheduler — never a Vaadin UI thread. Almost everything the page does with a
 * result touches the UI: opening a toast, triggering a download (which assigns an {@code href} and
 * therefore needs a current {@code UI}), and rebuilding the page after a write. All of those fail at
 * runtime with <em>"No currently active UI found"</em>, and only when the code path is actually
 * exercised.</p>
 *
 * <p>This defect was shipped twice: once in the metadata export, where the download trigger ran on
 * the scheduler thread, and once in the post-write reload. It cannot be caught at compile time, and
 * a unit test would have to boot Vaadin to reproduce it, so the rule is checked against the source:
 * a {@code subscribe} callback in these files must reach the UI through the project's adapter
 * ({@code UiHandle} or {@code ReactiveUiBridge}), not directly.</p>
 *
 * <p>Blocking calls ({@code blockOptional}) are exempt: they run on the caller's thread, which is
 * the UI thread, so no hop is involved.</p>
 */
class MeasurementDatasetThreadingSpec extends Specification {

    /** The page files that consume reactive results. */
    private static final List<String> CONSUMERS = [
            "MeasurementDatasetMain.java",
            "MeasurementDatasetDetailsComponent.java",
            "MeasurementDatasetActions.java",
            "MeasurementDataDownloadGuide.java"
    ]

    private static File sourceFile(String name) {
        def candidates = [
                new File("datamanager-app/src/main/java/life/qbic/datamanager/views/projects/project/dataset"),
                new File("src/main/java/life/qbic/datamanager/views/projects/project/dataset"),
                new File("../datamanager-app/src/main/java/life/qbic/datamanager/views/projects/project/dataset")
        ]
        def dir = candidates.find { it.isDirectory() }
        assert dir != null: "dataset view package not found from ${new File('.').absolutePath}"
        def file = new File(dir, name)
        assert file.exists(): "${name} not found"
        return file
    }

    def "the page re-enters the UI thread through the shared adapter"() {
        given:
        def page = sourceFile("MeasurementDatasetMain.java").text

        expect: "a page that consumes reactive results must own the adapter and bind it"
        page.contains("UiHandle")
        page.contains("uiHandle.bind(")

        and: "and must release it on detach, so a dead session is not written to"
        page.contains("uiHandle.unbind()")
    }

    def "no reactive callback touches the UI without the adapter"() {
        given: "every consumer that subscribes to a reactive source"
        def offenders = []
        CONSUMERS.each { name ->
            def text = sourceFile(name).text
            if (!text.contains(".subscribe(")) {
                return
            }
            // The download guide and the selection record hold no reactive source; a file that
            // subscribes must route its UI work through the adapter.
            boolean declaredUiFreeElsewhere =
                    text.contains("PERFORMS_NO_UI_WORK")
            if (!text.contains("UiHandle") && !text.contains("ReactiveUiBridge")
                    && !declaredUiFreeElsewhere) {
                offenders << name
            }
        }

        expect: "a subscribe without the adapter means UI work on the scheduler thread"
        offenders.isEmpty()
    }

    def "the export hands its result to the adapter rather than to the download component"() {
        given:
        def page = sourceFile("MeasurementDatasetMain.java").text

        expect: "triggerDownload assigns an href, so it must run on the UI thread"
        page.contains("uiHandle.onUiAndPush(")

        and: "the download trigger is not reachable straight from the reactive consumer"
        !page.contains("actions.exportMetadata(selection, digitalObject -> {")
    }

    @Unroll
    def "the async sample fetch in #name delivers through the adapter"() {
        given:
        def text = sourceFile(name).text

        expect: "the detail panel's sample listing is fetched asynchronously, so its result must"
        // be re-entered onto the UI thread; rendering it directly would fail exactly like the
        // export did.
        !text.contains("queryService.getMeasuredSamples") ||
                (text.contains("uiHandle.onUi(") || text.contains("ReactiveUiBridge"))

        where:
        name << ["MeasurementDatasetDetailsComponent.java"]
    }

    def "every consumer file either blocks synchronously or uses the adapter"() {
        expect: "there is no third way of handling a reactive result in these views"
        CONSUMERS.every { name ->
            def text = sourceFile(name).text
            def subscribes = text.contains(".subscribe(")
            def usesAdapter = text.contains("UiHandle") || text.contains("ReactiveUiBridge")
                    || text.contains("PERFORMS_NO_UI_WORK")
            !subscribes || usesAdapter
        }
    }
}
