package com.myproject.entity.vo.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class CourseSaveVO {
    Integer id;

    @NotBlank
    @Length(max = 80)
    String name;

    @Length(max = 50)
    String teacher;

    @Length(max = 80)
    String location;

    @NotNull
    @Min(1)
    @Max(7)
    Integer weekday;

    @NotNull
    @Min(1)
    @Max(12)
    Integer startSection;

    @NotNull
    @Min(1)
    @Max(12)
    Integer endSection;

    @Length(max = 40)
    String weeks;

    @Length(max = 20)
    String color;

    @Length(max = 200)
    String note;
}
