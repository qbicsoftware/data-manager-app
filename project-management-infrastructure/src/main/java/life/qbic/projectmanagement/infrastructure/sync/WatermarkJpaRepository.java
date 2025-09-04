package life.qbic.projectmanagement.infrastructure.sync;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * <b>Watermark Jpa Repository</b>
 *
 * <p>Extension of the {@link JpaRepository} interface to support {@link WatermarkEntry}
 * entities.</p>
 *
 * @since 1.11.0
 */
public interface WatermarkJpaRepository extends JpaRepository<WatermarkEntry, String> {

}
