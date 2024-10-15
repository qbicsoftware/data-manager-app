package life.qbic.datamanager.export.model.isa;

import java.util.List;

/**
 * <b>The ISA investigation concept</b>
 *
 * <p>Implementation of the <a
 * href="https://isa-specs.readthedocs.io/en/latest/isamodel.html#investigation">investigation</a>
 * concept from the <a href="https://isa-specs.readthedocs.io/en/latest/index.html">ISA
 * specification</a>.</p>
 *
 * @since 1.5.0
 */
public record Investigation(String identifier, String title, String description,
                            List<Person> people) {

}
