package life.qbic.controlling.application.policy.directive;

import life.qbic.identity.api.UserInfo;

public class RecipientDTO {

  private String fullName;
  private String emailAddress;

  public RecipientDTO(UserInfo userInfo) {
    this.fullName = userInfo.fullName();
    this.emailAddress = userInfo.emailAddress();
  }

  public String getFullName() {
    return fullName;
  }

  public String getEmailAddress() {
    return emailAddress;
  }
}
