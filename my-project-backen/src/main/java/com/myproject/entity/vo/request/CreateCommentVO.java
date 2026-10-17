package com.myproject.entity.vo.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class CreateCommentVO {
    @NotNull
    @Min(1)
    Integer tid;

    /**
     * 富文本 HTML。纯文本上限 2000 字、HTML 上限 20000 字符由 Service 校验；
     * 这里保留一个宽松的硬上限，防止超大请求直接进业务层。
     */
    @NotBlank
    @Length(max = 20000)
    String content;

    /** 直接父评论 id；顶层评论为 null */
    @Min(1)
    Integer quote;
}
