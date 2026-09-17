package com.myproject.entity.vo.response;

import lombok.Data;

import java.util.Date;

@Data
public class TopicDetailVO {
    Integer id;
    String title;
    String content;
    Integer uid;
    String username;
    Integer type;
    Date time;
    Integer top;
    Integer likeCount;
    Integer collectCount;
    boolean liked;
    boolean collected;
}
