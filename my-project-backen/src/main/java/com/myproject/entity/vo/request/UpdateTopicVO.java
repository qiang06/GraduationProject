package com.myproject.entity.vo.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class UpdateTopicVO {
    @NotNull
    @Min(1)
    Integer id;

    @NotBlank
    @Pattern(regexp = "^[a-zA-Z0-9\\u4e00-\\u9fa5\\s~!?,.\"'_+\\-、。！？～【】「」（）]+$")
    @Length(min = 1, max = 30)
    String title;

    @NotNull
    @Min(1)
    Integer type;

    @NotBlank
    @Length(min = 1, max = 20000)
    String content;
}
