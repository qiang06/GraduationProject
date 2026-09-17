package com.myproject.entity.vo.response;

import java.util.List;

public record PageVO<T>(
        List<T> items,
        long total,
        long page,
        long size,
        long pages,
        boolean hasNext
) {
    public static <T> PageVO<T> of(List<T> items, long total, long page, long size, long pages) {
        return new PageVO<>(items, total, page, size, pages, page < pages);
    }
}
