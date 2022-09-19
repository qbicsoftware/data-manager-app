package life.qbic.finance.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Simple implementation of a {@link Pageable} interface that provides offset and limit based
 * pagination of queries.
 *
 * @since 1.0.0.
 */
public class OffsetBasedRequest implements Pageable {

  private final int limit;

  private final int offset;

  private final Sort sort;

  /**
   * Basic constructor.
   *
   * @param offset the offset of the query result index to start
   * @param limit  the size of the query result, starting from the offset
   * @since 1.0.0
   */
  public OffsetBasedRequest(int offset, int limit) {
    if (limit < 1) {
      throw new IllegalArgumentException("Limit must not be less than one!");
    }
    if (offset < 0) {
      throw new IllegalArgumentException("Offset index must not be less than zero!");
    }
    this.offset = offset;
    this.limit = limit;
    sort = Sort.by(Sort.Direction.DESC, "id");
  }

  @Override
  public int getPageNumber() {
    return offset / limit;
  }

  @Override
  public int getPageSize() {
    return limit;
  }

  @Override
  public long getOffset() {
    return offset;
  }

  @Override
  public Sort getSort() {
    return sort;
  }

  @Override
  public Pageable next() {
    return new OffsetBasedRequest((int) (getOffset() + getPageSize()), getPageSize());
  }

  public Pageable previous() {
    return hasPrevious() ? new OffsetBasedRequest((int) (getOffset() - getPageSize()),
        getPageSize()) : this;
  }

  @Override
  public Pageable previousOrFirst() {
    return hasPrevious() ? previous() : first();
  }

  @Override
  public Pageable first() {
    return new OffsetBasedRequest(0, getPageSize());
  }

  @Override
  public Pageable withPage(int pageNumber) {
    return new OffsetBasedRequest(offset * pageNumber, getPageSize());
  }

  @Override
  public boolean hasPrevious() {
    return offset > limit;
  }
}
