package life.qbic.projectmanagement.domain.project;

/**
 * Record representing a person reference with name and contact email address
 *
 * @since 1.0.0
 */
public record PersonReference(String referenceId, String fullName, String emailAddress) {}
