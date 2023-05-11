package life.qbic.projectmanagement.experiment.persistence;

import java.util.Objects;
import java.util.Optional;
import life.qbic.application.commons.Result;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.api.SampleCodeService;
import life.qbic.projectmanagement.domain.project.ProjectCode;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.sample.SampleCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Service
public class SampleCodeJpaRepository implements SampleCodeService {

  private final SampleStatistic sampleStatistic;

  @Autowired
  public SampleCodeJpaRepository(SampleStatistic sampleStatistic) {
    this.sampleStatistic = Objects.requireNonNull(sampleStatistic);
  }

  @Override
  public Result<SampleCode, ResponseCode> generateFor(ProjectId projectId) {
    Optional<SampleStatisticEntry> queryResult = sampleStatistic.findByProjectId(projectId).stream()
        .findFirst();
    if (queryResult.isPresent()) {
      var sampleStatisticEntry = queryResult.get();
      int sampleNumber = sampleStatisticEntry.drawNextSampleNumber();
      ProjectCode projectCode = sampleStatisticEntry.projectCode();
      sampleStatistic.save(sampleStatisticEntry);
      return Result.fromValue(generateSampleCode(projectCode, sampleNumber));
    } else {
      return Result.fromError(ResponseCode.SAMPLE_STATISTICS_RECORD_NOT_FOUND);
    }
  }

  @Override
  public void addProjectToSampleStats(ProjectId projectId, ProjectCode projectCode) {
    if (!sampleStatisticsEntryExistsFor(projectId)) {
      sampleStatistic.save(SampleStatisticEntry.create(projectId, projectCode));
    }
  }

  @Override
  public boolean sampleStatisticsEntryExistsFor(ProjectId projectId) {
    return !sampleStatistic.findByProjectId(projectId).isEmpty();
  }

  private static SampleCode generateSampleCode(ProjectCode code, int sampleNumber) {
    String sampleCode = String.format("%s%03d", code.value(), remainderCounter(sampleNumber)) + letterForMille(sampleNumber);
    String sampleCodeWithChecksum = sampleCode + checksum(sampleCode);
    return new SampleCode(sampleCodeWithChecksum);
  }

  private static char letterForMille(int sampleNumber) {
    return (char) (((sampleNumber -1) / 999) + 65);
  }

  private static int remainderCounter(int sampleNumber) {
    var remainder = sampleNumber % 999;
    return remainder == 0 ? 999 : remainder;
  }

  private static char checksum(String code) {
      int i = 1;
      int sum = 0;
      for (int idx = 0; idx <= code.length() - 1; idx++) {
        sum += (((int) code.charAt(idx))) * i;
        i += 1;
      }
      return mapToChar(sum % 34);
  }

  private static char mapToChar(int i) {
    i += 48;
    if (i > 57) {
      i += 7;
    }
    return (char) i;
  }
}
