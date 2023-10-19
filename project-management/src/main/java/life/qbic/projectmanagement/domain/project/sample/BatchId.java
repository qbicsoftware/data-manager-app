package life.qbic.projectmanagement.domain.project.sample;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.StringJoiner;
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
        this(UUID.randomUUID());
        // needed for JPA
    }

    private BatchId(UUID id) {
        if (Objects.isNull(id)) {
            throw new IllegalArgumentException("uuid must be provided");
        }
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
    public String toString() {
        return new StringJoiner(", ", BatchId.class.getSimpleName() + "[", "]")
                .add("uuid=" + uuid)
                .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }

    public String value() {
        return this.uuid;
    }
}
