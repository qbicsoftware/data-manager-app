package life.qbic.projectmanagement.offer;

import static java.util.Objects.requireNonNull;

import life.qbic.projectmanagement.ProjectManagementDomainException;

/**
 * The id of an offer.
 *
 * @since <version tag>
 */
public record OfferId(ProjectConservedPart projectConservedPart, RandomPart randomPart,
                      Version version) {

  public OfferId {
    requireNonNull(projectConservedPart);
    requireNonNull(randomPart);
    requireNonNull(version);
  }

  public static OfferId from(String offerId) {
    //O_[project conserved part]_[random id]_[version]
    if (!offerId.startsWith("O_")) {
      throw new ProjectManagementDomainException(
          "OfferId does not start with 'O_'. Provided: " + offerId);
    }
    String[] splits = offerId.split("_");
    if (splits.length != 4) {
      throw new ProjectManagementDomainException(
          "OfferId does not contain 4 parts. Provided: " + offerId);
    }
    ProjectConservedPart projectConservedPart = new ProjectConservedPart(splits[1]);
    RandomPart randomPart = new RandomPart(splits[2]);
    long versionNumber = Long.parseLong(splits[3]);
    Version version = new Version(versionNumber);
    return new OfferId(projectConservedPart, randomPart, version);
  }

}
