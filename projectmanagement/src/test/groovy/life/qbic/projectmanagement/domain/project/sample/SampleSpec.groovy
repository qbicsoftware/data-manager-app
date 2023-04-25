package life.qbic.projectmanagement.domain.project.sample

import life.qbic.projectmanagement.domain.project.experiment.ExperimentId
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Analyte
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Species
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Specimen
import spock.lang.Specification

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
class SampleSpec extends Specification {

    def "If a sample is not yet assign to a batch, allow to assign it to a new batch"() {
        given:
        SampleOrigin sampleOrigin = new SampleOrigin(Species.create("Homo Sapiens"), Specimen.create("Skin"), Analyte.create("DNA"))
        Sample unAssignedSample = Sample.create("My test sample", ExperimentId.create(), 1_000L, new BiologicalReplicateLabel("biol repl. 1"), sampleOrigin)
        Batch batch = Batch.create("Test Batch")
        BatchId batchId = batch.batchId()

        when:
        def response = unAssignedSample.assignToBatch(batch)

        then:
        response.code().equals(Sample.SampleAddResponse.ResponseCode.SUCCESSFUL)
        unAssignedSample.assignedBatch().get().equals(batchId)

    }

    def "If a sample is already assigned to a batch, the assignment is not overwritten and a failure response is returned"() {
        given:
        SampleOrigin sampleOrigin = new SampleOrigin(Species.create("Homo Sapiens"), Specimen.create("Skin"), Analyte.create("DNA"))
        Sample unAssignedSample = Sample.create("My test sample", ExperimentId.create(), 1_000L, new BiologicalReplicateLabel("biol repl. 1"), sampleOrigin)
        Batch batch = Batch.create("Test Batch")
        Batch anotherBatch = Batch.create("Another Batch")

        when:
        unAssignedSample.assignToBatch(batch)
        def response = unAssignedSample.assignToBatch(anotherBatch)

        then:
        response.code().equals(Sample.SampleAddResponse.ResponseCode.ALREADY_IN_BATCH)
        unAssignedSample.assignedBatch().get().equals(batch.batchId())

    }

}
