package life.qbic.projectmanagement.application.api.fair;

/**
 * Interface to describe a digital object factory that creates digital objects described by metadata
 * with or without data in the context of data management.
 *
 * @since 1.10.0
 */
public interface DigitalObjectFactory {

  /**
   * Creates a summary {@link DigitalObject} based on the provided {@link ResearchProject}.
   *
   * @param researchProject the research project to summarize
   * @return a digital object representation of the research project
   * @throws BuildException in case the digital object cannot be created.
   * @since 1.10.0
   */
  DigitalObject summary(ResearchProject researchProject) throws BuildException;

  /**
   * Exception class to use for exceptions during interaction with the {@link DigitalObjectFactory}
   * API.
   *
   * @since 1.10.0
   */
  class BuildException extends RuntimeException {

    public BuildException(String message) {
      super(message);
    }

    public BuildException(String message, Throwable cause) {
      super(message, cause);
    }

  }
}
