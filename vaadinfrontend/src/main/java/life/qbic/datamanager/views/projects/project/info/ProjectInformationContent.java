package life.qbic.datamanager.views.projects.project.info;

import life.qbic.projectmanagement.domain.project.PersonReference;

public record ProjectInformationContent(String projectTitle, String projectObjective,
                                        String experimentalDesignDescription,
                                        PersonReference principalInvestigator,
                                        PersonReference responsiblePerson,
                                        PersonReference projectManager) {

}
