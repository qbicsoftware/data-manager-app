package life.qbic.projectmanagement.application;

import java.util.List;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Species;

public interface SpeciesSearchService {

    List<Species> fetch(int offset, int limit, String filterText);
    int getCount(String filterText);
}
