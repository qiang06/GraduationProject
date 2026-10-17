package com.myproject.entity.vo.response;

import java.util.List;

/**
 * 评论分页结果。
 *
 * <p>评论是多级树：{@code total} 是顶层评论数，用于分页；
 * {@code allTotal} 是包含所有回复的总数，用于「评论 (x)」的展示。
 * 不能把两者混用，否则翻页页数会算错。
 */
public record CommentPageVO<T>(List<T> items,
                               long total,
                               long allTotal,
                               long page,
                               long size,
                               long pages,
                               boolean hasNext) {

    public static <T> CommentPageVO<T> of(List<T> items, long total, long allTotal,
                                          long page, long size, long pages) {
        return new CommentPageVO<>(items, total, allTotal, page, size, pages, page < pages);
    }
}
