package life.qbic.datamanager.views.projects.create;

import life.qbic.projectmanagement.domain.project.PersonReference;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Species;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Specimen;

import java.util.List;

public record ProjectCreationContent(String projectCode, String offerId, String title,
                                     String objective, List<Species> species,
                                     List<Specimen> specimen, List<Analyte> analyte,
                                     String experimentalDesignDescription,
                                     PersonReference principalInvestigator,
                                     PersonReference projectResponsible,
                                     PersonReference projectManager) {

}
