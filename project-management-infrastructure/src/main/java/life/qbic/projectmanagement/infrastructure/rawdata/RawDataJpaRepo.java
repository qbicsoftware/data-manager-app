package life.qbic.projectmanagement.infrastructure.rawdata;

import life.qbic.projectmanagement.domain.model.rawdata.RawData;
import life.qbic.projectmanagement.domain.model.rawdata.RawDataId;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Simple raw data measurement JPA repository to query and filter concise
 * {@link RawData} information
 */
public interface RawDataJpaRepo extends
    JpaRepository<RawData, RawDataId>,
    JpaSpecificationExecutor<RawData> {

  @Override
  long count(Specification<RawData> spec);

}
