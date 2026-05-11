package life.qbic.datamanager.files.parsing.converters

import life.qbic.datamanager.files.parsing.ParsingResult
import life.qbic.datamanager.files.structure.measurement.IPMeasurementRegisterColumn
import spock.lang.Specification

class MeasurementRegistrationMetadataConverterIPSpec extends Specification {

    def "Given a valid single-row ParsingResult, the converter extracts all IP measurement metadata correctly"() {
        given:
        def columnMap = buildIPColumnMap()
        def rowValues = buildFullRowValues()
        def parsingResult = new ParsingResult(columnMap, [new ParsingResult.Row(rowValues)])

        when:
        def result = new MeasurementRegistrationMetadataConverterIP().convert(parsingResult)

        then:
        result.size() == 1
        def info = result[0]

        and: "common metadata is extracted"
        info.organisationId() == "https://ror.org/03a1kwz48"
        info.instrumentCURIE() == "EFO:0008637"
        info.facility() == "QBiC"
        info.measurementName() == "MS Run 1"

        and: "sample-specific metadata is extracted"
        info.specificMetadata().size() == 1
        def specific = info.specificMetadata()["QTEST001AE"]
        specific != null
        specific.sampleMass() == "1.5"
        specific.sampleVolume() == "100.5"
        specific.cycleFractionName() == "Fraction 1"
        specific.mhcAntibody() == "W6/32"
        specific.mhcTypingMethod() == "PCR-SSP"
        specific.enrichmentMethod() == "Immune affinity"
        specific.prepDate() == "2024-01-15"
        specific.msRunDate() == "2024-01-16"
        specific.lcmsMethod() == "DDA"
        specific.lcColumn() == "C18"
        specific.dataAcquisition() == "DDA"
        specific.massRange() == "300-1800"
        specific.retentionTimeRange() == "120"
        specific.chargeRange() == "2-4"
        specific.ionMobilityRange() == "0.6-1.6"
        specific.comment() == "Test comment"
    }

    def "Given a ParsingResult with multiple rows, the converter produces one MeasurementRegistrationInformationIP per row"() {
        given:
        def columnMap = buildIPColumnMap()
        def row1 = buildFullRowValues()
        def row2 = buildFullRowValues()
        row2[0] = "QTEST002AE"  // different sample id
        row2[2] = "MS Run 2"    // different measurement name

        def parsingResult = new ParsingResult(columnMap, [
            new ParsingResult.Row(row1),
            new ParsingResult.Row(row2)
        ])

        when:
        def result = new MeasurementRegistrationMetadataConverterIP().convert(parsingResult)

        then:
        result.size() == 2
        result[0].specificMetadata().keySet().iterator().next() == "QTEST001AE"
        result[1].specificMetadata().keySet().iterator().next() == "QTEST002AE"
    }

    def "Given a ParsingResult with empty optional fields, the converter returns empty strings for those fields"() {
        given:
        def columnMap = buildIPColumnMap()
        def rowValues = buildFullRowValues()
        rowValues[3] = ""   // Cycle/Fraction Name
        rowValues[9] = ""   // MHC Typing Method
        rowValues[6] = ""   // Prep Date
        rowValues[12] = ""  // MS Run Date
        rowValues[18] = ""  // Ion Mobility Range
        rowValues[21] = ""  // Comment

        def parsingResult = new ParsingResult(columnMap, [new ParsingResult.Row(rowValues)])

        when:
        def result = new MeasurementRegistrationMetadataConverterIP().convert(parsingResult)

        then:
        result.size() == 1
        def specific = result[0].specificMetadata()["QTEST001AE"]
        specific.cycleFractionName().isEmpty()
        specific.mhcTypingMethod().isEmpty()
        specific.prepDate().isEmpty()
        specific.msRunDate().isEmpty()
        specific.ionMobilityRange().isEmpty()
        specific.comment().isEmpty()
    }

    def "The converter uses header names that match the template headers without asterisks"() {
        given:
        // Headers must NOT contain '*' because the template generator and sanitizer both
        // produce headers without '*'.
        def columnMap = [
            "QBiC Sample Id"              : 0,
            "Sample Name"                 : 1,
            "Measurement Name"            : 2,
            "Cycle/Fraction Name"         : 3,
            "Sample Mass (mg)"            : 4,
            "Sample Volume (decimal)"     : 5,
            "Prep Date"                   : 6,
            "Enrichment method"           : 7,
            "MHC Antibody"                : 8,
            "MHC Typing Method"           : 9,
            "Facility"                    : 10,
            "Organisation URL"            : 11,
            "MS Run Date"                 : 12,
            "Data Acquisition"            : 13,
            "Instrument"                  : 14,
            "LCMS Method"                 : 15,
            "LC Column"                   : 16,
            "Charge range"                : 17,
            "Ion mobility range (1/k0)"   : 18,
            "Mass range (m/z)"            : 19,
            "Retention time range (min)"  : 20,
            "Comment"                     : 21
        ]
        def row = buildFullRowValues()
        def parsingResult = new ParsingResult(columnMap, [new ParsingResult.Row(row)])

        when:
        def result = new MeasurementRegistrationMetadataConverterIP().convert(parsingResult)

        then:
        // If the converter used headers with '*', all values would be empty strings.
        // This assertion guards against the regression.
        result[0].organisationId() == "https://ror.org/03a1kwz48"
        result[0].instrumentCURIE() == "EFO:0008637"
        result[0].specificMetadata()["QTEST001AE"].mhcAntibody() == "W6/32"
        result[0].specificMetadata()["QTEST001AE"].enrichmentMethod() == "Immune affinity"
    }

    private static Map<String, Integer> buildIPColumnMap() {
        def map = new LinkedHashMap<String, Integer>()
        for (def col : IPMeasurementRegisterColumn.values()) {
            map.put(col.headerName(), col.index())
        }
        return map
    }

    private static List<String> buildFullRowValues() {
        return [
            "QTEST001AE",               // 0  SAMPLE_ID
            "Sample 1",                 // 1  SAMPLE_NAME
            "MS Run 1",                 // 2  MEASUREMENT_NAME
            "Fraction 1",               // 3  CYCLE_FRACTION_NAME
            "1.5",                      // 4  SAMPLE_MASS
            "100.5",                    // 5  SAMPLE_VOLUME
            "2024-01-15",               // 6  PREP_DATE
            "Immune affinity",          // 7  ENRICHMENT_METHOD
            "W6/32",                    // 8  MHC_ANTIBODY
            "PCR-SSP",                  // 9  MHC_TYPING_METHOD
            "QBiC",                     // 10 FACILITY
            "https://ror.org/03a1kwz48",// 11 ORGANISATION_URL
            "2024-01-16",               // 12 MS_RUN_DATE
            "DDA",                      // 13 DATA_ACQUISITION
            "EFO:0008637",              // 14 INSTRUMENT
            "DDA",                      // 15 LCMS_METHOD
            "C18",                      // 16 LC_COLUMN
            "2-4",                      // 17 CHARGE_RANGE
            "0.6-1.6",                  // 18 ION_MOBILITY_RANGE
            "300-1800",                 // 19 MASS_RANGE
            "120",                      // 20 RETENTION_TIME_RANGE
            "Test comment"              // 21 COMMENT
        ]
    }
}
