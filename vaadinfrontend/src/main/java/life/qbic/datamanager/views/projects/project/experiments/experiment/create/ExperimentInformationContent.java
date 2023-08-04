package life.qbic.datamanager.views.projects.project.experiments.experiment.create;

import java.util.List;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Species;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Specimen;

public record ExperimentInformationContent(String experimentName, List<Species> species,
                                           List<Specimen> specimen, List<Analyte> analytes) {

}
