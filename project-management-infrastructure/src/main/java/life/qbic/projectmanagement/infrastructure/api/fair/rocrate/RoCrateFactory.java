package life.qbic.projectmanagement.infrastructure.api.fair.rocrate;

import static life.qbic.logging.service.LoggerFactory.logger;

import edu.kit.datamanager.ro_crate.writer.RoCrateWriter;
import edu.kit.datamanager.ro_crate.writer.ZipWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.api.fair.DigitalObject;
import life.qbic.projectmanagement.application.api.fair.DigitalObjectFactory;
import life.qbic.projectmanagement.application.api.fair.ResearchProject;
import life.qbic.projectmanagement.infrastructure.TempDirectory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Component
public class RoCrateFactory implements DigitalObjectFactory {

  private static final Logger log = logger(RoCrateFactory.class);
  private final TempDirectory workingDir;

  @Autowired
  public RoCrateFactory(TempDirectory directory) {
    this.workingDir = Objects.requireNonNull(directory);
  }

  @Override
  public DigitalObject summary(ResearchProject researchProject) throws BuildException {
    Path buildDir;
    Path zippedRoCrateDir;

    try {
      buildDir = workingDir.createDirectory();
      zippedRoCrateDir = workingDir.createDirectory();
    } catch (IOException e) {
      log.error("Failed to create directory for ro-crate build", e);
      throw new BuildException("Failed to create digital object");
    }

    try {
      var roCrate = ROCreateBuilder.buildRoCrate(buildDir, researchProject);
      var roCrateZipWriter = new RoCrateWriter(new ZipWriter());
      var zippedRoCrateFile = zippedRoCrateDir.resolve(
          "project-summary-ro-crate.zip");
      roCrateZipWriter.save(roCrate, zippedRoCrateFile.toString());
      byte[] cachedContent = Files.readAllBytes(zippedRoCrateFile);
      log.info("----start");
      log.info(new String(cachedContent));
      log.info("----end");
      return new DigitalObject() {
        @Override
        public InputStream content() {
          return new ByteArrayInputStream(cachedContent);
        }

        @Override
        public MimeType mimeType() {
          return MimeType.valueOf("application/zip");
        }

        @Override
        public Optional<String> name() {
          return Optional.of(zippedRoCrateFile.getFileName().toString());
        }

        @Override
        public Optional<String> id() {
          return Optional.of(researchProject.identifier());
        }
      };
    } catch (IOException e) {
      log.error("Failed to create ro-crate zip file", e);
      throw new BuildException("Failed to create digital object");
    } finally {
      deleteTempDir(buildDir.toFile());
      deleteTempDir(zippedRoCrateDir.toFile());
    }
  }

  private boolean deleteTempDir(File dir) {
    File[] files = dir.listFiles(); //null if not a directory
    // https://docs.oracle.com/javase/8/docs/api/java/io/File.html#listFiles--
    if (files != null) {
      for (File file : files) {
        if (!deleteTempDir(file)) {
          return false;
        }
      }
    }
    return dir.delete();
  }

}
