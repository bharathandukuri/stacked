package com.bharath.stacked.common.api;

import java.util.Collections;
import java.util.List;

public class PageResponse<T> {

    private List<T> items;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean isFirst;
    private boolean isLast;
    private boolean hasNext;
    private boolean hasPrevious;

    public PageResponse() {
        this.items = Collections.emptyList();
    }

    public PageResponse(List<T> items, int page, int size, long totalElements, int totalPages,
                        boolean isFirst, boolean isLast, boolean hasNext, boolean hasPrevious) {
        this.items = items != null ? items : Collections.emptyList();
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.isFirst = isFirst;
        this.isLast = isLast;
        this.hasNext = hasNext;
        this.hasPrevious = hasPrevious;
    }

    public static <T> PageResponse<T> of(List<T> items, int page, int size, long totalElements) {
        List<T> safeItems = items != null ? items : Collections.emptyList();
        int safeSize = size <= 0 ? 20 : size;
        int totalPages = safeSize == 0 ? 1 : (int) Math.ceil((double) totalElements / (double) safeSize);
        if (totalElements == 0) {
            totalPages = 0;
        }

        return new PageResponse<>(
                safeItems,
                page,
                safeSize,
                totalElements,
                totalPages,
                page <= 0,
                totalPages == 0 || page >= totalPages - 1,
                totalPages > 0 && page < totalPages - 1,
                page > 0
        );
    }

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public boolean isFirst() {
        return isFirst;
    }

    public void setFirst(boolean first) {
        isFirst = first;
    }

    public boolean isLast() {
        return isLast;
    }

    public void setLast(boolean last) {
        isLast = last;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }

    public boolean isHasPrevious() {
        return hasPrevious;
    }

    public void setHasPrevious(boolean hasPrevious) {
        this.hasPrevious = hasPrevious;
    }
}
