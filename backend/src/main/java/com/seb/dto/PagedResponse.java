package com.seb.dto;

import lombok.Data;

import java.util.List;

@Data
public class PagedResponse<T> {
    private List<T> items;
    private long total;
    private int page;
    private int pageSize;

    public static <T> PagedResponse<T> of(List<T> items, long total, int page, int pageSize) {
        PagedResponse<T> response = new PagedResponse<>();
        response.setItems(items);
        response.setTotal(total);
        response.setPage(page);
        response.setPageSize(pageSize);
        return response;
    }
}
