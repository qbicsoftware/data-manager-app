package life.qbic.datamanager.views.projects.create;

import java.util.List;
import life.qbic.projectmanagement.domain.project.PersonReference;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Species;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Specimen;

public record ProjectCreationContent(String offerId, String projectCode, String title,
                                     String objective, String experimentName, List<Species> species,
                                     List<Specimen> specimen, List<Analyte> analyte,
                                     String experimentalDesignDescription,
                                     PersonReference principalInvestigator,
                                     PersonReference projectResponsible,
                                     PersonReference projectManager) {

}
