package life.qbic.projectmanagement.domain.experiment

import life.qbic.projectmanagement.domain.model.experiment.BiologicalReplicate
import spock.lang.Specification

class BiologicalReplicateSpec extends Specification {
    def "Given the counter is reset, the labels end with number 1 to the number of new created replicates"() {
        given:
        BiologicalReplicate.resetReplicateCounter()

        when:
        def firstReplicate = BiologicalReplicate.create()
        BiologicalReplicate lastReplicate = firstReplicate
        for (i in 0..< requestedReplicates - 1) {
            lastReplicate = BiologicalReplicate.create()
        }

        then:
        firstReplicate.label() == "biol-rep-1"
        lastReplicate.label() == "biol-rep-" + requestedReplicates

        where:
        requestedReplicates << [5, 10, 500, 1000]

    }

    def "After the counter has been reset, the next label ends with number 1 again"() {
        given:
        BiologicalReplicate.resetReplicateCounter()

        when:
        def firstReplicate = BiologicalReplicate.create()
        BiologicalReplicate lastReplicate = firstReplicate
        for (i in 0..< requestedReplicates - 1) {
            lastReplicate = BiologicalReplicate.create()
        }
        BiologicalReplicate.resetReplicateCounter()
        lastReplicate = BiologicalReplicate.create()

        then:
        firstReplicate.label() == "biol-rep-1"
        lastReplicate.label() == firstReplicate.label()

        where:
        requestedReplicates << [5, 10, 500, 1000]
    }
}
