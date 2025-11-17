package life.qbic.datamanager.views.projects.project.experiments.experiment.create;

import java.util.List;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Species;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Specimen;

public record ExperimentInformationContent(String experimentName, List<Species> species,
                                           List<Specimen> specimen, List<Analyte> analytes) {

}
