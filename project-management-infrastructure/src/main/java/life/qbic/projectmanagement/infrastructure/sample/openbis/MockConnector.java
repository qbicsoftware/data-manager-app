package life.qbic.projectmanagement.infrastructure.sample.openbis;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import life.qbic.application.commons.SortOrder;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.DataRepoConnectionTester;
import life.qbic.projectmanagement.application.dataset.RawDataLookup;
import life.qbic.projectmanagement.application.dataset.RawDataService.RawDataDatasetInformation;
import life.qbic.projectmanagement.application.sample.SampleIdCodeEntry;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementCode;
import life.qbic.projectmanagement.domain.model.measurement.NGSMeasurement;
import life.qbic.projectmanagement.domain.model.measurement.ProteomicsMeasurement;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectCode;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;
import life.qbic.projectmanagement.infrastructure.experiment.measurement.MeasurementDataRepo;
import life.qbic.projectmanagement.infrastructure.project.QbicProjectDataRepo;
import life.qbic.projectmanagement.infrastructure.sample.SampleDataRepository;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Profile;

@Profile("development")
public class MockConnector implements QbicProjectDataRepo, SampleDataRepository,
    MeasurementDataRepo, RawDataLookup, DataRepoConnectionTester, DisposableBean {

  private static final Logger log = logger(MockConnector.class);

  private static void logWarning() {
    log.warn("Using mock implementation. Not suited for production deployment.");
  }

  @Override
  public void testDatastoreServer() {
    logWarning();
  }

  @Override
  public void testApplicationServer() {
    logWarning();
  }

  @Override
  public List<RawDataDatasetInformation> queryRawDataByMeasurementCodes(String filter,
      Collection<MeasurementCode> measurementCodes, int offset, int limit,
      List<SortOrder> sortOrders) {
    logWarning();
    return List.of();
  }

  @Override
  public int countRawDataByMeasurementIds(Collection<MeasurementCode> measurementCodes) {
    logWarning();
    return 0;
  }

  @Override
  public void addNGSMeasurement(NGSMeasurement ngsMeasurement, List<SampleCode> sampleCodes) {
    logWarning();
  }

  @Override
  public void addProteomicsMeasurement(ProteomicsMeasurement proteomicsMeasurement,
      List<SampleCode> sampleCodes) {
    logWarning();
  }

  @Override
  public void deleteProteomicsMeasurements(List<ProteomicsMeasurement> measurements) {
    logWarning();
  }

  @Override
  public void deleteNGSMeasurements(List<NGSMeasurement> measurements) {
    logWarning();
  }

  @Override
  public void saveAllProteomics(
      Map<ProteomicsMeasurement, Collection<SampleIdCodeEntry>> proteomicsMeasurementsMapping) {
    logWarning();
  }

  @Override
  public void saveAllNGS(
      Map<NGSMeasurement, Collection<SampleIdCodeEntry>> ngsMeasurementsMapping) {
    logWarning();
  }

  @Override
  public boolean hasDataAttached(List<MeasurementCode> measurements) {
    logWarning();
    return false;
  }

  @Override
  public void add(Project project) {
    logWarning();
  }

  @Override
  public void delete(ProjectCode projectCode) {
    logWarning();
  }

  @Override
  public boolean projectExists(ProjectCode projectCode) {
    logWarning();
    return false;
  }

  @Override
  public void addSamplesToProject(Project project, List<Sample> samples) {
    logWarning();
  }

  @Override
  public void deleteAll(ProjectCode projectCode, Collection<SampleCode> samples) {
    logWarning();
  }

  @Override
  public void updateAll(Project project, Collection<Sample> samples) {
    logWarning();
  }

  @Override
  public boolean canDeleteSample(SampleCode sampleCode) {
    logWarning();
    return false;
  }

  @Override
  public void destroy() throws Exception {
    logWarning();
  }
}
