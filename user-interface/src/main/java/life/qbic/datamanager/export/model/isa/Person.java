package life.qbic.datamanager.export.model.isa;

/**
 * <b>A ISA-JSON person representation</b>
 *
 * <p>A <a
 * href="https://isa-specs.readthedocs.io/en/latest/isajson.html#person-schema-json">person</a>
 * representation based on the ISA-JSON specification</p>
 *
 * @since 1.5.0
 */
public record Person(String firstName, String lastName, String email) {

}
