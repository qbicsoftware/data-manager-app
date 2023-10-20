package life.qbic.controlling.domain.model.sample;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.UUID;
import org.springframework.lang.NonNull;

/**
 * Unique sample identifier. Identifies a sample unambiguously in Tuebingen's
 * FDM business.
 */
@Embeddable
@Access(AccessType.FIELD)
public class SampleId implements Serializable {
    @Serial
    private static final long serialVersionUID = 1841536150220843163L;

    @NonNull
    @Column(name = "sample_id")
    private final String uuid;

    private SampleId(UUID id) {
        if (Objects.isNull(id)) {
            throw new IllegalArgumentException("uuid must be provided");
        }
        this.uuid = id.toString();
    }

    protected SampleId() {
        this(UUID.randomUUID());
        // needed for JPA
    }

    public static SampleId create() {
        return new SampleId(UUID.randomUUID());
    }

    public static SampleId of(UUID uid) {
        return new SampleId(uid);
    }

    public static SampleId parse(String uid) throws IllegalArgumentException {
        UUID id = UUID.fromString(uid);
        return new SampleId(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SampleId sampleId = (SampleId) o;
        return Objects.equals(uuid, sampleId.uuid);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SampleId.class.getSimpleName() + "[", "]")
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
