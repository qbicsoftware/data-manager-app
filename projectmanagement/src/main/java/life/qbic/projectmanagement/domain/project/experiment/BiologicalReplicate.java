package life.qbic.projectmanagement.domain.project.experiment;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
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
@Entity(name = "bio_replicate")
public class BiologicalReplicate implements Serializable {
    @Serial
    private static final long serialVersionUID = 1551778532201183788L;

    @EmbeddedId
    private BiologicalReplicateId id;

    private String label;

    private BiologicalReplicate(String label) {
        this.label = label;
    }

    protected BiologicalReplicate() {
        // Needed for JPA
    }

    public static BiologicalReplicate create(){
        String randomLabel = "biol-rep-" + new Random().nextInt();
        return new BiologicalReplicate(randomLabel);
    }

    public String label(){
        return this.label;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BiologicalReplicate that = (BiologicalReplicate) o;
        return Objects.equals(id, that.id) && Objects.equals(label, that.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, label);
    }

    @Override
    public String toString() {
        return "BiologicalReplicate{" + "id=" + id + ", label='" + label + '\'' + '}';
    }
}
