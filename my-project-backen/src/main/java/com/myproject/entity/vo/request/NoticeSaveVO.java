package com.myproject.entity.vo.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class NoticeSaveVO {
    Integer id;

    @NotBlank
    @Length(max = 120)
    String title;

    @NotBlank
    @Length(max = 20)
    String category;

    @NotBlank
    @Length(max = 50000)
    String content;

    @NotNull
    Boolean top;

    @Length(max = 50)
    String publisher;
}
