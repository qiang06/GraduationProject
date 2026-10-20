package com.myproject.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class NoticeVO {
    Integer id;
    String title;
    String category;
    String content;
    Integer top;
    String publisher;
    Date publishTime;
    Date updateTime;
}
