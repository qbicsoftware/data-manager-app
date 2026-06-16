package life.qbic.projectmanagement.application.policy.directive

import java.time.Instant
import life.qbic.projectmanagement.application.ProjectInformationService
import life.qbic.projectmanagement.application.measurement.MeasurementLookupService
import life.qbic.projectmanagement.domain.model.measurement.ImmunopeptidomicsMeasurement
import life.qbic.projectmanagement.domain.model.measurement.MeasurementId
import life.qbic.projectmanagement.domain.model.measurement.NGSMeasurement
import life.qbic.projectmanagement.domain.model.measurement.ProteomicsMeasurement
import life.qbic.projectmanagement.domain.model.project.ProjectId
import org.jobrunr.scheduling.JobScheduler
import spock.lang.Specification

class UpdateProjectUponMeasurementCreationSpec extends Specification {

    def "Given an NGS measurement, update the project modified date"() {
        given:
        def expectedProjectId = ProjectId.create()
        def measurementId = MeasurementId.create()
        def modifiedOn = Instant.now()
        def ngsMeasurement = Mock(NGSMeasurement)
        ngsMeasurement.projectId() >> expectedProjectId
        def measurementLookupService = Mock(MeasurementLookupService) {
            findNGSMeasurementById(measurementId.value()) >> Optional.of(ngsMeasurement)
            findProteomicsMeasurementById(_) >> Optional.empty()
            findIPMeasurementById(_) >> Optional.empty()
        }
        def projectInformationService = Mock(ProjectInformationService)
        def jobScheduler = Mock(JobScheduler)

        def directive = new UpdateProjectUponMeasurementCreation(
                measurementLookupService, projectInformationService, jobScheduler)

        when:
        directive.updateProjectModified(measurementId, modifiedOn)

        then:
        1 * projectInformationService.updateModifiedDate(expectedProjectId, modifiedOn)
    }

    def "Given a Proteomics measurement, update the project modified date"() {
        given:
        def expectedProjectId = ProjectId.create()
        def measurementId = MeasurementId.create()
        def modifiedOn = Instant.now()
        def ptxMeasurement = Mock(ProteomicsMeasurement)
        ptxMeasurement.projectId() >> expectedProjectId
        def measurementLookupService = Mock(MeasurementLookupService) {
            findNGSMeasurementById(measurementId.value()) >> Optional.empty()
            findProteomicsMeasurementById(measurementId.value()) >> Optional.of(ptxMeasurement)
            findIPMeasurementById(_) >> Optional.empty()
        }
        def projectInformationService = Mock(ProjectInformationService)
        def jobScheduler = Mock(JobScheduler)

        def directive = new UpdateProjectUponMeasurementCreation(
                measurementLookupService, projectInformationService, jobScheduler)

        when:
        directive.updateProjectModified(measurementId, modifiedOn)

        then:
        1 * projectInformationService.updateModifiedDate(expectedProjectId, modifiedOn)
    }

    def "Given an IP measurement, update the project modified date"() {
        given:
        def expectedProjectId = ProjectId.create()
        def measurementId = MeasurementId.create()
        def modifiedOn = Instant.now()
        def ipMeasurement = Mock(ImmunopeptidomicsMeasurement)
        ipMeasurement.projectId() >> expectedProjectId
        def measurementLookupService = Mock(MeasurementLookupService) {
            findNGSMeasurementById(measurementId.value()) >> Optional.empty()
            findProteomicsMeasurementById(measurementId.value()) >> Optional.empty()
            findIPMeasurementById(measurementId.value()) >> Optional.of(ipMeasurement)
        }
        def projectInformationService = Mock(ProjectInformationService)
        def jobScheduler = Mock(JobScheduler)

        def directive = new UpdateProjectUponMeasurementCreation(
                measurementLookupService, projectInformationService, jobScheduler)

        when:
        directive.updateProjectModified(measurementId, modifiedOn)

        then:
        1 * projectInformationService.updateModifiedDate(expectedProjectId, modifiedOn)
    }

    def "Given no measurement is found, throw InvalidEventDataException"() {
        given:
        def measurementId = MeasurementId.create()
        def modifiedOn = Instant.now()
        def measurementLookupService = Mock(MeasurementLookupService) {
            findNGSMeasurementById(measurementId.value()) >> Optional.empty()
            findProteomicsMeasurementById(measurementId.value()) >> Optional.empty()
            findIPMeasurementById(measurementId.value()) >> Optional.empty()
        }
        def projectInformationService = Mock(ProjectInformationService)
        def jobScheduler = Mock(JobScheduler)

        def directive = new UpdateProjectUponMeasurementCreation(
                measurementLookupService, projectInformationService, jobScheduler)

        when:
        directive.updateProjectModified(measurementId, modifiedOn)

        then:
        thrown(InvalidEventDataException)
        0 * projectInformationService.updateModifiedDate(_, _)
    }
}
