package life.qbic.projectmanagement.infrastructure.sample;

import java.util.List;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SampleStatistic extends JpaRepository<SampleStatisticEntry, Integer> {

  List<SampleStatisticEntry> findByProjectId(ProjectId projectId);

}
