package life.qbic.projectmanagement.domain.project.sample;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Embeddable;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Embeddable
@Access(AccessType.FIELD)
public class SampleId implements Serializable {
    @Serial
    private static final long serialVersionUID = 1841536150220843163L;

    private String uuid;

    private SampleId(UUID id) {
        this.uuid = id.toString();
    }

    protected SampleId() {
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
    public int hashCode() {
        return Objects.hash(uuid);
    }

    public String value() {
        return this.uuid;
    }
}
