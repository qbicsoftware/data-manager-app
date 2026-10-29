package life.qbic.datamanager.views.projects.project.dataset

import spock.lang.Specification

/**
 * Guards the two rules that produced a 404 and two dead export buttons.
 *
 * <p>Neither defect is visible at compile time, and both fail only in a deployed application:
 * </p>
 *
 * <ul>
 *   <li><b>Routes are resolved by the router.</b> The token link was an
 *       {@code Anchor} whose href was concatenated by hand
 *       ({@code "../settings/api-tokens"}). The application is served under a servlet context
 *       path ({@code server.servlet.context-path}), so a relative href resolved against the
 *       current route produced a 404. A {@code RouterLink} asks the router for the URL and is
 *       therefore context-path correct by construction.</li>
 *   <li><b>A download anchor must sit inside the modal dialog.</b> {@code AppDialog} uses
 *       {@code ModalityMode.STRICT}, so an anchor rendered in the page behind it is inert: the
 *       synthetic click never reaches it and nothing downloads. Every working
 *       download-from-dialog component in the application keeps its anchor in the dialog for
 *       exactly this reason, and the dataset dialog did not.</li>
 * </ul>
 */
class MeasurementDatasetNavigationAndDownloadSpec extends Specification {

    private static File viewPackage() {
        def candidates = [
                new File("datamanager-app/src/main/java/life/qbic/datamanager/views/projects/project/dataset"),
                new File("src/main/java/life/qbic/datamanager/views/projects/project/dataset"),
                new File("../datamanager-app/src/main/java/life/qbic/datamanager/views/projects/project/dataset")
        ]
        def dir = candidates.find { it.isDirectory() }
        assert dir != null: "dataset view package not found from ${new File('.').absolutePath}"
        return dir
    }

    private static String sourceOf(String name) {
        def file = new File(viewPackage(), name)
        assert file.exists(): "${name} not found"
        return file.text
    }

    /**
     * The source with comments removed.
     *
     * <p>These rules are about code, and the comments in these files deliberately quote the
     * bad patterns to explain why they were wrong — a plain text search matches its own
     * documentation.</p>
     */
    private static String codeOf(String name) {
        // Java block comments first, then line comments. Written with string patterns rather than
        // slashy strings, because a block-comment regex inside /.../ needs escaping that Groovy
        // rejects.
        // Single-quoted strings: a double-quoted Groovy string would interpolate the `$` in the
        // regex, and the block-comment pattern needs its backslashes doubled.
        String withoutBlocks = sourceOf(name).replaceAll('(?s)/\\*.*?\\*/', '')
        return withoutBlocks.replaceAll('(?m)^\\s*//.*$', '')
    }

    def "the token settings link is a router link, not a hand-built href"() {
        given:
        def guide = sourceOf("MeasurementDataDownloadGuide.java")

        expect: "a RouterLink resolves the path through the router, so the context path is honoured"
        def code = codeOf("MeasurementDataDownloadGuide.java")
        code.contains("RouterLink")
        !code.contains("new Anchor()")
        !code.contains("setHref(")

        and: "it navigates to the route's own class rather than to a copied URL fragment"
        guide.contains("setRoute(navigationTarget)")
    }

    def "the token link target is a real routed view"() {
        given: "the link points at the personal access token view"
        def tokenMain = new File(
                "datamanager-app/src/main/java/life/qbic/datamanager/views/account/PersonalAccessTokenMain.java")

        expect: "the class the dialog names must actually declare the route it stands for"
        def source = tokenMain.exists() ? tokenMain.text
                : new File("src/main/java/life/qbic/datamanager/views/account/PersonalAccessTokenMain.java").text
        source.contains('@Route(value = "settings/api-tokens"')

        and: "and no view hard-codes that path string in code"
        !codeOf("MeasurementDataDownloadGuide.java").contains("settings/api-tokens")
        !codeOf("MeasurementDatasetMain.java").contains("settings/api-tokens")
    }

    def "the token link opens in a new tab, so the open dialog survives the round trip"() {
        given:
        def code = codeOf("MeasurementDataDownloadGuide.java")

        expect: "navigating this tab would discard the dialog and the selection it is scoped to"
        code.contains('setAttribute("target", "_blank")')

        and: "the new tab must not be able to reach back into this window"
        code.contains('setAttribute("rel", "noopener noreferrer")')

        and: "the URL is still resolved by the router, so the context path is honoured"
        code.contains("RouterLink")
        !code.contains("setHref(")
    }

    def "exported file names never carry the project's internal identifier"() {
        given:
        def code = codeOf("MeasurementDatasetMain.java")

        expect: "the project code is resolved in beforeEnter, as every other project view does"
        code.contains("withProjectCode(")
        code.contains("getProjectCode(")

        and: "the fallback degrades to a word, never to the UUID"
        // A UUID in an exported file name is meaningless to the reader and looks like a defect.
        !code.contains("projectId().map(p -> p.value())")
        code.contains('orElse("project")')
    }

    def "the download anchor lives inside the modal dialog, not in the page behind it"() {
        given:
        def guide = sourceOf("MeasurementDataDownloadGuide.java")

        expect: "AppDialog is strictly modal, so only an anchor inside the dialog receives the click"
        guide.contains("DownloadComponent")
        guide.contains("downloadComponent.trigger(")

        and: "the dialog adds it to its own component tree"
        guide.contains("downloadComponent);")
    }

    def "the location export downloads through the dialog's anchor"() {
        given: "the page builds the provider and the dialog performs the download"
        def page = sourceOf("MeasurementDatasetMain.java")
        def guide = sourceOf("MeasurementDataDownloadGuide.java")

        expect: "the page must not trigger the page-level anchor while its dialog is open"
        page.contains("guide.triggerDownload(")

        and: "the dialog exposes a trigger for exactly that"
        guide.contains("public void triggerDownload(")
    }

    def "no view in this package hand-builds a route path"() {
        given: "every view source of the dataset package"
        def offenders = []
        viewPackage().listFiles()?.each { file ->
            if (!file.name.endsWith(".java") || file.name.contains("Spec")) {
                return
            }
            // Comments removed first: explaining why a pattern was wrong must not trip the rule.
            def text = codeOf(file.name)
            // A quoted relative path is a hand-built route. Routes are declared once in AppRoutes
            // and resolved by the router; copying the string into a view is how a context path
            // gets missed.
            if (text =~ /"\.\.\/[a-z]/) {
                offenders << file.name
            }
        }

        expect:
        offenders.isEmpty()
    }
}
