package life.qbic.projectmanagement.domain.project.sample;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Unique sample batch identifier. Identifies a sample batch unambiguously in Tübingen's
 * FDM business.
 * @since 1.0.0
 */
@Embeddable
@Access(AccessType.FIELD)
public class BatchId implements Serializable {

    @Serial
    private static final long serialVersionUID = 2774541863689155375L;

    @Column(name = "id")
    private String uuid;

    protected BatchId() {
        // needed for JPA
    }

    private BatchId(UUID id) {
        this.uuid = id.toString();
    }

    public static BatchId create() {
        return new BatchId(UUID.randomUUID());
    }

    public static BatchId of(UUID id) {
        return new BatchId(id);
    }

    public static BatchId parse(String str) throws IllegalArgumentException {
        UUID id = UUID.fromString(str);
        return new BatchId(id);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BatchId batchId = (BatchId) o;
        return Objects.equals(uuid, batchId.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}
