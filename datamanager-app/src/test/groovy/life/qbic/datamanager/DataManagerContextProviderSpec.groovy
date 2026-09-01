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

    def "preserves the context path for an absolute project endpoint"() {
        given:
        def provider = new DataManagerContextProvider(PROTOCOL, HOST, -1, contextPath, PROJECT_ENDPOINT, SAMPLES_ENDPOINT)

        expect:
        provider.urlToProject("QABCD001") == expected

        where:
        contextPath | expected
        ""          | "https://data-manager.example.com/projects/QABCD001/info"
        "foobar"    | "https://data-manager.example.com/foobar/projects/QABCD001/info"
    }

    def "preserves the context path for a relative project endpoint"() {
        given:
        def provider = new DataManagerContextProvider(PROTOCOL, HOST, -1, contextPath, "projects/%s/info", SAMPLES_ENDPOINT)

        expect:
        provider.urlToProject("QABCD001") == expected

        where:
        contextPath | expected
        ""          | "https://data-manager.example.com/projects/QABCD001/info"
        "foobar"    | "https://data-manager.example.com/foobar/projects/QABCD001/info"
    }

    def "preserves the context path for the sample page endpoint"() {
        given:
        def provider = new DataManagerContextProvider(PROTOCOL, HOST, -1, contextPath, PROJECT_ENDPOINT, SAMPLES_ENDPOINT)

        expect:
        provider.urlToSamplePage("QABCD001", "E12345") == expected

        where:
        contextPath | expected
        ""          | "https://data-manager.example.com/projects/QABCD001/experiments/E12345/samples"
        "foobar"    | "https://data-manager.example.com/foobar/projects/QABCD001/experiments/E12345/samples"
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