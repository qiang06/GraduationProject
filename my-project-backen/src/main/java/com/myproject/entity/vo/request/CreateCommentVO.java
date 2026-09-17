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

    @NotBlank
    @Length(min = 1, max = 500)
    String content;

    Integer quote;
}
