package com.myproject.entity.vo.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class LibrarySaveVO {
    Integer id;

    @NotBlank
    @Length(max = 120)
    String title;

    @Length(max = 80)
    String author;

    @NotBlank
    @Length(max = 20)
    String category;

    @Length(max = 255)
    String cover;

    @Length(max = 2000)
    String description;

    @Length(max = 500)
    String url;
}
