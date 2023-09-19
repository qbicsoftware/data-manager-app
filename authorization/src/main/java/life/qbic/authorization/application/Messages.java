package life.qbic.authorization.application;

/**
 * <b>Messages</b>
 *
 * <p>A collection of message templates to notify users.</p>
 *
 * @since 1.0.0
 */
public class Messages {

  private Messages() {

  }

  /**
   * A pre-formatted message that informs a user about their new access grant to a project in the
   * data manager.
   *
   * @param fullNameUser the name of the user to inform for addressing them politely
   * @param projectTitle the title of the project, will be in the message to inform the user about
   *                     which project they have been granted access with
   * @param projectUri   a uniform resource identifier of the project, that the user can use to
   *                     access the project
   * @return the filled out template message
   * @since 1.0.0
   */
  public static String projectAccessToUser(String fullNameUser, String projectTitle,
      String projectUri) {
    return String.format("""
        Dear %s,
                      
        you have been granted access to project:
               
        '%s'
            
        Please click the link below to access the project after login:
            
        %s
        """, fullNameUser, projectTitle, projectUri);
  }

}
