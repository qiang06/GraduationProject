package com.myproject.entity.vo.response;

import com.alibaba.fastjson2.JSONObject;
import lombok.Data;

import java.util.Date;

@Data
public class TopicDetailVO {
    Integer id;
    String title;
    String content;
    Integer uid;
    String username;
    String avatar;
    Integer type;
    String typeName;
    String typeCode;
    JSONObject extra;
    TopicAuthorVO author;
    Date time;
    Integer top;
    Integer locked;
    Integer invisible;
    Integer likeCount;
    Integer collectCount;
    boolean liked;
    boolean collected;
}
