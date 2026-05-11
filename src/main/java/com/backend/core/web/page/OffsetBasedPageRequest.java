package com.backend.core.web.page;

import java.util.Objects;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class OffsetBasedPageRequest implements Pageable {
  public static final Integer DEFAULT_PAGE_SIZE = 20;
  private final int pageSize;
  private final long offset;
  private final Sort sort;

  public OffsetBasedPageRequest(Long offset, Integer pageSize, Sort sort) {
    this.pageSize = Objects.isNull(pageSize) ? DEFAULT_PAGE_SIZE : pageSize;
    this.offset = Objects.isNull(offset) ? 0 : offset;
    this.sort = sort;
  }

  public OffsetBasedPageRequest(Long offset, Integer pageSize) {
    this(offset, pageSize, Sort.unsorted());
  }

  @Override
  public int getPageNumber() {
    return (int) (offset / pageSize);
  }

  @Override
  public int getPageSize() {
    return pageSize;
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
    return new OffsetBasedPageRequest(getOffset() + getPageSize(), getPageSize(), getSort());
  }

  public OffsetBasedPageRequest previous() {
    return hasPrevious()
        ? new OffsetBasedPageRequest(getOffset() - getPageSize(), getPageSize(), getSort())
        : this;
  }

  @Override
  public Pageable previousOrFirst() {
    return hasPrevious() ? previous() : first();
  }

  @Override
  public Pageable first() {
    return new OffsetBasedPageRequest(0L, getPageSize(), getSort());
  }

  @Override
  public Pageable withPage(int pageNumber) {
    return new OffsetBasedPageRequest((long) pageNumber * getPageSize(), getPageSize(), getSort());
  }

  @Override
  public boolean hasPrevious() {
    return offset >= pageSize;
  }
}
