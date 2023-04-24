package life.qbic.projectmanagement.domain.project.experiment;

import javax.persistence.Embeddable;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Random;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Embeddable
public class BiologicalReplicateId implements Serializable {
    @Serial
    private static final long serialVersionUID = 3380429168282318636L;

    private Long id;

    protected BiologicalReplicateId() {
        // Needed for JPA
    }

    private BiologicalReplicateId(Long id) {
        this.id = id;
    }

    public static BiologicalReplicateId create() {
        Long id = new Random().nextLong();
        return new BiologicalReplicateId(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BiologicalReplicateId that = (BiologicalReplicateId) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public Long id() {
        return id;
    }

    @Override
    public String toString() {
        return "BiologicalReplicateId{" + "id=" + id + '}';
    }
}
