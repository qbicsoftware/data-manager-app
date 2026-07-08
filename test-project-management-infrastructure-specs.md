# Spock Tests for Immunopeptidomics Measurement Deletion

## Requirements
- `MEASUREMENT-R-03` (Deletion with dataset guard)
- `LAB-C-01` (OpenBIS synchronization)

## File Location
`project-management-infrastructure/src/test/groovy/life/qbic/projectmanagement/infrastructure/experiment/measurement/MeasurementRepositoryImplementationSpec.groovy`

## Test Cases

### 1. `def "deleteAllIP throws DATA_ATTACHED when measurements have attached data"`
```groovy
def "deleteAllIP throws DATA_ATTACHED when measurements have attached data"() {
  given:
  Mock measurementDataRepo = Mock(MeasurementDataRepo)
  MeasurementRepositoryImplementation implementation = new MeasurementRepositoryImplementation(
      measurementDataRepo, ipMeasurementJpaRepo, log
  )
  Set<String> measurementIds = ["IP-123", "IP-456"]
  List<ImmunopeptidomicsMeasurement> measurements = measurementIds.collect { new ImmunopeptidomicsMeasurement(it) }

  and:
  measurementDataRepo.hasDataAttached(_) >> true
  ipMeasurementJpaRepo.findAllById(_) >> measurements

  when:
  implementation.deleteAllIP(measurementIds)

  then:
  thrown(MeasurementDeletionException)
  MeasurementDeletionException ex = thrown()
  ex.reason() == DeletionErrorCode.DATA_ATTACHED

  0 * ipMeasurementJpaRepo.deleteAll(_)
  0 * measurementDataRepo.deleteImmunopeptidomicsMeasurements(_)
}
```

### 2. `def "deleteAllIP deletes from JPA and OpenBIS when no data attached"`
```groovy
def "deleteAllIP deletes from JPA and OpenBIS when no data attached"() {
  given:
  Mock measurementDataRepo = Mock(MeasurementDataRepo)
  MeasurementRepositoryImplementation implementation = new MeasurementRepositoryImplementation(
      measurementDataRepo, ipMeasurementJpaRepo, log
  )
  Set<String> measurementIds = ["IP-123", "IP-456"]
  List<ImmunopeptidomicsMeasurement> measurements = measurementIds.collect { new ImmunopeptidomicsMeasurement(it) }

  and:
  measurementDataRepo.hasDataAttached(_) >> false
  ipMeasurementJpaRepo.findAllById(_) >> measurements

  when:
  implementation.deleteAllIP(measurementIds)

  then:
  1 * ipMeasurementJpaRepo.deleteAll(measurements)
  1 * measurementDataRepo.deleteImmunopeptidomicsMeasurements(measurements)
}
```

## Verification
1. Run with `./mvnw verify`
2. Confirm 2 test cases pass
3. Ensure no other tests fail

> **Note**: The UI error handling is already verified in `MeasurementMainSpec` (already exists)
