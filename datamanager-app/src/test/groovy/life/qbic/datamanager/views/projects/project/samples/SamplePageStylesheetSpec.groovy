package life.qbic.datamanager.views.projects.project.samples

import spock.lang.Specification

import java.util.regex.Pattern

/**
 * Guards the sample page's custom properties against the failure mode that shipped the first
 * version of this rework: a {@code var()} that referenced a token nobody ever defined.
 *
 * <p>An undefined custom property is not a syntax error. When it is used where a value is required
 * -- most sharply as a grid track -- the whole declaration becomes invalid at computed-value time
 * and is dropped, so {@code grid-template-columns: minmax(0, 1fr) var(--missing)} silently collapses
 * to a single column. The compiler is happy, the unit tests are happy, and the layout is wrong only
 * on screen. That is precisely how the detail panel ended up stacked under the grid instead of
 * beside it.</p>
 *
 * <p>This spec reads the stylesheets as text and cross-checks every custom property a sample-page
 * rule consumes against the properties the theme defines, including the ones that come from Lumo
 * rather than from this repository.</p>
 */
class SamplePageStylesheetSpec extends Specification {

    private static final String THEME_PATH = "frontend/themes/datamanager/components"

    private static final String SAMPLE_PAGE_CSS = "sample-page.css"

    private static final Pattern VAR_USE = Pattern.compile(/var\(\s*(--[a-zA-Z0-9-]+)/)

    /**
     * Resolves the theme directory whether the spec runs from the repository root or from the
     * module directory, mirroring how {@link life.qbic.datamanager.views.demo.PrototypeIsolationSpec}
     * locates its sources.
     */
    private static File themeDir() {
        def candidates = [new File(THEME_PATH), new File("datamanager-app/" + THEME_PATH)]
        def found = candidates.find { it.isDirectory() }
        assert found != null: "theme directory not found; tried ${candidates*.path}"
        return found
    }

    def "every custom property the sample page uses is defined somewhere in the theme"() {
        given: "the sample page stylesheet"
        def samplePage = new File(themeDir(), SAMPLE_PAGE_CSS)
        assert samplePage.exists()

        and: "every property defined by the theme's own stylesheets"
        def defined = new HashSet<String>()
        themeDir().listFiles({ f -> f.name.endsWith(".css") } as FileFilter).each { f ->
            def matcher = Pattern.compile(/(--[a-zA-Z0-9-]+)\s*:/).matcher(f.text)
            while (matcher.find()) {
                defined.add(matcher.group(1))
            }
        }

        when: "the properties the sample page consumes are collected"
        def used = new TreeSet<String>()
        def matcher = VAR_USE.matcher(samplePage.text)
        while (matcher.find()) {
            used.add(matcher.group(1))
        }

        then: "the scan is not vacuous"
        used.size() > 5

        and: "each one is either defined in the theme or a Lumo built-in"
        // Lumo defines its own custom properties in the framework, not in this repository, so a
        // `--lumo-*` reference cannot be resolved from these files. Everything else must resolve
        // here, because a theme-local token that is used but never defined is always a bug.
        def unresolved = used.findAll { name ->
            !defined.contains(name) && !name.startsWith("--lumo-")
        }

        unresolved.isEmpty()
    }

    def "the detail panel width is defined, since it is used as a grid track"() {
        given: "the sample page stylesheet"
        def css = new File(themeDir(), SAMPLE_PAGE_CSS).text

        expect: "the token backing the master-detail column exists"
        // This is the specific regression: the two-column layout depends entirely on this value
        // resolving. If it is removed, the panel silently stacks under the grid.
        css.contains("--sample-detail-panel-width:")

        and: "and it is actually consumed by the two-column rule"
        css.contains("var(--sample-detail-panel-width)")
    }

    def "the sample page stylesheet is imported so its rules are not dead code"() {        given: "the aggregate stylesheet that the theme actually loads"
        def custom = new File(themeDir(), "custom.css").text

        expect: "the sample page is imported exactly once"
        def imports = custom.readLines().findAll { it.contains(SAMPLE_PAGE_CSS) }
        imports.size() == 1
    }
}
