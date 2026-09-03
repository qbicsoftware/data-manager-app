package life.qbic.projectmanagement.application;

import java.util.List;
import java.util.stream.Collectors;

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
   * A pre-formatted message that informs a user about newly created samples and their identifiers
   * in the data manager.
   *
   * @param fullNameUser the name of the user to inform for addressing them politely
   * @param projectTitle the title of the project, will be in the message to inform the user about
   *                     which project they have been granted access with
   * @param batchName    the name of the batch that was added
   * @param sampleUri   a uniform resource identifier of the sample page of this project, that the
   *                    recipient can use to access the newly registered samples
   * @return the filled out template message
   * @since 1.0.0
   */
  public static String samplesAddedToProject(String fullNameUser, String projectTitle,
      String batchName, String sampleUri) {
    return String.format("""
        Dear %s,
                      
        the new batch ('%s') of samples has been added to the project:
               
        '%s'
            
        Sample information and QBiC identifiers have been added to the Data Manager.
        These identifiers uniquely characterize each added sample. They will be used to attach data
        for each of the samples, as soon as it has been measured and uploaded.

        Please click the link below to access the sample information after login:
            
        %s
        """, fullNameUser, batchName, projectTitle, sampleUri);
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

  /**
   * A pre-formatted message that informs a project collaborator about a
   * dataset that has just been connected by a teammate.
   *
   * @param fullNameUser the full name of the recipient
   * @param projectTitle the title of the project the dataset was connected to
   * @param datasetTitle the human-readable title of the connected dataset
   * @param datasetPid   the persistent identifier (PID / DOI) of the dataset
   * @param projectUri   a resolvable URL to the project in Data Manager
   * @return the filled out template message
   * @since 1.12.0
   */
  public static String datasetConnectedToProject(String fullNameUser, String projectTitle,
      String datasetTitle, String datasetPid, String projectUri) {
    return String.format("""
        Dear %s,

        a new dataset has just been connected to the project '%s':

          Title: %s
          PID:   %s

        Please open the project to see the connected dataset:

        %s
        """, fullNameUser, projectTitle, datasetTitle, datasetPid, projectUri);
  }

  /**
   * A pre-formatted message that informs a project collaborator about
   * a dataset connection that has been removed by a teammate.
   *
   * @param fullNameUser the full name of the recipient
   * @param projectTitle the title of the project the dataset was removed from
   * @param datasetTitle the human-readable title of the dataset connection
   * @param datasetPid   the persistent identifier (PID / DOI) of the dataset
   * @param projectUri   a resolvable URL to the project in Data Manager
   * @return the filled out template message
   * @since 1.12.0
   */
  public static String datasetRemovedFromProject(String fullNameUser, String projectTitle,
      String datasetTitle, String datasetPid, String projectUri) {
    return String.format("""
        Dear %s,

        a dataset connection has been removed from the project '%s':

          Title: %s
          PID:   %s

        Please open the project to see the current list of connected datasets:

        %s
        """, fullNameUser, projectTitle, datasetTitle, datasetPid, projectUri);
  }

  /**
   * A pre-formatted message that informs a project collaborator that one
   * or more connected datasets in a project were updated by a sync
   * (DATSET-04/08, ADR-0005 N1).
   *
   * <p>One email is sent per sync trigger with <em>all</em> updated
   * records listed — never one email per record — to avoid flooding
   * members when several versions are updated in the same trigger.</p>
   *
   * @param fullNameUser    the full name of the recipient
   * @param projectTitle    the title of the project the datasets were updated in
   * @param updatedDatasets pre-rendered lines, one per updated record
   *                        (see {@link #updatedRecordLine})
   * @param projectUri      a resolvable URL to the project in Data Manager
   * @return the filled out template message
   * @since 1.13.0
   */
  public static String datasetsSyncedToProject(String fullNameUser, String projectTitle,
      List<String> updatedDatasets, String projectUri) {
    String lines = updatedDatasets.stream()
        .map(line -> "  - " + line)
        .collect(Collectors.joining("\n"));
    return String.format("""
        Dear %s,

        the following connected dataset(s) in the project '%s' have been updated:

        %s

        Please open the project to see the updated datasets:

        %s
        """, fullNameUser, projectTitle, lines, projectUri);
  }

  /**
   * Renders one record line for the combined dataset-sync email.
   *
   * @param title              the record title
   * @param pid                the persistent identifier (DOI)
   * @param previousVersion    version before the sync, or null
   * @param newVersion         version after the sync, or null
   * @param accessStatusChanged whether the access level changed
   *                            (e.g. embargo lifted or added)
   * @return a single human-readable line, e.g.
   *         {@code "My dataset (10.5281/zenodo.123): v1 → v2 (access status changed)"}
   * @since 1.13.0
   */
  public static String updatedRecordLine(
      String title, String pid, String previousVersion, String newVersion,
      boolean accessStatusChanged) {
    String versionPart;
    if (previousVersion != null && newVersion != null && !previousVersion.equals(newVersion)) {
      versionPart = previousVersion + " → " + newVersion;
    } else if (newVersion != null) {
      versionPart = newVersion;
    } else {
      versionPart = "—";
    }
    String accessNote = accessStatusChanged ? " (access status changed)" : "";
    return title + " (" + pid + "): " + versionPart + accessNote;
  }
}
