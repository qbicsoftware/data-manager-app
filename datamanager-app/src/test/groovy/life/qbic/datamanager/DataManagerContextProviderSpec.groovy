package life.qbic.datamanager

import spock.lang.Specification

class DataManagerContextProviderSpec extends Specification {

    private static final String PROTOCOL = "https"
    private static final String HOST = "data-manager.example.com"
    private static final String CONTEXT_PATH = ""
    private static final String PROJECT_ENDPOINT = "/projects/%s/info"
    private static final String SAMPLES_ENDPOINT = "/projects/%s/experiments/%s/samples"

    def "builds base url without port when no port is specified"() {
        given:
        def provider = providerWithPort(-1)

        expect:
        provider.urlToProject("QABCD001") == "https://data-manager.example.com/projects/QABCD001/info"
    }

    def "builds base url without port for sample page when no port is specified"() {
        given:
        def provider = providerWithPort(-1)

        expect:
        provider.urlToSamplePage("QABCD001", "E12345") ==
                "https://data-manager.example.com/projects/QABCD001/experiments/E12345/samples"
    }

    def "builds base url with port when a port is specified"() {
        given:
        def provider = providerWithPort(8443)

        expect:
        provider.urlToProject("QABCD001") == "https://data-manager.example.com:8443/projects/QABCD001/info"
    }

    private static DataManagerContextProvider providerWithPort(int port) {
        return new DataManagerContextProvider(
                PROTOCOL,
                HOST,
                port,
                CONTEXT_PATH,
                PROJECT_ENDPOINT,
                SAMPLES_ENDPOINT)
    }
}