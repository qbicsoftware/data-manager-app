package life.qbic.projectmanagement.infrastructure.sample.openbis;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import life.qbic.application.commons.SortOrder;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@ConditionalOnProperty(
    value = "testing.openbis.enabled",
    havingValue = "true",
    matchIfMissing = false)
public class DummyConnector implements QbicProjectDataRepo, SampleDataRepository,
    MeasurementDataRepo, RawDataLookup, DataRepoConnectionTester, DisposableBean {

  @Override
  public void testDatastoreServer() {

  }

  @Override
  public void testApplicationServer() {

  }

  @Override
  public List<RawDataDatasetInformation> queryRawDataByMeasurementCodes(String filter,
      Collection<MeasurementCode> measurementCodes, int offset, int limit,
      List<SortOrder> sortOrders) {
    return List.of();
  }

  @Override
  public int countRawDataByMeasurementIds(Collection<MeasurementCode> measurementCodes) {
    return 0;
  }

  @Override
  public void addNGSMeasurement(NGSMeasurement ngsMeasurement, List<SampleCode> sampleCodes) {

  }

  @Override
  public void addProteomicsMeasurement(ProteomicsMeasurement proteomicsMeasurement,
      List<SampleCode> sampleCodes) {

  }

  @Override
  public void deleteProteomicsMeasurements(List<ProteomicsMeasurement> measurements) {

  }

  @Override
  public void deleteNGSMeasurements(List<NGSMeasurement> measurements) {

  }

  @Override
  public void saveAllProteomics(
      Map<ProteomicsMeasurement, Collection<SampleIdCodeEntry>> proteomicsMeasurementsMapping) {

  }

  @Override
  public void saveAllNGS(
      Map<NGSMeasurement, Collection<SampleIdCodeEntry>> ngsMeasurementsMapping) {

  }

  @Override
  public boolean hasDataAttached(List<MeasurementCode> measurements) {
    return false;
  }

  @Override
  public void add(Project project) {

  }

  @Override
  public void delete(ProjectCode projectCode) {

  }

  @Override
  public boolean projectExists(ProjectCode projectCode) {
    return false;
  }

  @Override
  public void addSamplesToProject(Project project, List<Sample> samples) {

  }

  @Override
  public void deleteAll(ProjectCode projectCode, Collection<SampleCode> samples) {

  }

  @Override
  public void updateAll(Project project, Collection<Sample> samples) {

  }

  @Override
  public boolean canDeleteSample(SampleCode sampleCode) {
    return false;
  }

  @Override
  public void destroy() throws Exception {

  }
}
