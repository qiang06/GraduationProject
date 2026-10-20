package com.myproject.entity.vo.response;

import lombok.Data;

@Data
public class CourseVO {
    Integer id;
    Integer uid;
    String username;
    String name;
    String teacher;
    String location;
    Integer weekday;
    Integer startSection;
    Integer endSection;
    String weeks;
    String color;
    String note;
}
