package life.qbic.projectmanagement.offer;

import static java.util.Objects.requireNonNull;

import life.qbic.projectmanagement.ProjectManagementDomainException;

public record ProjectConservedPart(String conservedPart) {

  public ProjectConservedPart {
    requireNonNull(conservedPart);
    if (conservedPart.isEmpty()) {
      throw new ProjectManagementDomainException(
          "Project conserved part of offer id must not be empty. Provided: " + conservedPart);
    }
  }

}
