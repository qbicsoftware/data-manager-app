package life.qbic.projectmanagement.infrastructure.api.fair.rocrate

import life.qbic.projectmanagement.application.api.fair.ContactPoint
import life.qbic.projectmanagement.application.api.fair.ResearchProject
import life.qbic.projectmanagement.infrastructure.TempDirectory
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipInputStream

/**
 * End-to-end smoke test for the RO-Crate export.
 *
 * <p>This test exercises the production code path from crate construction through
 * serialization to a zipped crate. It exists because ro-crate-java 2.x depends on
 * {@code com.apicatalog:titanium-json-ld} at <em>runtime</em> only: its
 * {@code IdentifierUtils.isValidUri} delegates to
 * {@code com.apicatalog.jsonld.uri.UriUtils}, which is not referenced anywhere in
 * this module's own source code.
 *
 * <p>Consequently the dependency cannot be missed by the compiler. When it was
 * excluded from the classpath, the build stayed green and the failure surfaced
 * only when a user requested an export:
 *
 * <pre>
 * java.lang.ClassNotFoundException: com.apicatalog.jsonld.uri.UriUtils
 *   at edu.kit.datamanager.ro_crate.special.IdentifierUtils.isValidUri
 *   at edu.kit.datamanager.ro_crate.entities.AbstractEntity$AbstractEntityBuilder.setId
 * </pre>
 *
 * <p>Only executing the code detects that class of defect, so this test asserts
 * behaviour (a readable crate with metadata) rather than a dependency list.
 */
class RoCrateFactorySmokeSpec extends Specification {

  Path tempDir

  def setup() {
    tempDir = Files.createTempDirectory("rocrate-smoke")
  }

  def cleanup() {
    tempDir.toFile().deleteDir()
  }

  def "builds, serializes and zips a project summary RO-Crate"() {
    given: "a factory bound to a writable working directory"
    def factory = new RoCrateFactory(new TempDirectory(tempDir.toString()))

    and: "a minimal research project"
    def project = ResearchProject.from(
        "Test project",
        "QTEST001",
        "A project used to verify the RO-Crate export.",
        [ContactPoint.from("Jane Doe", "jane.doe@example.org", "Principal Investigator")])

    when: "the crate is built and serialized"
    def digitalObject = factory.summary(project)

    then: "an object with the expected identity and MIME type is returned"
    digitalObject != null
    digitalObject.mimeType().toString() == "application/zip"
    digitalObject.name().isPresent()

    and: "the payload is a well-formed zip archive"
    def entries = zipEntries(digitalObject.content().bytes)
    entries.containsKey("ro-crate-metadata.json")
    entries.containsKey("project-summary.docx")
    entries.containsKey("project-summary.yml")

    and: "the crate metadata is valid JSON-LD describing the project"
    def metadata = new tools.jackson.databind.ObjectMapper()
        .readTree(entries["ro-crate-metadata.json"])
    def graph = metadata.get("@graph")
    graph.isArray()

    and: "the root data entity carries the project identity and required links"
    def root = graph.find { it.get("@id")?.asString() == "./" }
    root != null
    root.get("@type")?.asString() == "Dataset"
    root.get("name")?.asString() == "QBiC-project-QTEST001-ro-crate"
    root.get("description")?.asString().contains("Test project")
    root.get("publisher")?.get("@id")?.asString() == "https://ror.org/00v34f693"
    root.get("license")?.get("@id")?.asString() == "https://creativecommons.org/licenses/by/4.0/"

    and: "the metadata descriptor and both payload files are referenced"
    graph.any { it.get("@id")?.asString() == "ro-crate-metadata.json" }
    root.get("hasPart").any { it.get("@id")?.asString() == "project-summary.docx" }
    root.get("hasPart").any { it.get("@id")?.asString() == "project-summary.yml" }
  }

  /**
   * Reads all zip entries into memory, failing with a descriptive message if the
   * payload is not a readable archive.
   */
  private static Map<String, String> zipEntries(byte[] content) {
    assert content != null && content.length > 0: "crate payload was empty"
    Map<String, String> result = [:]
    new ZipInputStream(new ByteArrayInputStream(content)).withCloseable { zip ->
      def entry
      while ((entry = zip.nextEntry) != null) {
        def bytes = zip.readAllBytes()
        result[entry.name] = new String(bytes, "UTF-8")
        zip.closeEntry()
      }
    }
    assert !result.isEmpty(): "crate zip contained no entries"
    return result
  }
}
