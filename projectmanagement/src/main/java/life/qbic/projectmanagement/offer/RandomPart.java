package life.qbic.projectmanagement.offer;

import static java.util.Objects.requireNonNull;

import life.qbic.projectmanagement.ProjectManagementDomainException;

public record RandomPart(String randomPart) {

  public RandomPart {
    requireNonNull(randomPart);
    if (randomPart.isEmpty()) {
      throw new ProjectManagementDomainException(
          "Random part of an offer id must not be empty. Provided: " + randomPart);
    }
  }

}
