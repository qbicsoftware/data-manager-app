package life.qbic.projectmanagement.persistence.repository;

import java.util.Optional;

/**
 * <b><interface short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public interface AnalyteTermMapper {

  /**
   * Tries to map a given term to a term of the mapper implementation
   *
   * @param term the term that the mapper should look-up and try to map
   * @return an {@link Optional<String>} mapped term, is {@link Optional#empty()} if the look-up was
   * unsuccessful.
   * @since 1.0.0
   */
  Optional<String> mapFrom(String term);

}
