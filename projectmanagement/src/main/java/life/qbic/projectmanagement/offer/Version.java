package life.qbic.projectmanagement.offer;

import life.qbic.projectmanagement.ProjectManagementDomainException;

public record Version(long number) {

  public Version {
    if (number <= 0) {
      throw new ProjectManagementDomainException(
          "Offer version must be greater than 0. Provided: " + number);
    }
  }

}
