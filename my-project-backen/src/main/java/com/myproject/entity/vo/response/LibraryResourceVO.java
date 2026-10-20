package com.myproject.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class LibraryResourceVO {
    Integer id;
    String title;
    String author;
    String category;
    String cover;
    String description;
    String url;
    Date updateTime;
}
