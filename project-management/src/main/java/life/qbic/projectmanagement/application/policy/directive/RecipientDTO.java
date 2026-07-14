package life.qbic.projectmanagement.application.policy.directive;

import com.fasterxml.jackson.annotation.JsonProperty;
import life.qbic.identity.api.UserInfo;

public class RecipientDTO {

  @JsonProperty("fullName")
  private String fullName;

  @JsonProperty("emailAddress")
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
