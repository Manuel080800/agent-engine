package io.agentengine.core.common.pagination;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public record OffsetPageRequest(long offset, int pageSize, Sort sort) implements Pageable {
    public OffsetPageRequest {
        if (offset < 0 || pageSize < 1) {
            throw new IllegalArgumentException("Invalid pagination");
        }
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
        return new OffsetPageRequest(offset + pageSize, pageSize, sort);
    }

    @Override
    public Pageable previousOrFirst() {
        return new OffsetPageRequest(Math.max(0, offset - pageSize), pageSize, sort);
    }

    @Override
    public Pageable first() {
        return new OffsetPageRequest(0, pageSize, sort);
    }

    @Override
    public Pageable withPage(int page) {
        if (page < 0) {
            throw new IllegalArgumentException("Invalid page");
        }

        return new OffsetPageRequest((long) page * pageSize, pageSize, sort);
    }

    @Override
    public boolean hasPrevious() {
        return offset > 0;
    }
}
