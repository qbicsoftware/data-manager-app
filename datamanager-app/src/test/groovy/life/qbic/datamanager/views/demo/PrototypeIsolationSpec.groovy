package life.qbic.datamanager.views.demo

import spock.lang.Specification

/**
 * Guards the one-way boundary between the throwaway prototypes and the production application.
 *
 * <p>The prototypes live in the same Maven module as the production views, so nothing at compile time
 * stops a real view from importing them. That is the leak this spec exists to catch: a demo type is
 * convenient, and the first import of {@code life.qbic.datamanager.views.demo} in production code is
 * how prototype shortcuts become production behaviour.</p>
 *
 * <p>It reads the source tree rather than the compiled classes on purpose — a failure has to name the
 * file and line that broke the boundary, and a source location is the useful thing to print.</p>
 */
class PrototypeIsolationSpec extends Specification {

  private static final String DEMO_PATH = "life/qbic/datamanager/views/demo"

  def "no production source references the prototype package"() {
    given: "every Java source outside the prototype package"
    def scanned = productionFiles()
    def offenders = offendersIn(scanned) { line ->
      line.contains("life.qbic.datamanager.views.demo")
    }

    expect: "the scan is not vacuous"
    scanned.size() > 10

    and: "the dependency points one way only"
    // If this fails, production code is reaching into a prototype. Implement the behaviour in a real
    // view under an approved Story — do not lift the prototype across.
    offenders.isEmpty()
  }

  def "no production source borrows a prototype style"() {
    given: "the prototype's stylesheet is not a design system and must not be adopted piecemeal"
    def offenders = offendersIn(productionFiles()) { line ->
      line.contains("project-page-prototype") || line.contains('"pt-')
    }

    expect: "a surviving visual rule belongs in data-manager-app-design-system, not here"
    offenders.isEmpty()
  }

  def "every prototype route is development-profile only"() {
    given: "the prototype views that declare routes"
    def routed = prototypeFiles().findAll { it.text.contains("@Route") }

    expect: "the isolation is not merely a convention: none of them is reachable in production"
    !routed.isEmpty()
    routed.every { it.text.contains('@Profile("development")') }
  }

  def "the prototype package declares its contract"() {
    expect: "package-info.java carries the half of the boundary a person has to read"
    prototypeFiles().any { it.name == "package-info.java" }
  }

  // ── helpers ─────────────────────────────────────────────────────────────

  /** Every matching line of the given sources, reported as {@code path:line: text}. */
  private static List<String> offendersIn(List<File> files, Closure<Boolean> matches) {
    files.collectMany { file ->
      def lines = file.readLines()
      (0..<lines.size()).findAll { i -> matches(lines[i]) }
          .collect { i -> "${file.path}:${i + 1}: ${lines[i].trim()}".toString() }
    }
  }

  private static List<File> productionFiles() {
    sourceRoots().collectMany { root ->
      def demo = new File(root, DEMO_PATH).canonicalPath + File.separator
      javaFiles(root).findAll { !it.canonicalPath.startsWith(demo) }
    }
  }

  private static List<File> prototypeFiles() {
    sourceRoots().collectMany { root -> javaFiles(new File(root, DEMO_PATH)) }
  }

  private static List<File> javaFiles(File root) {
    def out = []
    root.eachFileRecurse { file ->
      if (file.isFile() && file.name.endsWith(".java")) {
        out << file
      }
    }
    out
  }

  /** The main source roots, resolved from the module directory or the repository root. */
  private static List<File> sourceRoots() {
    def candidates = [new File("src/main/java"), new File("datamanager-app/src/main/java")]
    def roots = candidates.findAll { it.isDirectory() }
    assert !roots.isEmpty(): "no main source root; looked in ${candidates*.absolutePath}"
    roots
  }
}
