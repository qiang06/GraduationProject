package com.myproject.entity.vo.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class TopicTypeUpdateVO {
    @NotNull
    @Min(1)
    Integer id;

    @NotBlank
    @Length(max = 20)
    String name;

    @Length(max = 255)
    String desc;

    @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "颜色必须是 #RRGGBB")
    String color;

    Boolean anonymous;
}
