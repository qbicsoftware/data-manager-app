package life.qbic.projectmanagement.experiment.persistence;

import java.util.List;
import life.qbic.projectmanagement.domain.project.ProjectId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SampleStatistic extends JpaRepository<SampleStatisticEntry, Integer> {

  List<SampleStatisticEntry> findByProjectId(ProjectId projectId);

}
