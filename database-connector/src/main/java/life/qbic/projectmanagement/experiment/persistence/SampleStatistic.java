package life.qbic.projectmanagement.experiment.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SampleStatistic extends JpaRepository<Integer, SampleStatisticEntry> {

}
