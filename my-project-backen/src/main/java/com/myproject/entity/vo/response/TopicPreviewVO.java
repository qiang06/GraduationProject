package com.myproject.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class TopicPreviewVO {
    Integer id;
    String title;
    Integer uid;
    String username;
    Integer type;
    Date time;
    Integer top;
    Integer likeCount;
    Integer collectCount;
}
