package com.myproject.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class NoticePreviewVO {
    Integer id;
    String title;
    String category;
    Integer top;
    String publisher;
    Date publishTime;
    String summary;
}
